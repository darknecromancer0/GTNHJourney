package dev.gtnhjourney.persistence;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import dev.gtnhjourney.recovery.DeathInventorySnapshot;

/** Latest pre/post keepInventory death snapshot per player. */
public final class DeathInventoryRecoveryData extends WorldSavedData {

    public static final String DATA_NAME = "gtnhjourney_death_inventory";
    private static final int DATA_VERSION = 1;
    private final Map<UUID, Record> records = new LinkedHashMap<UUID, Record>();

    public DeathInventoryRecoveryData() { super(DATA_NAME); }
    public DeathInventoryRecoveryData(String name) { super(name); }

    public static DeathInventoryRecoveryData get(World world) {
        if (world == null) throw new IllegalArgumentException("world must not be null");
        MapStorage storage = world.mapStorage;
        DeathInventoryRecoveryData data = (DeathInventoryRecoveryData) storage.loadData(DeathInventoryRecoveryData.class, DATA_NAME);
        if (data == null) {
            data = new DeathInventoryRecoveryData(DATA_NAME);
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public void capturePre(UUID playerId, long timestamp, DeathInventorySnapshot pre) {
        if (playerId == null || pre == null) return;
        records.put(playerId, new Record(timestamp, pre, null, false));
        markDirty();
    }

    public void capturePost(UUID playerId, DeathInventorySnapshot post, boolean mismatch) {
        if (playerId == null || post == null) return;
        Record old = records.get(playerId);
        if (old == null || old.pre() == null) return;
        records.put(playerId, new Record(old.timestamp(), old.pre(), post, mismatch));
        markDirty();
    }

    public Record record(UUID playerId) { return records.get(playerId); }

    @Override
    public void readFromNBT(NBTTagCompound root) {
        records.clear();
        NBTTagList list = root.getTagList("Players", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            UUID playerId = new UUID(tag.getLong("UuidMost"), tag.getLong("UuidLeast"));
            DeathInventorySnapshot pre = DeathInventorySnapshot.fromNbt(tag.getCompoundTag("Pre"));
            DeathInventorySnapshot post = tag.hasKey("Post", 10)
                ? DeathInventorySnapshot.fromNbt(tag.getCompoundTag("Post"))
                : null;
            records.put(playerId, new Record(tag.getLong("Timestamp"), pre, post, tag.getBoolean("Mismatch")));
        }
        if (root.getInteger("Version") != DATA_VERSION) markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound root) {
        root.setInteger("Version", DATA_VERSION);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<UUID, Record> entry : records.entrySet()) {
            Record record = entry.getValue();
            if (record == null || record.pre() == null) continue;
            NBTTagCompound tag = new NBTTagCompound();
            tag.setLong("UuidMost", entry.getKey().getMostSignificantBits());
            tag.setLong("UuidLeast", entry.getKey().getLeastSignificantBits());
            tag.setLong("Timestamp", record.timestamp());
            tag.setBoolean("Mismatch", record.mismatch());
            tag.setTag("Pre", record.pre().toNbt());
            if (record.post() != null) tag.setTag("Post", record.post().toNbt());
            list.appendTag(tag);
        }
        root.setTag("Players", list);
    }

    public static final class Record {
        private final long timestamp;
        private final DeathInventorySnapshot pre;
        private final DeathInventorySnapshot post;
        private final boolean mismatch;

        Record(long timestamp, DeathInventorySnapshot pre, DeathInventorySnapshot post, boolean mismatch) {
            this.timestamp = timestamp;
            this.pre = pre;
            this.post = post;
            this.mismatch = mismatch;
        }

        public long timestamp() { return timestamp; }
        public DeathInventorySnapshot pre() { return pre; }
        public DeathInventorySnapshot post() { return post; }
        public boolean mismatch() { return mismatch; }
    }
}
