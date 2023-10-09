package ericthelemur.personalend;

import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class DragonPersistentState extends PersistentState {

    private HashMap<UUID, EnderDragonFight.Data> fights = new HashMap<>();
    private HashMap<String, UUID> usernames = new HashMap<>();

    private static HashMap<String, ServerWorld> loadedWorlds = new HashMap<>();

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        syncDragons();
        NbtCompound dragons = new NbtCompound();
        for (var s : fights.entrySet()) {
            dragons.put(s.getKey().toString(), DragonNbtConverter.toNBT(s.getValue()));
        }
        nbt.put("dragons", dragons);

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

    public static DragonPersistentState createFromNbt(NbtCompound tag) {
        DragonPersistentState state = new DragonPersistentState();
        var dragons = tag.getCompound("dragons");
        for (var uuid : dragons.getKeys()) {
            state.fights.put(UUID.fromString(uuid), DragonNbtConverter.fromNBT(dragons.getCompound(uuid)));
        }

        var names = tag.getCompound("names");
        for (var name : names.getKeys()) {
            state.usernames.put(name, UUID.fromString(names.getString(name)));
        }
        return state;
    }

    public static DragonPersistentState getServerState(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        return persistentStateManager.getOrCreate(
                DragonPersistentState::createFromNbt,
                DragonPersistentState::new,
                PersonalEnd.MOD_ID);
    }

    public void addLoadedWorld(String uuid, ServerWorld world) {
        loadedWorlds.put(uuid, world);
        addPlayer(uuid, world.getServer());
    }

    private void addPlayer(String uuid, MinecraftServer server) {
        var username = server.getPlayerManager().getPlayer(UUID.fromString(uuid));
        usernames.put(username.getGameProfile().getName(), UUID.fromString(uuid));
    }

    public HashMap<UUID, EnderDragonFight.Data> getFights() {
        return fights;
    }

    public EnderDragonFight.Data getFight(UUID player) {
        return fights.get(player);
    }

    public void addFight(String uuid, ServerWorld dim) {
        this.fights.put(UUID.fromString(uuid), dim.getEnderDragonFight().toData());
        addPlayer(uuid, dim.getServer());
    }

    public UUID getUUID(String username) {
        return usernames.get(username);
    }

    public void addUUID(String username, UUID uuid) {
        this.usernames.put(username, uuid);
    }

    public Collection<String> getUsernames() {
        return usernames.keySet();
    }

}