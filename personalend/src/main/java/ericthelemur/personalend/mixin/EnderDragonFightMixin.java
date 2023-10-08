package ericthelemur.personalend.mixin;

import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.entity.boss.dragon.EnderDragonSpawnState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonFight.class)
public class EnderDragonFightMixin {

    @Shadow
    private EnderDragonSpawnState dragonSpawnState;

    @Inject(at = @At("TAIL"), method = "<init>(Lnet/minecraft/server/world/ServerWorld;JLnet/minecraft/entity/boss/dragon/EnderDragonFight$Data;Lnet/minecraft/util/math/BlockPos;)V")
    public void constructorMixin(ServerWorld world, long gatewaysSeed, EnderDragonFight.Data data, BlockPos origin, CallbackInfo ci){
        // Is custom dim
        if (world.getRegistryKey() != World.END && world.getDimensionKey().getValue() == DimensionTypes.THE_END.getValue()) {
            if (!world.getAliveEnderDragons().isEmpty()) {
                this.dragonSpawnState = EnderDragonSpawnState.END;
            }
        }
    }
}
