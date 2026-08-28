package dev.gtnhjourney.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import dev.gtnhjourney.recovery.JourneySnapshot;
import dev.gtnhjourney.recovery.ResearchEntrySnapshot;
import dev.gtnhjourney.recovery.ResearchStateSnapshot;
import dev.gtnhjourney.recovery.SnapshotKind;
import dev.gtnhjourney.research.ResearchKey;

/** Bounded snapshot archive independent from primary Journey research and undo/redo storage. */
public final class JourneySnapshotData extends WorldSavedData {

    public static final String DATA_NAME = "gtnhjourney_snapshots";
    private static final int DATA_VERSION = 1;
    private static final int MAX_ROTATING = 20;
    private static final int MAX_MANUAL = 10;

    private final Map<UUID, PlayerSnapshots> snapshots = new LinkedHashMap<UUID, PlayerSnapshots>();

    public JourneySnapshotData() {
        super(DATA_NAME);
    }

    public JourneySnapshotData(String name) {
        super(name);
    }

    public static JourneySnapshotData get(World world) {
        if (world == null) throw new IllegalArgumentException("world must not be null");
        MapStorage storage = world.mapStorage;
        JourneySnapshotData data = (JourneySnapshotData) storage.loadData(JourneySnapshotData.class, DATA_NAME);
        if (data == null) {
            data = new JourneySnapshotData(DATA_NAME);
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public void add(UUID playerId, JourneySnapshot snapshot) {
        if (playerId == null || snapshot == null) return;
        PlayerSnapshots player = player(playerId);
        List<JourneySnapshot> target = snapshot.kind() == SnapshotKind.MANUAL ? player.manual : player.rotating;
        target.add(copy(snapshot));
        trim(target, snapshot.kind() == SnapshotKind.MANUAL ? MAX_MANUAL : MAX_ROTATING);
        markDirty();
    }

    public List<JourneySnapshot> rotatingSnapshots(UUID playerId) {
        PlayerSnapshots player = snapshots.get(playerId);
        return copyList(player == null ? null : player.rotating);
    }

    public List<JourneySnapshot> manualSnapshots(UUID playerId) {
        PlayerSnapshots player = snapshots.get(playerId);
        return copyList(player == null ? null : player.manual);
    }

    public JourneySnapshot latestAuto(UUID playerId) {
        PlayerSnapshots player = snapshots.get(playerId);
        if (player == null) return null;
        for (int i = player.rotating.size() - 1; i >= 0; i--) {
            JourneySnapshot snapshot = player.rotating.get(i);
            if (snapshot.kind() == SnapshotKind.AUTO) return copy(snapshot);
        }
        return null;
    }

    public JourneySnapshot latestRotating(UUID playerId) {
        PlayerSnapshots player = snapshots.get(playerId);
        if (player == null || player.rotating.isEmpty()) return null;
        return copy(player.rotating.get(player.rotating.size() - 1));
    }

    public JourneySnapshot find(UUID playerId, String idOrName) {
        if (idOrName == null || idOrName.isEmpty()) return null;
        PlayerSnapshots player = snapshots.get(playerId);
        if (player == null) return null;
        JourneySnapshot match = findIn(player.manual, idOrName);
        if (match == null) match = findIn(player.rotating, idOrName);
        return match == null ? null : copy(match);
    }

    @Override
    public void readFromNBT(NBTTagCompound root) {
        snapshots.clear();
        boolean migrated = root.getInteger("Version") != DATA_VERSION;
        NBTTagList players = root.getTagList("Players", 10);
        for (int i = 0; i < players.tagCount(); i++) {
            NBTTagCompound playerTag = players.getCompoundTagAt(i);
            UUID playerId = new UUID(playerTag.getLong("UuidMost"), playerTag.getLong("UuidLeast"));
            PlayerSnapshots player = new PlayerSnapshots();
            migrated |= readSnapshots(playerTag.getTagList("Rotating", 10), player.rotating);
            migrated |= readSnapshots(playerTag.getTagList("Manual", 10), player.manual);
            trim(player.rotating, MAX_ROTATING);
            trim(player.manual, MAX_MANUAL);
            if (!player.rotating.isEmpty() || !player.manual.isEmpty()) snapshots.put(playerId, player);
        }
        if (migrated) markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound root) {
        root.setInteger("Version", DATA_VERSION);
        NBTTagList players = new NBTTagList();
        for (Map.Entry<UUID, PlayerSnapshots> mapEntry : snapshots.entrySet()) {
            PlayerSnapshots player = mapEntry.getValue();
            if (player == null || (player.rotating.isEmpty() && player.manual.isEmpty())) continue;
            NBTTagCompound playerTag = new NBTTagCompound();
            UUID playerId = mapEntry.getKey();
            playerTag.setLong("UuidMost", playerId.getMostSignificantBits());
            playerTag.setLong("UuidLeast", playerId.getLeastSignificantBits());
            playerTag.setTag("Rotating", writeSnapshots(player.rotating));
            playerTag.setTag("Manual", writeSnapshots(player.manual));
            players.appendTag(playerTag);
        }
        root.setTag("Players", players);
    }

    private PlayerSnapshots player(UUID playerId) {
        PlayerSnapshots player = snapshots.get(playerId);
        if (player == null) {
            player = new PlayerSnapshots();
            snapshots.put(playerId, player);
        }
        return player;
    }

    private static JourneySnapshot findIn(List<JourneySnapshot> list, String idOrName) {
        for (int i = list.size() - 1; i >= 0; i--) {
            JourneySnapshot snapshot = list.get(i);
            if (idOrName.equals(snapshot.name()) || idOrName.equals(Long.toString(snapshot.id()))) return snapshot;
        }
        return null;
    }

    private static List<JourneySnapshot> copyList(List<JourneySnapshot> source) {
        List<JourneySnapshot> copy = new ArrayList<JourneySnapshot>();
        if (source != null) for (JourneySnapshot snapshot : source) copy.add(copy(snapshot));
        return Collections.unmodifiableList(copy);
    }

    private static JourneySnapshot copy(JourneySnapshot snapshot) {
        return new JourneySnapshot(snapshot.id(), snapshot.name(), snapshot.worldTick(), snapshot.kind(), snapshot.state());
    }

    private static void trim(List<JourneySnapshot> list, int max) {
        while (list.size() > max) list.remove(0);
    }

    private static NBTTagList writeSnapshots(List<JourneySnapshot> source) {
        NBTTagList list = new NBTTagList();
        for (JourneySnapshot snapshot : source) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setLong("Id", snapshot.id());
            tag.setString("Name", snapshot.name());
            tag.setLong("WorldTick", snapshot.worldTick());
            tag.setString("Kind", snapshot.kind().name());
            NBTTagList entries = new NBTTagList();
            for (ResearchEntrySnapshot entry : snapshot.state().entries()) entries.appendTag(writeEntry(entry));
            tag.setTag("Entries", entries);
            list.appendTag(tag);
        }
        return list;
    }

    private static boolean readSnapshots(NBTTagList list, List<JourneySnapshot> out) {
        boolean migrated = false;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            SnapshotKind kind;
            try {
                kind = SnapshotKind.valueOf(tag.getString("Kind"));
            } catch (IllegalArgumentException ignored) {
                migrated = true;
                continue;
            }
            PersistedResearchHistoryResolver.ListResult resolved = PersistedResearchHistoryResolver
                .resolveEntries(tag.getTagList("Entries", 10), true);
            migrated |= resolved.changed();
            out.add(
                new JourneySnapshot(
                    tag.getLong("Id"),
                    tag.getString("Name"),
                    tag.getLong("WorldTick"),
                    kind,
                    new ResearchStateSnapshot(resolved.entries())));
        }
        return migrated;
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

    private static final class PlayerSnapshots {

        private final List<JourneySnapshot> rotating = new ArrayList<JourneySnapshot>();
        private final List<JourneySnapshot> manual = new ArrayList<JourneySnapshot>();
    }
}
