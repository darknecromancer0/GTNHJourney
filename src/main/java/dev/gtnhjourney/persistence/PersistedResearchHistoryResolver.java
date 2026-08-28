package dev.gtnhjourney.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import dev.gtnhjourney.minecraft.NbtCanonicalizer;
import dev.gtnhjourney.minecraft.PersistedResearchEntryResolver;
import dev.gtnhjourney.recovery.ResearchEntrySnapshot;
import dev.gtnhjourney.research.ResearchKey;

/** Recanonicalizes research entries stored outside the primary research file without losing chronology. */
final class PersistedResearchHistoryResolver {

    private PersistedResearchHistoryResolver() {}

    static ListResult resolveEntries(NBTTagList tags, boolean reindexChronology) {
        Map<ResearchKey, ResearchEntrySnapshot> unique = new LinkedHashMap<ResearchKey, ResearchEntrySnapshot>();
        boolean changed = false;
        if (tags != null) {
            for (int i = 0; i < tags.tagCount(); i++) {
                EntryResult resolved = resolveEntry(tags.getCompoundTagAt(i));
                changed |= resolved.changed();
                ResearchEntrySnapshot entry = resolved.entry();
                if (entry == null) continue;
                if (unique.containsKey(entry.key())) {
                    changed = true;
                    continue;
                }
                unique.put(entry.key(), entry);
            }
        }

        List<ResearchEntrySnapshot> entries = new ArrayList<ResearchEntrySnapshot>(unique.values());
        if (reindexChronology) {
            for (int i = 0; i < entries.size(); i++) {
                ResearchEntrySnapshot entry = entries.get(i);
                if (entry.timelineIndex() == i) continue;
                entries.set(i, new ResearchEntrySnapshot(entry.key(), entry.template(), i));
                changed = true;
            }
        }
        return new ListResult(entries, changed);
    }

    static EntryResult resolveEntry(NBTTagCompound tag) {
        if (tag == null) return new EntryResult(null, false);
        String itemId = tag.getString("ItemId");
        if (itemId == null || itemId.isEmpty()) return new EntryResult(null, true);

        int meta = tag.getInteger("Meta");
        String canonical = tag.getString("CanonicalNbt");
        NBTTagCompound template = tag.hasKey("Tag", 10) ? tag.getCompoundTag("Tag") : null;
        int timelineIndex = Math.max(0, tag.getInteger("TimelineIndex"));
        ResearchKey original = new ResearchKey(itemId, meta, canonical == null ? "" : canonical);

        PersistedResearchEntryResolver.ResolvedEntry resolved = PersistedResearchEntryResolver
            .resolveEntry(itemId, meta, canonical, template);
        if (resolved == null) return new EntryResult(null, true);

        ResearchEntrySnapshot entry = new ResearchEntrySnapshot(resolved.key(), resolved.template(), timelineIndex);
        boolean changed = !original.equals(resolved.key()) || !sameTemplate(template, resolved.template());
        return new EntryResult(entry, changed);
    }

    private static boolean sameTemplate(NBTTagCompound left, NBTTagCompound right) {
        try {
            return NbtCanonicalizer.canonicalize(left)
                .equals(NbtCanonicalizer.canonicalize(right));
        } catch (IllegalArgumentException unsafe) {
            return false;
        } catch (RuntimeException unsafe) {
            return false;
        }
    }

    static final class EntryResult {

        private final ResearchEntrySnapshot entry;
        private final boolean changed;

        EntryResult(ResearchEntrySnapshot entry, boolean changed) {
            this.entry = entry;
            this.changed = changed;
        }

        ResearchEntrySnapshot entry() {
            return entry;
        }

        boolean changed() {
            return changed;
        }
    }

    static final class ListResult {

        private final List<ResearchEntrySnapshot> entries;
        private final boolean changed;

        ListResult(List<ResearchEntrySnapshot> entries, boolean changed) {
            this.entries = entries;
            this.changed = changed;
        }

        List<ResearchEntrySnapshot> entries() {
            return entries;
        }

        boolean changed() {
            return changed;
        }
    }
}
