package ericthelemur.personalend.mixin;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.BlockState;
import net.minecraft.block.EndPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.Iterator;

@Mixin(EndPortalBlock.class)
public class EndPortalBlockMixin {
	@Inject(at = @At("HEAD"), method = "onEntityCollision", cancellable = true)
	private void disableEndPortal(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
		if (entity.isPlayer()) {
			PlayerEntity player = (PlayerEntity) entity;
			MinecraftServer server = entity.getServer();
            Fantasy fantasy = Fantasy.get(server);

			Identifier end_key = new Identifier("personalend", player.getUuidAsString());
			Long end_seed = (long) player.getUuidAsString().hashCode();

			ChunkGenerator end_gen = server.getWorld(World.END).getChunkManager().getChunkGenerator();
			RuntimeWorldConfig config = new RuntimeWorldConfig()
					.setDimensionType(DimensionTypes.THE_END)
					.setGenerator(end_gen)
					.setSeed(end_seed);

			RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, end_key);
			boolean endExistsAlready = worldKey != null;

			RuntimeWorldHandle worldHandle = fantasy.getOrOpenPersistentWorld(end_key, config);

			ServerWorld new_end = worldHandle.asWorld();
			if (!endExistsAlready) {
				setupEnd(new_end);
			}

			player = (PlayerEntity) movePlayer(new_end, server.getPlayerManager().getPlayer(player.getUuid()));
		}
		ci.cancel();
	}
	
	private Entity movePlayer(ServerWorld new_end, ServerPlayerEntity player) {
		// Modified from ServerPlayerEntity.moveToWorld
		ServerWorld serverWorld = player.getServerWorld();

		WorldProperties worldProperties = new_end.getLevelProperties();
		player.networkHandler.sendPacket(new PlayerRespawnS2CPacket(new_end.getDimensionKey(), new_end.getRegistryKey(), BiomeAccess.hashSeed(new_end.getSeed()), player.interactionManager.getGameMode(), player.interactionManager.getPreviousGameMode(), new_end.isDebugWorld(), new_end.isFlat(), (byte)3, player.getLastDeathPos(), player.getPortalCooldown()));
		player.networkHandler.sendPacket(new DifficultyS2CPacket(worldProperties.getDifficulty(), worldProperties.isDifficultyLocked()));
		PlayerManager playerManager = player.server.getPlayerManager();
		playerManager.sendCommandTree(player);
		serverWorld.removePlayer(player, Entity.RemovalReason.CHANGED_DIMENSION);
		player.setRemoved(null);
		Vec3d pos = ServerWorld.END_SPAWN_POS.toCenterPos();
		pos = pos.add(0, -1.5, 0);
		TeleportTarget teleportTarget = new TeleportTarget(pos, Vec3d.ZERO, 90.0F, 0.0F);

        serverWorld.getProfiler().push("moving");
        ServerWorld.createEndSpawnPlatform(new_end);

        serverWorld.getProfiler().pop();
        serverWorld.getProfiler().push("placing");
        player.setServerWorld(new_end);
        player.networkHandler.requestTeleport(teleportTarget.position.x, teleportTarget.position.y, teleportTarget.position.z, teleportTarget.yaw, teleportTarget.pitch);
        player.networkHandler.syncWithPlayerPosition();
        new_end.onPlayerChangeDimension(player);
        serverWorld.getProfiler().pop();
        Criteria.CHANGED_DIMENSION.trigger(player, serverWorld.getRegistryKey(), new_end.getRegistryKey());
        player.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(player.getAbilities()));
        playerManager.sendWorldInfo(player, new_end);
        playerManager.sendPlayerStatus(player);

        for (StatusEffectInstance statusEffectInstance : player.getStatusEffects()) {
            player.networkHandler.sendPacket(new EntityStatusEffectS2CPacket(player.getId(), statusEffectInstance));
        }

        player.networkHandler.sendPacket(new WorldEventS2CPacket(1032, BlockPos.ORIGIN, 0, false));

        return player;
	}
	
	private void setupEnd(ServerWorld new_end) {
		new_end.setEnderDragonFight(new EnderDragonFight(new_end, new_end.getSeed(), EnderDragonFight.Data.DEFAULT));
	}
}