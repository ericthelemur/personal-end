package ericthelemur.personalend;

import ericthelemur.personalend.commands.Commands;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.EndPlatformFeature;
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

	public static boolean isAnyEnd(World world) {
		return world.getRegistryKey() == World.END || PersonalEnd.isPersonalEnd(world);
	}

	public static boolean isPersonalEnd(World world) {
		return PersonalEnd.MOD_ID.equals(world.getRegistryKey().getValue().getNamespace());
	}

	/**
	 * Creates/loads the personal End dimension & teleports the player to it
	 * @param visitor Player to teleport to the End dimension
	 * @param owner UUID of dimension owner
	 * @param ownerName Display name of dimension owner
	 */
	public static TeleportTarget genAndGoToEnd(PlayerEntity visitor, UUID owner, String ownerName) {
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
//		visitor = (PlayerEntity) visitor.teleportTo(createEndTeleportTarget(visitor, new_end));
//		grantAdvancements((ServerPlayerEntity) visitor);
		return createEndTeleportTarget(visitor, new_end);
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

		Identifier end_key = Identifier.of(PersonalEnd.MOD_ID, owner.toString());
		Fantasy fantasy = Fantasy.get(server);
		return fantasy.getOrOpenPersistentWorld(end_key, config);
	}

	/**
	 * Teleports a player into the new end (needs to be manual as internal code works for End only)
	 * @return The new player object in new dimension
	 */
	public static TeleportTarget createEndTeleportTarget(Entity entity, ServerWorld new_end) {
		Vec3d vec3d = ServerWorld.END_SPAWN_POS.toBottomCenterPos();
		EndPlatformFeature.generate(new_end, BlockPos.ofFloored(vec3d).down(), true);

		var tt = new TeleportTarget(new_end, vec3d, entity.getVelocity(), Direction.WEST.asRotation(), entity.getPitch(),
								TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET.then(TeleportTarget.ADD_PORTAL_CHUNK_TICKET));
		if (entity.isPlayer()) grantAdvancements((ServerPlayerEntity) entity, PersonalEnd.isPersonalEnd(new_end));
		return tt;
	}

	/**
	 * Teleports a player back to the overworld from portal
	 * (default behaviour sends to shared end from individual Ends)
	 */
	public static TeleportTarget createOverworldTeleportTarget(Entity entity, MinecraftServer server) {
		ServerWorld serverWorld = server.getOverworld();
		if (serverWorld == null) return null;

		TeleportTarget tt;
		if (entity instanceof ServerPlayerEntity) {
			ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)entity;
			tt = serverPlayerEntity.getRespawnTarget(false, TeleportTarget.NO_OP);
		} else {
			var pos = entity.getWorldSpawnPos(serverWorld, serverWorld.getSpawnPos()).toBottomCenterPos();
			tt = new TeleportTarget(serverWorld, pos, entity.getVelocity(), 90F, entity.getPitch(), TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET.then(TeleportTarget.ADD_PORTAL_CHUNK_TICKET));
		}
		return tt;
	}

	/**
	 * Sends a player to the normal End, pretty much how Minecraft's End Portal code works
	 */
	public static void tpPlayerToSharedEnd(PlayerEntity visitor) {
		visitor.sendMessage(Text.literal("Visiting the shared end ..."));

		MinecraftServer server = visitor.getServer();
		visitor.teleportTo(createEndTeleportTarget(visitor, server.getWorld(World.END)));
	}

	/**
	 * Grants the End visiting advancements to the player. Also sends instruction message if it's their first visit
	 */
	private static void grantAdvancements(ServerPlayerEntity player, boolean isPersonal) {
		MinecraftServer server = player.getServer();
		var al = server.getAdvancementLoader();
		var at = player.getAdvancementTracker();
		var a1 = al.get(Identifier.ofVanilla("end/root"));
		if (!at.getProgress(a1).isDone()) {
			at.grantCriterion(a1, "entered_end");
			var a2 = al.get(Identifier.ofVanilla("story/enter_the_end"));
			at.grantCriterion(a2, "entered_end");
			server.getPlayerManager().sendCommandTree(player);

			var stringBuilder = new StringBuilder();
			stringBuilder.append("You now have your own personal End to explore, loot & beat!\n");
			if (!isPersonal) {
				stringBuilder.append("Now you've visited the shared End, use /end visit to visit your personal End");
			} else {
				stringBuilder.append("Now you've visited your End, use /end shared to visit the shared End");
			}
			stringBuilder.append(", or /end visit <player> to join others.\n");
			stringBuilder.append("Entering a portal within 30s after another player pulls you to their End too.");

			player.sendMessage(Text.literal(stringBuilder.toString()));
		}
	}
}