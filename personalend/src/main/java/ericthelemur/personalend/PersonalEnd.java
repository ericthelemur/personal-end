package ericthelemur.personalend;

import ericthelemur.personalend.commands.Commands;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

import java.util.UUID;

public class PersonalEnd implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("personal-end");
	public static final String MOD_ID = "personalend";

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			var state = DragonPersistentState.getServerState(server);
			state.markDirty();
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			var state = DragonPersistentState.getServerState(server);
			state.markDirty();
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			Commands.register(dispatcher);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			var state = DragonPersistentState.getServerState(server);
			if (state.getFight(handler.player.getUuid()) != null) {
				state.addPlayer(handler.player.getUuid(), handler.player.getGameProfile().getName());
				state.markDirty();
			}
		});

		LOGGER.info("Initialized Personal End!");
	}

	public static void genAndGoToEnd(PlayerEntity visitor, UUID owner, String ownerName) {
		if (ownerName == null || owner.equals(visitor.getUuid())) {
			visitor.sendMessage(Text.literal("Visiting your end ..."));
		} else {
			visitor.sendMessage(Text.literal("Visiting " + ownerName + "'s end ..."));
		}

		MinecraftServer server = visitor.getServer();
		Fantasy fantasy = Fantasy.get(server);

		Identifier end_key = new Identifier(PersonalEnd.MOD_ID, owner.toString());

		Long end_seed = (long) owner.toString().hashCode();

		LOGGER.info("Generating dimension for " + ownerName + " (" + owner + ")");
		ChunkGenerator end_gen = server.getWorld(World.END).getChunkManager().getChunkGenerator();
		RuntimeWorldConfig config = new RuntimeWorldConfig()
				.setDimensionType(DimensionTypes.THE_END)
				.setGenerator(end_gen)
				.setSeed(end_seed);

		RuntimeWorldHandle worldHandle = fantasy.getOrOpenPersistentWorld(end_key, config);

		ServerWorld new_end = worldHandle.asWorld();
		visitor.sendMessage(Text.literal(String.format("Teleporting ...")));
		tpPlayerToEnd(visitor, new_end);
	}

	private static void tpPlayerToEnd(PlayerEntity player, ServerWorld new_end) {
		ServerWorld.createEndSpawnPlatform(new_end);
		Vec3d spawn = ServerWorld.END_SPAWN_POS.toCenterPos();
		spawn = spawn.add(0, -1.5, 0);
		TeleportTarget teleportTarget = new TeleportTarget(spawn, Vec3d.ZERO, 90.0F, 0.0F);
		player = FabricDimensions.teleport(player, new_end, teleportTarget);
	}

	public static void tpToOverworld(Entity entity, MinecraftServer server) {
		ServerWorld serverWorld = server.getOverworld();
		if (serverWorld == null) {
			return;
		}
		var tt = new TeleportTarget(serverWorld.getSpawnPos().toCenterPos(), Vec3d.ZERO, serverWorld.getSpawnAngle(), 0);
		FabricDimensions.teleport(entity, serverWorld, tt);
	}
}