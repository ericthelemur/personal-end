package ericthelemur.personalend.mixin;

import ericthelemur.personalend.DragonPersistentState;
import ericthelemur.personalend.PersonalEnd;
import net.minecraft.block.BlockState;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {
	// To record times for portal tailgating
	private static long lastEntryTime;
	private static UUID lastPlayer;
	private static final long trailTime = 15 * 1000;

	/**
	 * Sends a player to their End from the overworld (entities still go to the shared end)
	 */
	@Inject(at = @At("HEAD"), method = "onEntityCollision", cancellable = true)
	private void sendToEnds(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
		MinecraftServer server = entity.getServer();
		if (PersonalEnd.isPersonalEnd(world)) {
			// Send player from person End to overworld
			PersonalEnd.tpToOverworld(entity, server);
			ci.cancel();
		}

		if (PersonalEnd.CONFIG.redirectPortals) {
			if (entity.isPlayer() && world.getRegistryKey() == World.OVERWORLD) {
				// Send player from overworld to personal End
				var owner = getDimOwner(entity);
				var dstate = DragonPersistentState.getServerState(server);
				PersonalEnd.genAndGoToEnd((PlayerEntity) entity, owner, dstate.getUsername(owner));
				ci.cancel();
			}
			// Non-player and other dims behave as default
		} else if (entity.isPlayer() && !entity.hasPortalCooldown()) {
			// Send message to player going to shared End
			entity.sendMessage(Text.literal("Visiting the shared End, use /end visit to visit your personal End."));
		}
	}

	/**
	 * Implement the tailgating logic, send to own unless time is within window of entry
	 */
	private UUID getDimOwner(Entity entity) {
		long t = System.currentTimeMillis();
		UUID owner = entity.getUuid();
		if (lastPlayer != null && t < lastEntryTime + trailTime) {
			owner = lastPlayer;
		}
		lastEntryTime = t;
		lastPlayer = owner;
		return owner;
	}
}