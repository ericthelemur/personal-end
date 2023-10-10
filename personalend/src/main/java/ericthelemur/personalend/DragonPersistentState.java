package ericthelemur.personalend;

import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class DragonPersistentState extends PersistentState {

    private HashMap<UUID, EnderDragonFight.Data> fights = new HashMap<>();
    // Records name data for offline players
    // Lazy Bi-directional map
    private HashMap<String, UUID> usernames = new HashMap<>();
    private HashMap<UUID, String> uuids = new HashMap<>();

    // Probably shouldn't be here, but not worth the hassle to move
    private static HashMap<String, ServerWorld> loadedWorlds = new HashMap<>();

    /**
     * Write the data of this state into NBT
     */
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        // Write dragon data from worlds
        syncDragons();
        NbtCompound dragons = new NbtCompound();
        for (var s : fights.entrySet()) {
            dragons.put(s.getKey().toString(), DragonNbtConverter.toNBT(s.getValue()));
        }
        nbt.put("dragons", dragons);

        // Write name data
        NbtCompound names = new NbtCompound();
        for (var s : usernames.entrySet()) {
            names.put(s.getKey(), NbtString.of(s.getValue().toString()));
        }
        nbt.put("names", names);
        return nbt;
    }

    private void syncDragons() {
        for (var dim : loadedWorlds.entrySet()) {
            addFight(dim.getKey(), dim.getValue());
        }
    }

    /**
     * Create a state object from NBT
     */
    public static DragonPersistentState createFromNbt(NbtCompound tag) {
        DragonPersistentState state = new DragonPersistentState();
        var dragons = tag.getCompound("dragons");
        for (var uuid : dragons.getKeys()) {
            state.fights.put(UUID.fromString(uuid), DragonNbtConverter.fromNBT(dragons.getCompound(uuid)));
        }

        var names = tag.getCompound("names");
        for (var name : names.getKeys()) {
            state.addPlayer(UUID.fromString(names.getString(name)), name);
        }
        return state;
    }

    /**
     * Fetches the shared state object (or creates if new)
     */
    public static DragonPersistentState getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        return persistentStateManager.getOrCreate(
                DragonPersistentState::createFromNbt,
                DragonPersistentState::new,
                PersonalEnd.MOD_ID);
    }

    /**
     * Add world to list of loaded dims, for syncing at save
     */
    public void addLoadedWorld(String uuid, ServerWorld world) {
        loadedWorlds.put(uuid, world);
        addPlayer(uuid, world.getServer());
    }

    /**
     * Adds username and uuid to both ways of map (fetches name from server if online (should be))
     */
    public void addPlayer(String uuid, MinecraftServer server) {
        var player = server.getPlayerManager().getPlayer(UUID.fromString(uuid));
        if (player != null) {
            addPlayer(UUID.fromString(uuid), player.getGameProfile().getName());
        }
    }

    /**
     * Adds username and uuid to both ways of map
     */
    public void addPlayer(UUID uuid, String username) {
        usernames.put(username, uuid);
        uuids.put(uuid, username);
    }

    public HashMap<UUID, EnderDragonFight.Data> getFights() {
        return fights;
    }

    public EnderDragonFight.Data getFight(UUID player) {
        return fights.get(player);
    }

    /**
     * Adds the dragon fight data from dimension dim for world of player with uuid
     */
    public void addFight(String uuid, ServerWorld dim) {
        var fight = dim.getEnderDragonFight();
        if (fight != null) {
            this.fights.put(UUID.fromString(uuid), fight.toData());
        }

        addPlayer(uuid, dim.getServer());
    }

    public UUID getUUID(String username) {
        return usernames.get(username);
    }

    public Collection<String> getUsernames() {
        return usernames.keySet();
    }

    public String getUsername(UUID uuid) {
        return uuids.get(uuid);
    }

}