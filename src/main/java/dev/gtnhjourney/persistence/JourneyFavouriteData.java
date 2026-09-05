package dev.gtnhjourney.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import dev.gtnhjourney.research.ResearchFingerprint;

/** Per-player exact Journey favourites, stored as fixed-size research fingerprints. */
public final class JourneyFavouriteData extends WorldSavedData {

    public static final String DATA_NAME = "gtnhjourney_favourites";
    private static final int DATA_VERSION = 1;
    private final Map<UUID, LinkedHashSet<ResearchFingerprint>> values = new LinkedHashMap<UUID, LinkedHashSet<ResearchFingerprint>>();

    public JourneyFavouriteData() { super(DATA_NAME); }
    public JourneyFavouriteData(String name) { super(name); }

    public static JourneyFavouriteData get(World world) {
        if (world == null) throw new IllegalArgumentException("world must not be null");
        MapStorage storage = world.mapStorage;
        JourneyFavouriteData data = (JourneyFavouriteData) storage.loadData(JourneyFavouriteData.class, DATA_NAME);
        if (data == null) {
            data = new JourneyFavouriteData(DATA_NAME);
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public boolean contains(UUID playerId, ResearchFingerprint fingerprint) {
        Set<ResearchFingerprint> set = values.get(playerId);
        return set != null && fingerprint != null && set.contains(fingerprint);
    }

    public boolean set(UUID playerId, ResearchFingerprint fingerprint, boolean favourite) {
        if (playerId == null || fingerprint == null) return false;
        LinkedHashSet<ResearchFingerprint> set = values.get(playerId);
        if (set == null) {
            if (!favourite) return false;
            set = new LinkedHashSet<ResearchFingerprint>();
            values.put(playerId, set);
        }
        boolean changed = favourite ? set.add(fingerprint) : set.remove(fingerprint);
        if (set.isEmpty()) values.remove(playerId);
        if (changed) markDirty();
        return changed;
    }

    public boolean toggle(UUID playerId, ResearchFingerprint fingerprint) {
        boolean next = !contains(playerId, fingerprint);
        set(playerId, fingerprint, next);
        return next;
    }

    public List<ResearchFingerprint> snapshot(UUID playerId) {
        Set<ResearchFingerprint> set = values.get(playerId);
        return set == null ? new ArrayList<ResearchFingerprint>() : new ArrayList<ResearchFingerprint>(set);
    }

    @Override
    public void readFromNBT(NBTTagCompound root) {
        values.clear();
        NBTTagList players = root.getTagList("Players", 10);
        for (int i = 0; i < players.tagCount(); i++) {
            NBTTagCompound playerTag = players.getCompoundTagAt(i);
            UUID playerId = new UUID(playerTag.getLong("UuidMost"), playerTag.getLong("UuidLeast"));
            NBTTagList list = playerTag.getTagList("Values", 10);
            LinkedHashSet<ResearchFingerprint> set = new LinkedHashSet<ResearchFingerprint>();
            for (int j = 0; j < list.tagCount(); j++) {
                byte[] bytes = list.getCompoundTagAt(j).getByteArray("Fingerprint");
                if (bytes.length == ResearchFingerprint.BYTE_LENGTH) set.add(ResearchFingerprint.fromBytes(bytes));
            }
            if (!set.isEmpty()) values.put(playerId, set);
        }
        if (root.getInteger("Version") != DATA_VERSION) markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound root) {
        root.setInteger("Version", DATA_VERSION);
        NBTTagList players = new NBTTagList();
        for (Map.Entry<UUID, LinkedHashSet<ResearchFingerprint>> entry : values.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            NBTTagCompound playerTag = new NBTTagCompound();
            playerTag.setLong("UuidMost", entry.getKey().getMostSignificantBits());
            playerTag.setLong("UuidLeast", entry.getKey().getLeastSignificantBits());
            NBTTagList list = new NBTTagList();
            for (ResearchFingerprint fingerprint : entry.getValue()) {
                NBTTagCompound value = new NBTTagCompound();
                value.setByteArray("Fingerprint", fingerprint.toBytes());
                list.appendTag(value);
            }
            playerTag.setTag("Values", list);
            players.appendTag(playerTag);
        }
        root.setTag("Players", players);
    }
}
