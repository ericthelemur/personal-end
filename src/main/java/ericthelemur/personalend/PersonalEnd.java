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
import net.minecraft.server.network.ServerPlayerEntity;
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

	public static Config CONFIG = new Config();

	@Override
	public void onInitialize() {
		Config.load();

		// Load state and load worlds on start
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			var state = DragonPersistentState.getServerState(server);
			state.markDirty();

			for (var uuid : state.getFights().keySet()) {
				createWorld(server, uuid);
			}
		});

		// Mark state dirty on stop (to ensure save)
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			var state = DragonPersistentState.getServerState(server);
			state.markDirty();
		});

		// Register /end command
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			Commands.register(dispatcher);
		});

		// Save player names and uuids on disconnect
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			var state = DragonPersistentState.getServerState(server);
			if (state.getFight(handler.player.getUuid()) != null) {
				state.addPlayer(handler.player.getUuid(), handler.player.getGameProfile().getName());
				state.markDirty();
			}
		});

		LOGGER.info("Initialized Personal End!");
	}

	/**
	 * Creates/loads the personal End dimension & teleports the player to it
	 * @param visitor Player to teleport to the End dimension
	 * @param owner UUID of dimension owner
	 * @param ownerName Display name of dimension owner
	 */
	public static void genAndGoToEnd(PlayerEntity visitor, UUID owner, String ownerName) {
		if (ownerName == null || owner.equals(visitor.getUuid())) {
			visitor.sendMessage(Text.literal("Visiting your end ..."));
		} else {
			visitor.sendMessage(Text.literal("Visiting " + ownerName + "'s end ..."));
		}

		MinecraftServer server = visitor.getServer();

		LOGGER.info("Generating dimension for " + ownerName + " (" + owner + ")");
		var worldHandle = createWorld(server, owner);
		ServerWorld new_end = worldHandle.asWorld();

		visitor.sendMessage(Text.literal("Teleporting ..."));
		visitor = tpPlayerToEnd(visitor, new_end);
		grantAdvancements((ServerPlayerEntity) visitor);
	}

	/**
	 * Constructs the world config and creates/loads the world with fantasy
	 * @param owner UUID of the dimension owner
	 * @return Handle of the created/loaded world
	 */
	public static RuntimeWorldHandle createWorld(MinecraftServer server, UUID owner) {
		ChunkGenerator end_gen = server.getWorld(World.END).getChunkManager().getChunkGenerator();
		var config = new RuntimeWorldConfig()
				.setDimensionType(DimensionTypes.THE_END)
				.setGenerator(end_gen)
				.setSeed(owner.toString().hashCode());

		Identifier end_key = new Identifier(PersonalEnd.MOD_ID, owner.toString());
		Fantasy fantasy = Fantasy.get(server);
		return fantasy.getOrOpenPersistentWorld(end_key, config);
	}

	/**
	 * Teleports a player into the new end (needs to be manual as internal code works for End only)
	 * @return The new player object in new dimension
	 */
	private static PlayerEntity tpPlayerToEnd(PlayerEntity player, ServerWorld new_end) {
		ServerWorld.createEndSpawnPlatform(new_end);
		Vec3d spawn = ServerWorld.END_SPAWN_POS.toCenterPos();
		spawn = spawn.add(0, -1.5, 0);
		TeleportTarget teleportTarget = new TeleportTarget(spawn, Vec3d.ZERO, 90.0F, 0.0F);
		return FabricDimensions.teleport(player, new_end, teleportTarget);
	}

	/**
	 * Grants the End visiting advancements to the player. Also sends intruction message if it's their first visit
	 */
	private static void grantAdvancements(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		var al = server.getAdvancementLoader();
		var at = player.getAdvancementTracker();
		var a1 = al.get(new Identifier("end/root"));
		if (!at.getProgress(a1).isDone()) {
			at.grantCriterion(a1, "entered_end");
			var a2 = al.get(new Identifier("story/enter_the_end"));
			at.grantCriterion(a2, "entered_end");
			server.getPlayerManager().sendCommandTree(player);

			player.sendMessage(Text.literal(
			"""
					You now have your own personal End to explore, loot & beat!
					Now you've visited your End, use /end shared or /end visit <player> to join others.
					Entering a portal within 30s after another player pulls you to their End too."""
			));
		}
	}

	/**
	 * Teleports a player back to the overworld from portal
	 * (default behaviour sends to shared end from individual Ends)
	 */
	public static void tpToOverworld(Entity entity, MinecraftServer server) {
		ServerWorld serverWorld = server.getOverworld();
		if (serverWorld == null) {
			return;
		}
		var tt = new TeleportTarget(serverWorld.getSpawnPos().toCenterPos(), Vec3d.ZERO, serverWorld.getSpawnAngle(), 0);
		FabricDimensions.teleport(entity, serverWorld, tt);
	}

	/**
	 * Sends a player to the normal End, pretty much how Minecraft's End Portal code works
	 */
	public static void tpPlayerToSharedEnd(PlayerEntity visitor) {
		visitor.sendMessage(Text.literal("Visiting the shared end ..."));

		MinecraftServer server = visitor.getServer();
		visitor.moveToWorld(server.getWorld(World.END));
	}
}