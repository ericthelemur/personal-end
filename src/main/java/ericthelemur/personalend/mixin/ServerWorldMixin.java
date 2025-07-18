package ericthelemur.personalend.mixin;

import ericthelemur.personalend.DragonPersistentState;
import ericthelemur.personalend.PersonalEnd;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.spawner.SpecialSpawner;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World {
    @Shadow
    @Nullable
    private EnderDragonFight enderDragonFight;

    @Shadow public abstract long getSeed();

    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }

    @Inject(at = @At("TAIL"), method = "<init>")
    public void constructorMixin(MinecraftServer server, Executor workerExecutor, LevelStorage.Session session, ServerWorldProperties properties, RegistryKey<World> worldKey, DimensionOptions dimensionOptions, WorldGenerationProgressListener worldGenerationProgressListener, boolean debugWorld, long seed, List<SpecialSpawner> spawners, boolean shouldTickTime, RandomSequencesState randomSequencesState, CallbackInfo ci) {
        var sw = (ServerWorld) (World) this;
        // If a personal End
        if (PersonalEnd.isPersonalEnd(this)) {
            // Load existing fight
            var ident = sw.getRegistryKey().getValue();
            var state = DragonPersistentState.getServerState(server);
            var fight = state.getFight(UUID.fromString(ident.getPath()));
            // Record the world
            state.addLoadedWorld(ident.getPath(), sw);
            if (fight != null) {
                // Load existing fight if exists
                this.enderDragonFight = new EnderDragonFight(sw, sw.getSeed(), fight);
                LoggerFactory.getLogger("mixin").info("Loaded dragon fight {}", this.enderDragonFight);
            } else {
                // Set default fight if none
                var data = EnderDragonFight.Data.DEFAULT;
                this.enderDragonFight = new EnderDragonFight(sw, this.getSeed(), data);
                LoggerFactory.getLogger("mixin").info("Created new dragon fight {}", this.enderDragonFight);
            }
        }

    }

    @Redirect(
            method = "saveLevel()V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/world/ServerWorld;enderDragonFight : Lnet/minecraft/entity/boss/dragon/EnderDragonFight;", opcode = Opcodes.GETFIELD)
    )
    private EnderDragonFight saveLevel(ServerWorld world) {
        if (world.getRegistryKey() == World.END) {
            return world.getEnderDragonFight();
        } else {
            return null;
        }
    }
}

