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
			PlayerEntity player = (PlayerEntity) entity;
            Fantasy fantasy = Fantasy.get(server);

			Identifier end_key = new Identifier(PersonalEnd.MOD_ID, player.getUuidAsString());
			Long end_seed = (long) player.getUuidAsString().hashCode();

			ChunkGenerator end_gen = server.getWorld(World.END).getChunkManager().getChunkGenerator();
			RuntimeWorldConfig config = new RuntimeWorldConfig()
					.setDimensionType(DimensionTypes.THE_END)
					.setGenerator(end_gen)
					.setSeed(end_seed);

			RuntimeWorldHandle worldHandle = fantasy.getOrOpenPersistentWorld(end_key, config);

			ServerWorld new_end = worldHandle.asWorld();
			PersonalEnd.loaded_worlds.put(player.getUuidAsString(), new_end);
			teleportPlayer(player, new_end);
		} else if (world.getDimensionKey().getValue() == DimensionTypes.THE_END.getValue()) {
			ServerWorld serverWorld = server.getOverworld();
			if (serverWorld == null) {
				return;
			}
			var tt = new TeleportTarget(serverWorld.getSpawnPos().toCenterPos(), Vec3d.ZERO, serverWorld.getSpawnAngle(), 0);
			FabricDimensions.teleport(entity, serverWorld, tt);
		}
		ci.cancel();
	}

	private void teleportPlayer(PlayerEntity player, ServerWorld new_end) {
		ServerWorld.createEndSpawnPlatform(new_end);
		Vec3d spawn = ServerWorld.END_SPAWN_POS.toCenterPos();
		spawn = spawn.add(0, -1.5, 0);
		TeleportTarget teleportTarget = new TeleportTarget(spawn, Vec3d.ZERO, 90.0F, 0.0F);
		player = FabricDimensions.teleport(player, new_end, teleportTarget);
	}
}