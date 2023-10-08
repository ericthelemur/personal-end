package ericthelemur.personalend.mixin;
import ericthelemur.personalend.DragonPersistentState;
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
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
    public void constructorMixin(MinecraftServer server, Executor workerExecutor, LevelStorage.Session session, ServerWorldProperties properties, RegistryKey worldKey, DimensionOptions dimensionOptions, WorldGenerationProgressListener worldGenerationProgressListener, boolean debugWorld, long seed, List spawners, boolean shouldTickTime, RandomSequencesState randomSequencesState, CallbackInfo ci) {
        var sw = (ServerWorld) (World) this;
        if (this.getRegistryKey() != World.END && sw.getDimensionKey().getValue() == DimensionTypes.THE_END.getValue()) {
            if (this.enderDragonFight == null && sw.getAliveEnderDragons().isEmpty()) {
                var ident = sw.getRegistryKey().getValue();
                var fight = DragonPersistentState.getServerState(server).getFight(UUID.fromString(ident.getPath()));
                if (fight != null) {
                    this.enderDragonFight = new EnderDragonFight(sw, sw.getSeed(), fight);
                    LoggerFactory.getLogger("mixin").info("Loaded dragon fight {}", this.enderDragonFight);
                } else {
                    var data = EnderDragonFight.Data.DEFAULT;
                    this.enderDragonFight = new EnderDragonFight(sw, this.getSeed(), data);
                    LoggerFactory.getLogger("mixin").info("Created new dragon fight {}", this.enderDragonFight);
                }
            }
        }

    }
}

