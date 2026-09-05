package dev.gtnhjourney.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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

import dev.gtnhjourney.research.ResearchActivityTimeline;
import dev.gtnhjourney.research.ResearchKey;

/** Persistent per-player Journey activity plus an independent successful-issuance chronology. */
public final class JourneyActivityData extends WorldSavedData {

    public static final String DATA_NAME = "gtnhjourney_activity";
    private static final int DATA_VERSION = 2;
    private final Map<UUID, ResearchActivityTimeline> timelines = new LinkedHashMap<UUID, ResearchActivityTimeline>();
    private final Map<UUID, ResearchActivityTimeline> issuedTimelines = new LinkedHashMap<UUID, ResearchActivityTimeline>();

    public JourneyActivityData() { super(DATA_NAME); }
    public JourneyActivityData(String name) { super(name); }

    public static JourneyActivityData get(World world) {
        if (world == null) throw new IllegalArgumentException("world must not be null");
        MapStorage storage = world.mapStorage;
        JourneyActivityData data = (JourneyActivityData) storage.loadData(JourneyActivityData.class, DATA_NAME);
        if (data == null) {
            data = new JourneyActivityData(DATA_NAME);
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public boolean recordUnlock(UUID playerId, ResearchKey key) {
        if (playerId == null || key == null) return false;
        boolean changed = timeline(playerId).recordUnlock(key);
        if (changed) markDirty();
        return changed;
    }

    /** A successful Journey retrieval updates both legacy combined activity and the truthful issued-only timeline. */
    public boolean recordRetrieval(UUID playerId, ResearchKey key) {
        if (playerId == null || key == null) return false;
        boolean activityChanged = timeline(playerId).recordRetrieval(key);
        boolean issuedChanged = issuedTimeline(playerId).recordRetrieval(key);
        if (activityChanged || issuedChanged) markDirty();
        return activityChanged || issuedChanged;
    }

    /** Records successful issuance without implying research activity, used by C/native issuance. */
    public boolean recordIssued(UUID playerId, ResearchKey key) {
        if (playerId == null || key == null) return false;
        boolean changed = issuedTimeline(playerId).recordRetrieval(key);
        if (changed) markDirty();
        return changed;
    }

    /**
     * Returns oldest-first legacy activity order reconciled against the current research set. Missing research is kept,
     * but is treated as older than known activity so migration/recovery cannot falsely mark an item as recently touched.
     */
    public List<ResearchKey> snapshotReconciled(UUID playerId, Collection<ResearchKey> researchOldestFirst) {
        if (playerId == null) return Collections.emptyList();
        List<ResearchKey> research = researchOldestFirst == null ? Collections.<ResearchKey>emptyList()
            : new ArrayList<ResearchKey>(researchOldestFirst);
        Set<ResearchKey> researched = new HashSet<ResearchKey>(research);
        List<ResearchKey> current = timeline(playerId).snapshotOldestFirst();
        LinkedHashSet<ResearchKey> known = new LinkedHashSet<ResearchKey>();
        for (ResearchKey key : current) if (researched.contains(key)) known.add(key);

        LinkedHashSet<ResearchKey> next = new LinkedHashSet<ResearchKey>();
        for (ResearchKey key : research) if (key != null && !known.contains(key)) next.add(key);
        next.addAll(known);

        List<ResearchKey> reconciled = new ArrayList<ResearchKey>(next);
        if (!reconciled.equals(current)) {
            timeline(playerId).restore(reconciled);
            markDirty();
        }
        return Collections.unmodifiableList(reconciled);
    }

    /** Oldest-first successful issuance only. Legacy v1 saves intentionally return empty rather than inventing history. */
    public List<ResearchKey> snapshotIssuedOldestFirst(UUID playerId) {
        if (playerId == null) return Collections.emptyList();
        return issuedTimeline(playerId).snapshotOldestFirst();
    }

    @Override
    public void readFromNBT(NBTTagCompound root) {
        timelines.clear();
        issuedTimelines.clear();
        NBTTagList players = root.getTagList("Players", 10);
        for (int i = 0; i < players.tagCount(); i++) {
            NBTTagCompound playerTag = players.getCompoundTagAt(i);
            UUID playerId = new UUID(playerTag.getLong("UuidMost"), playerTag.getLong("UuidLeast"));
            List<ResearchKey> activity = readKeys(playerTag.getTagList("Entries", 10));
            if (!activity.isEmpty()) timeline(playerId).restore(activity);
            List<ResearchKey> issued = readKeys(playerTag.getTagList("IssuedEntries", 10));
            if (!issued.isEmpty()) issuedTimeline(playerId).restore(issued);
        }
        if (root.getInteger("Version") != DATA_VERSION) markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound root) {
        root.setInteger("Version", DATA_VERSION);
        NBTTagList players = new NBTTagList();
        Set<UUID> allIds = new HashSet<UUID>();
        allIds.addAll(timelines.keySet());
        allIds.addAll(issuedTimelines.keySet());
        List<UUID> ids = new ArrayList<UUID>(allIds);
        Collections.sort(ids);
        for (UUID playerId : ids) {
            List<ResearchKey> activity = existingSnapshot(timelines, playerId);
            List<ResearchKey> issued = existingSnapshot(issuedTimelines, playerId);
            if (activity.isEmpty() && issued.isEmpty()) continue;
            NBTTagCompound playerTag = new NBTTagCompound();
            playerTag.setLong("UuidMost", playerId.getMostSignificantBits());
            playerTag.setLong("UuidLeast", playerId.getLeastSignificantBits());
            playerTag.setTag("Entries", writeKeys(activity));
            playerTag.setTag("IssuedEntries", writeKeys(issued));
            players.appendTag(playerTag);
        }
        root.setTag("Players", players);
    }

    private static List<ResearchKey> readKeys(NBTTagList entries) {
        List<ResearchKey> ordered = new ArrayList<ResearchKey>();
        if (entries == null) return ordered;
        for (int j = 0; j < entries.tagCount(); j++) {
            NBTTagCompound entry = entries.getCompoundTagAt(j);
            String itemId = entry.getString("ItemId");
            if (itemId == null || itemId.trim().isEmpty()) continue;
            try {
                ordered.add(new ResearchKey(itemId, entry.getInteger("Meta"), entry.getString("CanonicalNbt")));
            } catch (IllegalArgumentException ignored) {}
        }
        return ordered;
    }

    private static NBTTagList writeKeys(List<ResearchKey> ordered) {
        NBTTagList entries = new NBTTagList();
        if (ordered == null) return entries;
        for (ResearchKey key : ordered) {
            if (key == null) continue;
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("ItemId", key.getItemId());
            entry.setInteger("Meta", key.getMeta());
            entry.setString("CanonicalNbt", key.getCanonicalNbt());
            entries.appendTag(entry);
        }
        return entries;
    }

    private static List<ResearchKey> existingSnapshot(
        Map<UUID, ResearchActivityTimeline> source,
        UUID playerId) {
        ResearchActivityTimeline timeline = source.get(playerId);
        return timeline == null ? Collections.<ResearchKey>emptyList() : timeline.snapshotOldestFirst();
    }

    private ResearchActivityTimeline timeline(UUID playerId) {
        ResearchActivityTimeline timeline = timelines.get(playerId);
        if (timeline == null) {
            timeline = new ResearchActivityTimeline();
            timelines.put(playerId, timeline);
        }
        return timeline;
    }

    private ResearchActivityTimeline issuedTimeline(UUID playerId) {
        ResearchActivityTimeline timeline = issuedTimelines.get(playerId);
        if (timeline == null) {
            timeline = new ResearchActivityTimeline();
            issuedTimelines.put(playerId, timeline);
        }
        return timeline;
    }
}
