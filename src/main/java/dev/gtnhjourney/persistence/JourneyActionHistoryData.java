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

import dev.gtnhjourney.recovery.JourneyActionKind;
import dev.gtnhjourney.recovery.JourneyActionTransaction;

/** Persistent bounded undo/redo journal for non-research Journey actions. */
public final class JourneyActionHistoryData extends WorldSavedData {

    public static final String DATA_NAME = "gtnhjourney_action_history";
    private static final int DATA_VERSION = 1;
    private static final int MAX_TRANSACTIONS = 100;
    private final Map<UUID, PlayerHistory> histories = new LinkedHashMap<UUID, PlayerHistory>();

    public JourneyActionHistoryData() { super(DATA_NAME); }
    public JourneyActionHistoryData(String name) { super(name); }

    public static JourneyActionHistoryData get(World world) {
        if (world == null) throw new IllegalArgumentException("world must not be null");
        MapStorage storage = world.mapStorage;
        JourneyActionHistoryData data = (JourneyActionHistoryData) storage.loadData(JourneyActionHistoryData.class, DATA_NAME);
        if (data == null) {
            data = new JourneyActionHistoryData(DATA_NAME);
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public void record(UUID playerId, JourneyActionTransaction transaction) {
        if (playerId == null || transaction == null) return;
        PlayerHistory history = history(playerId);
        history.redo.clear();
        history.undo.add(transaction);
        trim(history.undo);
        markDirty();
    }

    public JourneyActionTransaction peekUndo(UUID playerId) { return peek(playerId, true, null); }
    public JourneyActionTransaction peekRedo(UUID playerId) { return peek(playerId, false, null); }
    public JourneyActionTransaction peekUndo(UUID playerId, JourneyActionKind kind) { return peek(playerId, true, kind); }
    public JourneyActionTransaction peekRedo(UUID playerId, JourneyActionKind kind) { return peek(playerId, false, kind); }

    public JourneyActionTransaction popUndo(UUID playerId, JourneyActionKind kind) { return pop(playerId, true, kind); }
    public JourneyActionTransaction popRedo(UUID playerId, JourneyActionKind kind) { return pop(playerId, false, kind); }

    public void pushUndo(UUID playerId, JourneyActionTransaction transaction) {
        push(playerId, true, transaction);
    }

    public void pushRedo(UUID playerId, JourneyActionTransaction transaction) {
        push(playerId, false, transaction);
    }

    public void clearRedo(UUID playerId) {
        PlayerHistory history = histories.get(playerId);
        if (history == null || history.redo.isEmpty()) return;
        history.redo.clear();
        prune(playerId, history);
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

    private JourneyActionTransaction peek(UUID playerId, boolean undo, JourneyActionKind kind) {
        PlayerHistory history = histories.get(playerId);
        if (history == null) return null;
        List<JourneyActionTransaction> list = undo ? history.undo : history.redo;
        for (int i = list.size() - 1; i >= 0; i--) {
            JourneyActionTransaction transaction = list.get(i);
            if (kind == null || transaction.kind() == kind) return transaction;
        }
        return null;
    }

    private JourneyActionTransaction pop(UUID playerId, boolean undo, JourneyActionKind kind) {
        PlayerHistory history = histories.get(playerId);
        if (history == null) return null;
        List<JourneyActionTransaction> list = undo ? history.undo : history.redo;
        for (int i = list.size() - 1; i >= 0; i--) {
            JourneyActionTransaction transaction = list.get(i);
            if (kind != null && transaction.kind() != kind) continue;
            list.remove(i);
            prune(playerId, history);
            markDirty();
            return transaction;
        }
        return null;
    }

    private void push(UUID playerId, boolean undo, JourneyActionTransaction transaction) {
        if (playerId == null || transaction == null) return;
        PlayerHistory history = history(playerId);
        List<JourneyActionTransaction> list = undo ? history.undo : history.redo;
        list.add(transaction);
        trim(list);
        markDirty();
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
            trim(history.undo);
            trim(history.redo);
            if (!history.undo.isEmpty() || !history.redo.isEmpty()) histories.put(playerId, history);
        }
        if (root.getInteger("Version") != DATA_VERSION) markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound root) {
        root.setInteger("Version", DATA_VERSION);
        NBTTagList players = new NBTTagList();
        for (Map.Entry<UUID, PlayerHistory> entry : histories.entrySet()) {
            PlayerHistory history = entry.getValue();
            if (history.undo.isEmpty() && history.redo.isEmpty()) continue;
            NBTTagCompound playerTag = new NBTTagCompound();
            playerTag.setLong("UuidMost", entry.getKey().getMostSignificantBits());
            playerTag.setLong("UuidLeast", entry.getKey().getLeastSignificantBits());
            playerTag.setTag("Undo", writeTransactions(history.undo));
            playerTag.setTag("Redo", writeTransactions(history.redo));
            players.appendTag(playerTag);
        }
        root.setTag("Players", players);
    }

    private static NBTTagList writeTransactions(List<JourneyActionTransaction> transactions) {
        NBTTagList list = new NBTTagList();
        for (JourneyActionTransaction transaction : transactions) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setLong("Id", transaction.id());
            tag.setLong("Timestamp", transaction.timestamp());
            tag.setString("Kind", transaction.kind().name());
            tag.setString("Description", transaction.description());
            tag.setTag("Before", transaction.before());
            tag.setTag("After", transaction.after());
            list.appendTag(tag);
        }
        return list;
    }

    private static void readTransactions(NBTTagList list, List<JourneyActionTransaction> out) {
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            JourneyActionKind kind = JourneyActionKind.parse(tag.getString("Kind"));
            if (kind == null) continue;
            out.add(new JourneyActionTransaction(
                tag.getLong("Id"),
                tag.getLong("Timestamp"),
                kind,
                tag.getString("Description"),
                tag.getCompoundTag("Before"),
                tag.getCompoundTag("After")));
        }
    }

    private PlayerHistory history(UUID playerId) {
        PlayerHistory history = histories.get(playerId);
        if (history == null) {
            history = new PlayerHistory();
            histories.put(playerId, history);
        }
        return history;
    }

    private void prune(UUID playerId, PlayerHistory history) {
        if (history.undo.isEmpty() && history.redo.isEmpty()) histories.remove(playerId);
    }

    private static void trim(List<JourneyActionTransaction> values) {
        while (values.size() > MAX_TRANSACTIONS) values.remove(0);
    }

    private static final class PlayerHistory {
        final List<JourneyActionTransaction> undo = new ArrayList<JourneyActionTransaction>();
        final List<JourneyActionTransaction> redo = new ArrayList<JourneyActionTransaction>();
    }
}
