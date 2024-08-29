package ericthelemur.personalend.mixin;

import ericthelemur.personalend.DragonPersistentState;
import ericthelemur.personalend.PersonalEnd;
import net.minecraft.block.BlockState;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.block.Portal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.dimension.PortalManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(EndPortalBlock.class)
public abstract class EndPortalBlockMixin {
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

		if (PersonalEnd.CONFIG.redirectPortals || PersonalEnd.isPersonalEnd(world)) {
			if (entity.canUsePortals(false) && VoxelShapes.matchesAnywhere(VoxelShapes.cuboid(entity.getBoundingBox().offset((double)(-pos.getX()), (double)(-pos.getY()), (double)(-pos.getZ()))), state.getOutlineShape(world, pos), BooleanBiFunction.AND)) {
				if (!world.isClient && PersonalEnd.isPersonalEnd(world) && entity instanceof ServerPlayerEntity player && !player.seenCredits) {
					player.detachForDimensionChange();
					ci.cancel();
					return;
				}

				// From Entity.tryUsePortal
				if (entity.hasPortalCooldown()) {
					entity.resetPortalCooldown();
				} else {
					if (entity.portalManager != null && entity.portalManager.portalMatches((Portal) this)) {
						entity.portalManager.setPortalPos(pos.toImmutable());
						entity.portalManager.setInPortal(true);
					} else {
						entity.portalManager = new PortalManager((Portal) this, pos.toImmutable());
					}

				}
			}

			ci.cancel();
		}
	}

	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public TeleportTarget createTeleportTarget(ServerWorld world, Entity entity, BlockPos pos) {
		if (PersonalEnd.isAnyEnd(world)) {
			// In end, tp to overworld
			return PersonalEnd.createOverworldTeleportTarget(entity, world.getServer());
		} else {
			// In Overworld
			if (PersonalEnd.CONFIG.redirectPortals && entity.isPlayer()) {
				// Teleport to personal end if player
				var owner = getDimOwner(entity);
				var dstate = DragonPersistentState.getServerState(world.getServer());
				return PersonalEnd.genAndGoToEnd((PlayerEntity) entity, owner, dstate.getUsername(owner));
			} else {
				// If not redirecting or not player, just go to shared end
				if (entity.isPlayer()) 	entity.sendMessage(Text.literal("Visiting the shared End, use /end visit to visit your personal End."));
				return PersonalEnd.createEndTeleportTarget(entity, world.getServer().getWorld(World.END));
			}
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