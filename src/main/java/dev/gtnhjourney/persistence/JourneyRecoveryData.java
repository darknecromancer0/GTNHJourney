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

import dev.gtnhjourney.recovery.ResearchEntrySnapshot;
import dev.gtnhjourney.recovery.ResearchTransaction;
import dev.gtnhjourney.research.ResearchKey;

/** Persistent bounded undo/redo journals, separate from authoritative research data. */
public final class JourneyRecoveryData extends WorldSavedData {

    public static final String DATA_NAME = "gtnhjourney_recovery";
    private static final int DATA_VERSION = 1;
    private static final int MAX_TRANSACTIONS = 100;

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
            trimOldest(history.undo);
            trimOldest(history.redo);
            if (!history.undo.isEmpty() || !history.redo.isEmpty()) histories.put(playerId, history);
        }
        if (root.getInteger("Version") != DATA_VERSION) markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound root) {
        root.setInteger("Version", DATA_VERSION);
        NBTTagList players = new NBTTagList();
        for (Map.Entry<UUID, PlayerHistory> mapEntry : histories.entrySet()) {
            PlayerHistory history = mapEntry.getValue();
            if (history == null || (history.undo.isEmpty() && history.redo.isEmpty())) continue;
            UUID playerId = mapEntry.getKey();
            NBTTagCompound playerTag = new NBTTagCompound();
            playerTag.setLong("UuidMost", playerId.getMostSignificantBits());
            playerTag.setLong("UuidLeast", playerId.getLeastSignificantBits());
            playerTag.setTag("Undo", writeTransactions(history.undo));
            playerTag.setTag("Redo", writeTransactions(history.redo));
            players.appendTag(playerTag);
        }
        root.setTag("Players", players);
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
        if (history.undo.isEmpty() && history.redo.isEmpty()) histories.remove(playerId);
    }

    private static void trimOldest(List<ResearchTransaction> transactions) {
        while (transactions.size() > MAX_TRANSACTIONS) transactions.remove(0);
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
        return tag;
    }

    private static ResearchTransaction readTransaction(NBTTagCompound tag) {
        if (tag == null) return null;
        return new ResearchTransaction(
            tag.getLong("Id"),
            tag.getLong("Timestamp"),
            tag.getString("Description"),
            readEntries(tag.getTagList("Added", 10)),
            readEntries(tag.getTagList("Removed", 10)));
    }

    private static NBTTagList writeEntries(List<ResearchEntrySnapshot> entries) {
        NBTTagList list = new NBTTagList();
        for (ResearchEntrySnapshot entry : entries) {
            NBTTagCompound tag = new NBTTagCompound();
            ResearchKey key = entry.key();
            tag.setString("ItemId", key.getItemId());
            tag.setInteger("Meta", key.getMeta());
            tag.setString("CanonicalNbt", key.getCanonicalNbt());
            tag.setInteger("TimelineIndex", entry.timelineIndex());
            NBTTagCompound template = entry.template();
            if (template != null) tag.setTag("Tag", template);
            list.appendTag(tag);
        }
        return list;
    }

    private static List<ResearchEntrySnapshot> readEntries(NBTTagList list) {
        List<ResearchEntrySnapshot> entries = new ArrayList<ResearchEntrySnapshot>();
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            String itemId = tag.getString("ItemId");
            if (itemId == null || itemId.isEmpty()) continue;
            ResearchKey key = new ResearchKey(itemId, tag.getInteger("Meta"), tag.getString("CanonicalNbt"));
            NBTTagCompound template = tag.hasKey("Tag", 10) ? tag.getCompoundTag("Tag") : null;
            entries.add(new ResearchEntrySnapshot(key, template, Math.max(0, tag.getInteger("TimelineIndex"))));
        }
        return entries;
    }

    private static final class PlayerHistory {

        private final List<ResearchTransaction> undo = new ArrayList<ResearchTransaction>();
        private final List<ResearchTransaction> redo = new ArrayList<ResearchTransaction>();
    }
}
