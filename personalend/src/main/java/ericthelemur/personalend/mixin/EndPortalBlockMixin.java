package ericthelemur.personalend.mixin;

import ericthelemur.personalend.DragonPersistentState;
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

import java.util.UUID;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {
	private static long lastEntryTime;
	private static UUID lastPlayer;
	private static final long trailTime = 15 * 1000;

	@Inject(at = @At("HEAD"), method = "onEntityCollision", cancellable = true)
	private void disableEndPortal(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
		MinecraftServer server = entity.getServer();
		if (entity.isPlayer() && world.getRegistryKey() == World.OVERWORLD) {
			PersonalEnd.LOGGER.info("Joining world as {} {}", entity.getName().getString(), server.getTickTime());
			var owner = getDimOwner(entity);
			var dstate = DragonPersistentState.getServerState(server);
			PersonalEnd.genAndGoToEnd((PlayerEntity) entity, owner, dstate.getUsername(owner));
			ci.cancel();
		} else if (world.getDimensionKey().getValue() == DimensionTypes.THE_END.getValue()) {
			PersonalEnd.tpToOverworld(entity, server);
			ci.cancel();
		}
	}

	private UUID getDimOwner(Entity entity) {
		long t = System.currentTimeMillis();
		PersonalEnd.LOGGER.info("Times base {} limit {} current {} last {}", lastEntryTime, lastEntryTime + trailTime, t, lastPlayer);
		UUID owner = entity.getUuid();
		if (lastPlayer != null && t < lastEntryTime + trailTime) {
			owner = lastPlayer;
		}
		lastEntryTime = t;
		lastPlayer = owner;
		return owner;
	}
}