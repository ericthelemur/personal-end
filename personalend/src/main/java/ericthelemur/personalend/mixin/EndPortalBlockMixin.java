package ericthelemur.personalend.mixin;

import ericthelemur.personalend.PersonalEnd;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.block.BlockState;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {
	@Inject(at = @At("HEAD"), method = "onEntityCollision", cancellable = true)
	private void disableEndPortal(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
		MinecraftServer server = entity.getServer();
		if (entity.isPlayer() && world.getRegistryKey() == World.OVERWORLD) {
			PersonalEnd.genAndGoToEnd((PlayerEntity) entity, entity.getUuid(), null);
		} else if (world.getDimensionKey().getValue() == DimensionTypes.THE_END.getValue()) {
			PersonalEnd.tpToOverworld(entity, server);
		}
		ci.cancel();
	}
}