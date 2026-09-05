package dev.gtnhjourney.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import dev.gtnhjourney.research.ResearchFingerprint;

/** Per-player exact Journey favourites plus stable favourite-added chronology. */
public final class JourneyFavouriteData extends WorldSavedData {

    public static final String DATA_NAME = "gtnhjourney_favourites";
    private static final int DATA_VERSION = 2;
    private final Map<UUID, LinkedHashMap<ResearchFingerprint, Long>> values =
        new LinkedHashMap<UUID, LinkedHashMap<ResearchFingerprint, Long>>();
    private final Map<UUID, Long> nextSequence = new LinkedHashMap<UUID, Long>();

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
        Map<ResearchFingerprint, Long> set = values.get(playerId);
        return set != null && fingerprint != null && set.containsKey(fingerprint);
    }

    public long addSequence(UUID playerId, ResearchFingerprint fingerprint) {
        Map<ResearchFingerprint, Long> set = values.get(playerId);
        Long sequence = set == null || fingerprint == null ? null : set.get(fingerprint);
        return sequence == null ? -1L : sequence.longValue();
    }

    public boolean set(UUID playerId, ResearchFingerprint fingerprint, boolean favourite) {
        if (playerId == null || fingerprint == null) return false;
        LinkedHashMap<ResearchFingerprint, Long> set = values.get(playerId);
        if (set == null) {
            if (!favourite) return false;
            set = new LinkedHashMap<ResearchFingerprint, Long>();
            values.put(playerId, set);
        }
        final boolean changed;
        if (favourite) {
            if (set.containsKey(fingerprint)) return false;
            long sequence = next(playerId);
            set.put(fingerprint, Long.valueOf(sequence));
            changed = true;
        } else {
            changed = set.remove(fingerprint) != null;
        }
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
        Map<ResearchFingerprint, Long> set = values.get(playerId);
        return set == null ? new ArrayList<ResearchFingerprint>() : new ArrayList<ResearchFingerprint>(set.keySet());
    }

    public List<FavouriteEntry> snapshotEntries(UUID playerId) {
        Map<ResearchFingerprint, Long> set = values.get(playerId);
        List<FavouriteEntry> out = new ArrayList<FavouriteEntry>();
        if (set != null) {
            for (Map.Entry<ResearchFingerprint, Long> entry : set.entrySet()) {
                out.add(new FavouriteEntry(entry.getKey(), entry.getValue().longValue()));
            }
        }
        return out;
    }

    @Override
    public void readFromNBT(NBTTagCompound root) {
        values.clear();
        nextSequence.clear();
        boolean migrated = root.getInteger("Version") != DATA_VERSION;
        NBTTagList players = root.getTagList("Players", 10);
        for (int i = 0; i < players.tagCount(); i++) {
            NBTTagCompound playerTag = players.getCompoundTagAt(i);
            UUID playerId = new UUID(playerTag.getLong("UuidMost"), playerTag.getLong("UuidLeast"));
            NBTTagList list = playerTag.getTagList("Values", 10);
            LinkedHashMap<ResearchFingerprint, Long> set = new LinkedHashMap<ResearchFingerprint, Long>();
            long sequence = 0L;
            for (int j = 0; j < list.tagCount(); j++) {
                NBTTagCompound value = list.getCompoundTagAt(j);
                byte[] bytes = value.getByteArray("Fingerprint");
                if (bytes.length != ResearchFingerprint.BYTE_LENGTH) continue;
                long stored = value.hasKey("AddedSequence", 4) ? value.getLong("AddedSequence") : ++sequence;
                sequence = Math.max(sequence, stored);
                set.put(ResearchFingerprint.fromBytes(bytes), Long.valueOf(stored));
            }
            if (!set.isEmpty()) {
                values.put(playerId, set);
                nextSequence.put(playerId, Long.valueOf(sequence));
            }
        }
        if (migrated) markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound root) {
        root.setInteger("Version", DATA_VERSION);
        NBTTagList players = new NBTTagList();
        for (Map.Entry<UUID, LinkedHashMap<ResearchFingerprint, Long>> entry : values.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            NBTTagCompound playerTag = new NBTTagCompound();
            playerTag.setLong("UuidMost", entry.getKey().getMostSignificantBits());
            playerTag.setLong("UuidLeast", entry.getKey().getLeastSignificantBits());
            NBTTagList list = new NBTTagList();
            for (Map.Entry<ResearchFingerprint, Long> favourite : entry.getValue().entrySet()) {
                NBTTagCompound value = new NBTTagCompound();
                value.setByteArray("Fingerprint", favourite.getKey().toBytes());
                value.setLong("AddedSequence", favourite.getValue().longValue());
                list.appendTag(value);
            }
            playerTag.setTag("Values", list);
            players.appendTag(playerTag);
        }
        root.setTag("Players", players);
    }

    private long next(UUID playerId) {
        Long current = nextSequence.get(playerId);
        long next = current == null ? 1L : current.longValue() + 1L;
        nextSequence.put(playerId, Long.valueOf(next));
        return next;
    }

    public static final class FavouriteEntry {
        private final ResearchFingerprint fingerprint;
        private final long addedSequence;

        public FavouriteEntry(ResearchFingerprint fingerprint, long addedSequence) {
            this.fingerprint = fingerprint;
            this.addedSequence = addedSequence;
        }

        public ResearchFingerprint fingerprint() { return fingerprint; }
        public long addedSequence() { return addedSequence; }
    }
}
