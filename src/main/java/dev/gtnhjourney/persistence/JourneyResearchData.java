package dev.gtnhjourney.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.storage.MapStorage;

import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.minecraft.NbtCanonicalizer;
import dev.gtnhjourney.minecraft.PersistedResearchEntryResolver;
import dev.gtnhjourney.minecraft.ResearchStateExpander;
import dev.gtnhjourney.minecraft.ResearchTemplateNormalizer;
import dev.gtnhjourney.research.ResearchKey;
import dev.gtnhjourney.research.ResearchRegistry;
import dev.gtnhjourney.research.ResearchTimeline;

/** Server-side world persistence for all players' Journey research. */
public final class JourneyResearchData extends WorldSavedData {

    public static final String DATA_NAME = "gtnhjourney_research";
    private static final int DATA_VERSION = 7;

    private final PlayerResearchStore research = new PlayerResearchStore();
    private final Map<UUID, Map<ResearchKey, NBTTagCompound>> templates = new LinkedHashMap<UUID, Map<ResearchKey, NBTTagCompound>>();
    private final Map<UUID, ResearchTimeline> timelines = new LinkedHashMap<UUID, ResearchTimeline>();
    private final Map<UUID, PlayerResearchBackup> undoBackups = new LinkedHashMap<UUID, PlayerResearchBackup>();

    public JourneyResearchData() {
        super(DATA_NAME);
    }

    public JourneyResearchData(String name) {
        super(name);
    }

    public static JourneyResearchData get(World world) {
        if (world == null) throw new IllegalArgumentException("world must not be null");
        MapStorage storage = world.mapStorage;
        JourneyResearchData data = (JourneyResearchData) storage.loadData(JourneyResearchData.class, DATA_NAME);
        if (data == null) {
            data = new JourneyResearchData(DATA_NAME);
            storage.setData(DATA_NAME, data);
        }
        return data;
    }

    public ResearchRegistry registry(UUID playerId) {
        return research.forPlayer(playerId);
    }

    public boolean unlock(UUID playerId, ItemStack stack) {
        return !unlockStates(playerId, stack).isEmpty();
    }

    /**
     * Unlocks every semantic endpoint proven by one observed stack. A verified FULL GT electric item, for example,
     * proves both its base/empty endpoint and its FULL endpoint while partial charge proves only the base endpoint.
     */
    public List<ItemStack> unlockStates(UUID playerId, ItemStack stack) {
        if (playerId == null || stack == null || stack.getItem() == null) return Collections.emptyList();
        ResearchRegistry registry = research.forPlayer(playerId);
        List<ItemStack> addedStacks = new ArrayList<ItemStack>();
        for (ItemStack candidate : ResearchStateExpander.expand(stack)) {
            if (candidate == null || candidate.getItem() == null) continue;
            ResearchKey key;
            try {
                key = ItemStackKeyFactory.from(candidate);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            if (!registry.unlock(key)) continue;
            NBTTagCompound savedTag = copyTag(candidate);
            templateMap(playerId).put(key, savedTag);
            timeline(playerId).record(key);
            ItemStack clientTemplate = dev.gtnhjourney.retrieval.ItemStackTemplateFactory.create(key, savedTag, 1);
            if (clientTemplate == null) {
                clientTemplate = candidate.copy();
                clientTemplate.stackSize = 1;
            }
            addedStacks.add(clientTemplate);
        }
        if (!addedStacks.isEmpty()) markDirty();
        return Collections.unmodifiableList(addedStacks);
    }

    public NBTTagCompound template(UUID playerId, ResearchKey key) {
        Map<ResearchKey, NBTTagCompound> playerTemplates = templates.get(playerId);
        if (playerTemplates == null) return null;
        NBTTagCompound tag = playerTemplates.get(key);
        return tag == null ? null : (NBTTagCompound) tag.copy();
    }

    public List<ResearchKey> snapshot(UUID playerId) {
        return research.forPlayer(playerId)
            .snapshot();
    }

    public List<ItemStack> snapshotStacks(UUID playerId) {
        return stacksForKeys(
            playerId,
            research.forPlayer(playerId)
                .snapshot());
    }

    public List<ResearchKey> snapshotNewest(UUID playerId, int limit) {
        return timeline(playerId).snapshotNewest(limit);
    }

    public List<ItemStack> snapshotStacksInUnlockOrder(UUID playerId) {
        return stacksForKeys(playerId, timeline(playerId).snapshotOldestFirst());
    }

    public List<ItemStack> snapshotNewestStacks(UUID playerId, int limit) {
        return stacksForKeys(playerId, timeline(playerId).snapshotNewest(limit));
    }

    public boolean forget(UUID playerId, ResearchKey key) {
        if (playerId == null || key == null) return false;
        ResearchRegistry registry = research.forPlayer(playerId);
        if (!registry.contains(key)) return false;
        saveUndoSnapshot(playerId);
        if (!registry.remove(key)) return false;
        Map<ResearchKey, NBTTagCompound> playerTemplates = templates.get(playerId);
        if (playerTemplates != null) {
            playerTemplates.remove(key);
            if (playerTemplates.isEmpty()) templates.remove(playerId);
        }
        ResearchTimeline playerTimeline = timelines.get(playerId);
        if (playerTimeline != null) {
            playerTimeline.remove(key);
            if (playerTimeline.size() == 0) timelines.remove(playerId);
        }
        markDirty();
        return true;
    }

    public int clear(UUID playerId) {
        ResearchRegistry registry = research.forPlayer(playerId);
        int previous = registry.size();
        if (previous > 0) saveUndoSnapshot(playerId);
        registry.clear();
        research.clearPlayer(playerId);
        templates.remove(playerId);
        timelines.remove(playerId);
        if (previous > 0) markDirty();
        return previous;
    }

    public int undo(UUID playerId) {
        if (playerId == null) return 0;
        PlayerResearchBackup backup = undoBackups.remove(playerId);
        if (backup == null) return 0;

        List<ResearchKey> keys = backup.keys();
        research.restore(playerId, keys);
        Map<ResearchKey, NBTTagCompound> restoredTemplates = backup.templateCopies();
        if (restoredTemplates.isEmpty()) templates.remove(playerId);
        else templates.put(playerId, restoredTemplates);
        if (keys.isEmpty()) timelines.remove(playerId);
        else timeline(playerId).restore(keys);
        markDirty();
        return keys.size();
    }

    public int undoSize(UUID playerId) {
        PlayerResearchBackup backup = undoBackups.get(playerId);
        return backup == null ? 0 : backup.size();
    }

    public int pruneUnavailable(UUID playerId) {
        if (playerId == null) return 0;
        List<ResearchKey> keys = research.forPlayer(playerId)
            .snapshot();
        List<ResearchKey> unavailable = new ArrayList<ResearchKey>();
        for (ResearchKey key : keys) {
            ItemStack stack = dev.gtnhjourney.retrieval.ItemStackTemplateFactory
                .create(key, template(playerId, key), 1);
            if (stack == null) unavailable.add(key);
        }
        if (unavailable.isEmpty()) return 0;

        saveUndoSnapshot(playerId);
        ResearchRegistry registry = research.forPlayer(playerId);
        Map<ResearchKey, NBTTagCompound> playerTemplates = templates.get(playerId);
        ResearchTimeline playerTimeline = timelines.get(playerId);
        for (ResearchKey key : unavailable) {
            registry.remove(key);
            if (playerTemplates != null) playerTemplates.remove(key);
            if (playerTimeline != null) playerTimeline.remove(key);
        }
        if (playerTemplates != null && playerTemplates.isEmpty()) templates.remove(playerId);
        if (playerTimeline != null && playerTimeline.size() == 0) timelines.remove(playerId);
        markDirty();
        return unavailable.size();
    }

    @Override
    public void readFromNBT(NBTTagCompound root) {
        research.clearAll();
        templates.clear();
        timelines.clear();
        undoBackups.clear();
        boolean migrated = root.getInteger("Version") != DATA_VERSION;
        NBTTagList players = root.getTagList("Players", 10);
        for (int i = 0; i < players.tagCount(); i++) {
            NBTTagCompound playerTag = players.getCompoundTagAt(i);
            UUID playerId = new UUID(playerTag.getLong("UuidMost"), playerTag.getLong("UuidLeast"));
            NBTTagList entries = playerTag.getTagList("Entries", 10);
            List<ResearchKey> keys = new ArrayList<ResearchKey>();
            Map<ResearchKey, NBTTagCompound> playerTemplates = templateMap(playerId);

            for (int j = 0; j < entries.tagCount(); j++) {
                NBTTagCompound entry = entries.getCompoundTagAt(j);
                String itemId = entry.getString("ItemId");
                if (itemId == null || itemId.isEmpty()) continue;
                NBTTagCompound persistedTemplate = entry.hasKey("Tag", 10) ? entry.getCompoundTag("Tag") : null;
                int persistedMeta = entry.getInteger("Meta");
                String persistedCanonical = entry.getString("CanonicalNbt");
                PersistedResearchEntryResolver.ResolvedEntry resolved = PersistedResearchEntryResolver
                    .resolveEntry(itemId, persistedMeta, persistedCanonical, persistedTemplate);
                if (resolved == null) {
                    migrated = true;
                    continue;
                }
                ResearchKey key = resolved.key();
                NBTTagCompound template = resolved.template();
                if (key.getMeta() != persistedMeta || !key.getCanonicalNbt()
                    .equals(persistedCanonical) || !sameTemplate(persistedTemplate, template)) migrated = true;
                if (playerTemplates.containsKey(key)) {
                    migrated = true;
                    continue;
                }
                keys.add(key);
                playerTemplates.put(key, template);
            }
            research.restore(playerId, keys);
            timeline(playerId).restore(keys);
        }

        NBTTagList undoPlayers = root.getTagList("UndoPlayers", 10);
        for (int i = 0; i < undoPlayers.tagCount(); i++) {
            NBTTagCompound playerTag = undoPlayers.getCompoundTagAt(i);
            UUID playerId = new UUID(playerTag.getLong("UuidMost"), playerTag.getLong("UuidLeast"));
            NBTTagList entries = playerTag.getTagList("Entries", 10);
            List<ResearchKey> keys = new ArrayList<ResearchKey>();
            Map<ResearchKey, NBTTagCompound> backupTemplates = new LinkedHashMap<ResearchKey, NBTTagCompound>();
            for (int j = 0; j < entries.tagCount(); j++) {
                NBTTagCompound entry = entries.getCompoundTagAt(j);
                String itemId = entry.getString("ItemId");
                NBTTagCompound persistedTemplate = entry.hasKey("Tag", 10) ? entry.getCompoundTag("Tag") : null;
                int persistedMeta = entry.getInteger("Meta");
                String persistedCanonical = entry.getString("CanonicalNbt");
                PersistedResearchEntryResolver.ResolvedEntry resolved = PersistedResearchEntryResolver
                    .resolveEntry(itemId, persistedMeta, persistedCanonical, persistedTemplate);
                if (resolved == null) {
                    migrated = true;
                    continue;
                }
                ResearchKey key = resolved.key();
                NBTTagCompound template = resolved.template();
                if (key.getMeta() != persistedMeta || !key.getCanonicalNbt()
                    .equals(persistedCanonical) || !sameTemplate(persistedTemplate, template)) migrated = true;
                if (backupTemplates.containsKey(key)) {
                    migrated = true;
                    continue;
                }
                keys.add(key);
                backupTemplates.put(key, template);
            }
            if (!keys.isEmpty()) undoBackups.put(playerId, new PlayerResearchBackup(keys, backupTemplates));
        }
        if (migrated) markDirty();
    }

    @Override
    public void writeToNBT(NBTTagCompound root) {
        root.setInteger("Version", DATA_VERSION);
        NBTTagList players = new NBTTagList();

        for (UUID playerId : allPlayerIds()) {
            NBTTagCompound playerTag = new NBTTagCompound();
            playerTag.setLong("UuidMost", playerId.getMostSignificantBits());
            playerTag.setLong("UuidLeast", playerId.getLeastSignificantBits());
            NBTTagList entries = new NBTTagList();

            List<ResearchKey> persistedOrder = timeline(playerId).snapshotOldestFirst();
            if (persistedOrder.isEmpty() && research.forPlayer(playerId)
                .size() > 0) {
                persistedOrder = research.forPlayer(playerId)
                    .snapshot();
            }
            for (ResearchKey key : persistedOrder) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("ItemId", key.getItemId());
                entry.setInteger("Meta", key.getMeta());
                entry.setString("CanonicalNbt", key.getCanonicalNbt());
                NBTTagCompound originalTag = rawTemplate(playerId, key);
                if (originalTag != null) entry.setTag("Tag", originalTag.copy());
                entries.appendTag(entry);
            }
            playerTag.setTag("Entries", entries);
            players.appendTag(playerTag);
        }
        root.setTag("Players", players);

        NBTTagList undoPlayers = new NBTTagList();
        for (Map.Entry<UUID, PlayerResearchBackup> backupEntry : undoBackups.entrySet()) {
            UUID playerId = backupEntry.getKey();
            PlayerResearchBackup backup = backupEntry.getValue();
            if (backup == null || backup.size() == 0) continue;
            NBTTagCompound playerTag = new NBTTagCompound();
            playerTag.setLong("UuidMost", playerId.getMostSignificantBits());
            playerTag.setLong("UuidLeast", playerId.getLeastSignificantBits());
            NBTTagList entries = new NBTTagList();
            Map<ResearchKey, NBTTagCompound> backupTemplates = backup.templateCopies();
            for (ResearchKey key : backup.keys()) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("ItemId", key.getItemId());
                entry.setInteger("Meta", key.getMeta());
                entry.setString("CanonicalNbt", key.getCanonicalNbt());
                NBTTagCompound tag = backupTemplates.get(key);
                if (tag != null) entry.setTag("Tag", tag.copy());
                entries.appendTag(entry);
            }
            playerTag.setTag("Entries", entries);
            undoPlayers.appendTag(playerTag);
        }
        root.setTag("UndoPlayers", undoPlayers);
    }

    private void saveUndoSnapshot(UUID playerId) {
        List<ResearchKey> keys = timeline(playerId).snapshotOldestFirst();
        if (keys.isEmpty()) keys = research.forPlayer(playerId)
            .snapshot();
        Map<ResearchKey, NBTTagCompound> currentTemplates = templates.get(playerId);
        undoBackups.put(playerId, new PlayerResearchBackup(keys, currentTemplates));
        markDirty();
    }

    private List<ItemStack> stacksForKeys(UUID playerId, List<ResearchKey> keys) {
        List<ItemStack> out = new ArrayList<ItemStack>();
        for (ResearchKey key : keys) {
            ItemStack stack = dev.gtnhjourney.retrieval.ItemStackTemplateFactory
                .create(key, template(playerId, key), 1);
            if (stack != null) out.add(stack);
        }
        return Collections.unmodifiableList(out);
    }

    private ResearchTimeline timeline(UUID playerId) {
        ResearchTimeline timeline = timelines.get(playerId);
        if (timeline == null) {
            timeline = new ResearchTimeline();
            timelines.put(playerId, timeline);
        }
        return timeline;
    }

    private Collection<UUID> allPlayerIds() {
        // Every researched player with entries also has a template map; empty players need not be persisted.
        List<UUID> ids = new ArrayList<UUID>(templates.keySet());
        Collections.sort(ids);
        return ids;
    }

    private Map<ResearchKey, NBTTagCompound> templateMap(UUID playerId) {
        Map<ResearchKey, NBTTagCompound> map = templates.get(playerId);
        if (map == null) {
            map = new LinkedHashMap<ResearchKey, NBTTagCompound>();
            templates.put(playerId, map);
        }
        return map;
    }

    private NBTTagCompound rawTemplate(UUID playerId, ResearchKey key) {
        Map<ResearchKey, NBTTagCompound> map = templates.get(playerId);
        return map == null ? null : map.get(key);
    }

    private static boolean sameTemplate(NBTTagCompound a, NBTTagCompound b) {
        try {
            return NbtCanonicalizer.canonicalize(a)
                .equals(NbtCanonicalizer.canonicalize(b));
        } catch (IllegalArgumentException unsafeNbt) {
            return false;
        } catch (RuntimeException unsafeNbt) {
            return false;
        }
    }

    private static NBTTagCompound copyTag(ItemStack stack) {
        return ResearchTemplateNormalizer.normalize(stack);
    }
}
