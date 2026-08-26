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

/** Persistent per-player ordering used only by the N Journey view. */
public final class JourneyActivityData extends WorldSavedData {

    public static final String DATA_NAME = "gtnhjourney_activity";
    private static final int DATA_VERSION = 1;
    private final Map<UUID, ResearchActivityTimeline> timelines = new LinkedHashMap<UUID, ResearchActivityTimeline>();

    public JourneyActivityData() {
        super(DATA_NAME);
    }

    public JourneyActivityData(String name) {
        super(name);
    }

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

    public boolean recordRetrieval(UUID playerId, ResearchKey key) {
        if (playerId == null || key == null) return false;
        boolean changed = timeline(playerId).recordRetrieval(key);
        if (changed) markDirty();
        return changed;
    }

    /**
     * Returns oldest-first activity order reconciled against the current research set. Missing research is kept, but is
     * treated as older than known activity so migration/recovery cannot falsely mark an item as recently touched.
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

    @Override
    public void readFromNBT(NBTTagCompound root) {
        timelines.clear();
        NBTTagList players = root.getTagList("Players", 10);
        for (int i = 0; i < players.tagCount(); i++) {
            NBTTagCompound playerTag = players.getCompoundTagAt(i);
            UUID playerId = new UUID(playerTag.getLong("UuidMost"), playerTag.getLong("UuidLeast"));
            NBTTagList entries = playerTag.getTagList("Entries", 10);
            List<ResearchKey> ordered = new ArrayList<ResearchKey>();
            for (int j = 0; j < entries.tagCount(); j++) {
                NBTTagCompound entry = entries.getCompoundTagAt(j);
                String itemId = entry.getString("ItemId");
                if (itemId == null || itemId.trim().isEmpty()) continue;
                try {
                    ordered.add(new ResearchKey(itemId, entry.getInteger("Meta"), entry.getString("CanonicalNbt")));
                } catch (IllegalArgumentException ignored) {}
            }
            if (!ordered.isEmpty()) timeline(playerId).restore(ordered);
        }
        if (root.getInteger("Version") != DATA_VERSION) markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound root) {
        root.setInteger("Version", DATA_VERSION);
        NBTTagList players = new NBTTagList();
        List<UUID> ids = new ArrayList<UUID>(timelines.keySet());
        Collections.sort(ids);
        for (UUID playerId : ids) {
            List<ResearchKey> ordered = timeline(playerId).snapshotOldestFirst();
            if (ordered.isEmpty()) continue;
            NBTTagCompound playerTag = new NBTTagCompound();
            playerTag.setLong("UuidMost", playerId.getMostSignificantBits());
            playerTag.setLong("UuidLeast", playerId.getLeastSignificantBits());
            NBTTagList entries = new NBTTagList();
            for (ResearchKey key : ordered) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("ItemId", key.getItemId());
                entry.setInteger("Meta", key.getMeta());
                entry.setString("CanonicalNbt", key.getCanonicalNbt());
                entries.appendTag(entry);
            }
            playerTag.setTag("Entries", entries);
            players.appendTag(playerTag);
        }
        root.setTag("Players", players);
    }

    private ResearchActivityTimeline timeline(UUID playerId) {
        ResearchActivityTimeline timeline = timelines.get(playerId);
        if (timeline == null) {
            timeline = new ResearchActivityTimeline();
            timelines.put(playerId, timeline);
        }
        return timeline;
    }
}
