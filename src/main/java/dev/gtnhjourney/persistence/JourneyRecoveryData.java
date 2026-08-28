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

import dev.gtnhjourney.recovery.DeletionRecord;
import dev.gtnhjourney.recovery.DeletionStateChange;
import dev.gtnhjourney.recovery.ResearchEntrySnapshot;
import dev.gtnhjourney.recovery.ResearchTransaction;
import dev.gtnhjourney.research.ResearchKey;

/** Persistent bounded recovery journals, separate from authoritative research data. */
public final class JourneyRecoveryData extends WorldSavedData {

    public static final String DATA_NAME = "gtnhjourney_recovery";
    private static final int DATA_VERSION = 3;
    private static final int MAX_TRANSACTIONS = 100;
    private static final int MAX_DELETION_RECORDS = 1000;

    private final Map<UUID, PlayerHistory> histories = new LinkedHashMap<UUID, PlayerHistory>();

    public JourneyRecoveryData() {
        super(DATA_NAME);
    }

    public JourneyRecoveryData(String name) {
        super(name);
    }

    public static JourneyRecoveryData get(World world) {
        if (world == null) throw new IllegalArgumentException("world must not be null");
        MapStorage storage = world.mapStorage;
        JourneyRecoveryData data = (JourneyRecoveryData) storage.loadData(JourneyRecoveryData.class, DATA_NAME);
        if (data == null) {
            data = new JourneyRecoveryData(DATA_NAME);
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public void pushUndo(UUID playerId, ResearchTransaction transaction) {
        if (playerId == null || transaction == null) return;
        PlayerHistory history = history(playerId);
        history.undo.add(transaction);
        trimOldest(history.undo);
        markDirty();
    }

    public ResearchTransaction popUndo(UUID playerId) {
        PlayerHistory history = histories.get(playerId);
        if (history == null || history.undo.isEmpty()) return null;
        ResearchTransaction transaction = history.undo.remove(history.undo.size() - 1);
        pruneEmpty(playerId, history);
        markDirty();
        return transaction;
    }

    public void pushRedo(UUID playerId, ResearchTransaction transaction) {
        if (playerId == null || transaction == null) return;
        PlayerHistory history = history(playerId);
        history.redo.add(transaction);
        trimOldest(history.redo);
        markDirty();
    }

    public ResearchTransaction popRedo(UUID playerId) {
        PlayerHistory history = histories.get(playerId);
        if (history == null || history.redo.isEmpty()) return null;
        ResearchTransaction transaction = history.redo.remove(history.redo.size() - 1);
        pruneEmpty(playerId, history);
        markDirty();
        return transaction;
    }

    public void clearRedo(UUID playerId) {
        PlayerHistory history = histories.get(playerId);
        if (history == null || history.redo.isEmpty()) return;
        history.redo.clear();
        pruneEmpty(playerId, history);
        markDirty();
    }

    public int undoDepth(UUID playerId) {
        PlayerHistory history = histories.get(playerId);
        return history == null ? 0 : history.undo.size();
    }

    public int redoDepth(UUID playerId) {
        PlayerHistory history = histories.get(playerId);
        return history == null ? 0 : history.redo.size();
    }

    public void appendDeletion(UUID playerId, DeletionRecord record) {
        if (playerId == null || record == null) return;
        PlayerHistory history = history(playerId);
        history.deletions.add(record);
        trimDeletions(history.deletions);
        markDirty();
    }

    public List<DeletionRecord> deletions(UUID playerId) {
        PlayerHistory history = histories.get(playerId);
        return history == null ? new ArrayList<DeletionRecord>() : new ArrayList<DeletionRecord>(history.deletions);
    }

    public List<DeletionRecord> newestActiveDeletions(UUID playerId, int limit) {
        List<DeletionRecord> result = new ArrayList<DeletionRecord>();
        PlayerHistory history = histories.get(playerId);
        if (history == null || limit <= 0) return result;
        int capped = Math.min(MAX_DELETION_RECORDS, limit);
        for (int i = history.deletions.size() - 1; i >= 0 && result.size() < capped; i--) {
            DeletionRecord record = history.deletions.get(i);
            if (record.active()) result.add(record);
        }
        return result;
    }

    public boolean markDeletionInactiveForPresentKey(UUID playerId, ResearchKey key) {
        return setNewestMatchingActivity(playerId, key, true, false);
    }

    public boolean markNewestDeletionActiveForAbsentKey(UUID playerId, ResearchKey key) {
        return setNewestMatchingActivity(playerId, key, false, true);
    }

    public boolean setDeletionActive(UUID playerId, long deletionId, boolean active) {
        PlayerHistory history = histories.get(playerId);
        if (history == null) return false;
        for (int i = history.deletions.size() - 1; i >= 0; i--) {
            DeletionRecord record = history.deletions.get(i);
            if (record.id() != deletionId) continue;
            if (record.active() == active) return false;
            history.deletions.set(i, record.withActive(active));
            markDirty();
            return true;
        }
        return false;
    }

    public int activeDeletionCount(UUID playerId) {
        PlayerHistory history = histories.get(playerId);
        if (history == null) return 0;
        int count = 0;
        for (DeletionRecord record : history.deletions) if (record.active()) count++;
        return count;
    }

    public int deletionCount(UUID playerId) {
        PlayerHistory history = histories.get(playerId);
        return history == null ? 0 : history.deletions.size();
    }

    @Override
    public void readFromNBT(NBTTagCompound root) {
        histories.clear();
        NBTTagList players = root.getTagList("Players", 10);
        for (int i = 0; i < players.tagCount(); i++) {
            NBTTagCompound playerTag = players.getCompoundTagAt(i);
            UUID playerId = new UUID(playerTag.getLong("UuidMost"), playerTag.getLong("UuidLeast"));
            PlayerHistory history = new PlayerHistory();
            readTransactions(playerTag.getTagList("Undo", 10), history.undo);
            readTransactions(playerTag.getTagList("Redo", 10), history.redo);
            readDeletions(playerTag.getTagList("Deletions", 10), history.deletions);
            trimOldest(history.undo);
            trimOldest(history.redo);
            trimDeletions(history.deletions);
            if (!isEmpty(history)) histories.put(playerId, history);
        }
        if (root.getInteger("Version") != DATA_VERSION) markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound root) {
        root.setInteger("Version", DATA_VERSION);
        NBTTagList players = new NBTTagList();
        for (Map.Entry<UUID, PlayerHistory> mapEntry : histories.entrySet()) {
            PlayerHistory history = mapEntry.getValue();
            if (history == null || isEmpty(history)) continue;
            UUID playerId = mapEntry.getKey();
            NBTTagCompound playerTag = new NBTTagCompound();
            playerTag.setLong("UuidMost", playerId.getMostSignificantBits());
            playerTag.setLong("UuidLeast", playerId.getLeastSignificantBits());
            playerTag.setTag("Undo", writeTransactions(history.undo));
            playerTag.setTag("Redo", writeTransactions(history.redo));
            playerTag.setTag("Deletions", writeDeletions(history.deletions));
            players.appendTag(playerTag);
        }
        root.setTag("Players", players);
    }

    private boolean setNewestMatchingActivity(UUID playerId, ResearchKey key, boolean current, boolean next) {
        if (playerId == null || key == null) return false;
        PlayerHistory history = histories.get(playerId);
        if (history == null) return false;
        for (int i = history.deletions.size() - 1; i >= 0; i--) {
            DeletionRecord record = history.deletions.get(i);
            if (record.active() == current && record.entry().key().equals(key)) {
                history.deletions.set(i, record.withActive(next));
                markDirty();
                return true;
            }
        }
        return false;
    }

    private PlayerHistory history(UUID playerId) {
        PlayerHistory history = histories.get(playerId);
        if (history == null) {
            history = new PlayerHistory();
            histories.put(playerId, history);
        }
        return history;
    }

    private void pruneEmpty(UUID playerId, PlayerHistory history) {
        if (isEmpty(history)) histories.remove(playerId);
    }

    private static boolean isEmpty(PlayerHistory history) {
        return history.undo.isEmpty() && history.redo.isEmpty() && history.deletions.isEmpty();
    }

    private static void trimOldest(List<ResearchTransaction> transactions) {
        while (transactions.size() > MAX_TRANSACTIONS) transactions.remove(0);
    }

    private static void trimDeletions(List<DeletionRecord> deletions) {
        while (deletions.size() > MAX_DELETION_RECORDS) {
            int remove = -1;
            for (int i = 0; i < deletions.size(); i++) {
                if (!deletions.get(i).active()) {
                    remove = i;
                    break;
                }
            }
            deletions.remove(remove >= 0 ? remove : 0);
        }
    }

    private static NBTTagList writeTransactions(List<ResearchTransaction> transactions) {
        NBTTagList list = new NBTTagList();
        for (ResearchTransaction transaction : transactions) list.appendTag(writeTransaction(transaction));
        return list;
    }

    private static void readTransactions(NBTTagList list, List<ResearchTransaction> out) {
        for (int i = 0; i < list.tagCount(); i++) {
            ResearchTransaction transaction = readTransaction(list.getCompoundTagAt(i));
            if (transaction != null) out.add(transaction);
        }
    }

    private static NBTTagCompound writeTransaction(ResearchTransaction transaction) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("Id", transaction.id());
        tag.setLong("Timestamp", transaction.timestamp());
        tag.setString("Description", transaction.description());
        tag.setTag("Added", writeEntries(transaction.added()));
        tag.setTag("Removed", writeEntries(transaction.removed()));
        NBTTagList deletionChanges = new NBTTagList();
        for (DeletionStateChange change : transaction.deletionChanges()) {
            NBTTagCompound changeTag = new NBTTagCompound();
            changeTag.setLong("DeletionId", change.deletionId());
            changeTag.setBoolean("ActiveAfterForward", change.activeAfterForward());
            deletionChanges.appendTag(changeTag);
        }
        tag.setTag("DeletionChanges", deletionChanges);
        return tag;
    }

    private static ResearchTransaction readTransaction(NBTTagCompound tag) {
        if (tag == null) return null;
        List<DeletionStateChange> changes = new ArrayList<DeletionStateChange>();
        NBTTagList changeTags = tag.getTagList("DeletionChanges", 10);
        for (int i = 0; i < changeTags.tagCount(); i++) {
            NBTTagCompound change = changeTags.getCompoundTagAt(i);
            changes.add(new DeletionStateChange(change.getLong("DeletionId"), change.getBoolean("ActiveAfterForward")));
        }
        return new ResearchTransaction(
            tag.getLong("Id"),
            tag.getLong("Timestamp"),
            tag.getString("Description"),
            readEntries(tag.getTagList("Added", 10)),
            readEntries(tag.getTagList("Removed", 10)),
            changes);
    }

    private static NBTTagList writeDeletions(List<DeletionRecord> deletions) {
        NBTTagList list = new NBTTagList();
        for (DeletionRecord record : deletions) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setLong("Id", record.id());
            tag.setLong("Timestamp", record.timestamp());
            tag.setBoolean("Active", record.active());
            tag.setTag("Entry", writeEntry(record.entry()));
            list.appendTag(tag);
        }
        return list;
    }

    private static void readDeletions(NBTTagList list, List<DeletionRecord> out) {
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            ResearchEntrySnapshot entry = readEntry(tag.getCompoundTag("Entry"));
            if (entry == null) continue;
            out.add(new DeletionRecord(tag.getLong("Id"), tag.getLong("Timestamp"), entry, tag.getBoolean("Active")));
        }
    }

    private static NBTTagList writeEntries(List<ResearchEntrySnapshot> entries) {
        NBTTagList list = new NBTTagList();
        for (ResearchEntrySnapshot entry : entries) list.appendTag(writeEntry(entry));
        return list;
    }

    private static NBTTagCompound writeEntry(ResearchEntrySnapshot entry) {
        NBTTagCompound tag = new NBTTagCompound();
        ResearchKey key = entry.key();
        tag.setString("ItemId", key.getItemId());
        tag.setInteger("Meta", key.getMeta());
        tag.setString("CanonicalNbt", key.getCanonicalNbt());
        tag.setInteger("TimelineIndex", entry.timelineIndex());
        NBTTagCompound template = entry.template();
        if (template != null) tag.setTag("Tag", template);
        return tag;
    }

    private static List<ResearchEntrySnapshot> readEntries(NBTTagList list) {
        PersistedResearchHistoryResolver.ListResult resolved = PersistedResearchHistoryResolver.resolveEntries(list, false);
        return resolved.entries();
    }

    private static ResearchEntrySnapshot readEntry(NBTTagCompound tag) {
        return PersistedResearchHistoryResolver.resolveEntry(tag).entry();
    }

    private static final class PlayerHistory {

        private final List<ResearchTransaction> undo = new ArrayList<ResearchTransaction>();
        private final List<ResearchTransaction> redo = new ArrayList<ResearchTransaction>();
        private final List<DeletionRecord> deletions = new ArrayList<DeletionRecord>();
    }
}
