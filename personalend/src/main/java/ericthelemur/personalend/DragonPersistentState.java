package ericthelemur.personalend;

import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.UUID;

public class DragonPersistentState extends PersistentState {

    public HashMap<UUID, EnderDragonFight.Data> fights = new HashMap<>();

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        for (var s : fights.entrySet()) {
            nbt.put(s.getKey().toString(), DragonNbtConverter.toNBT(s.getValue()));
        }
        return nbt;
    }

    public static DragonPersistentState createFromNbt(NbtCompound tag) {
        DragonPersistentState state = new DragonPersistentState();
        for (var uuid : tag.getKeys()) {
            state.fights.put(UUID.fromString(uuid), DragonNbtConverter.fromNBT(tag.getCompound(uuid)));
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

    public HashMap<UUID, EnderDragonFight.Data> getFights() {
        return fights;
    }

    public EnderDragonFight.Data getFight(UUID player) {
        return fights.get(player);
    }

    public void addFight(UUID player, EnderDragonFight.Data data) {
        this.fights.put(player, data);
    }
}