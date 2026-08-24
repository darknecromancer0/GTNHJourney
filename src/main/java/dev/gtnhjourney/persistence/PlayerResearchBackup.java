package dev.gtnhjourney.persistence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;

import dev.gtnhjourney.research.ResearchKey;

/** One deep-copied undo point for a player's destructive Journey operation. */
final class PlayerResearchBackup {

    private final List<ResearchKey> oldestFirst;
    private final Map<ResearchKey, NBTTagCompound> templates;

    PlayerResearchBackup(List<ResearchKey> oldestFirst, Map<ResearchKey, NBTTagCompound> templates) {
        this.oldestFirst = new ArrayList<ResearchKey>();
        this.templates = new LinkedHashMap<ResearchKey, NBTTagCompound>();
        if (oldestFirst == null) return;
        for (ResearchKey key : oldestFirst) {
            if (key == null) continue;
            this.oldestFirst.add(key);
            NBTTagCompound tag = templates == null ? null : templates.get(key);
            this.templates.put(key, tag == null ? null : (NBTTagCompound) tag.copy());
        }
    }

    List<ResearchKey> keys() {
        return Collections.unmodifiableList(new ArrayList<ResearchKey>(oldestFirst));
    }

    Map<ResearchKey, NBTTagCompound> templateCopies() {
        Map<ResearchKey, NBTTagCompound> out = new LinkedHashMap<ResearchKey, NBTTagCompound>();
        for (ResearchKey key : oldestFirst) {
            NBTTagCompound tag = templates.get(key);
            out.put(key, tag == null ? null : (NBTTagCompound) tag.copy());
        }
        return out;
    }

    int size() {
        return oldestFirst.size();
    }
}
