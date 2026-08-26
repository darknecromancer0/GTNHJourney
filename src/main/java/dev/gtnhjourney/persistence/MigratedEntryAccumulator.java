package dev.gtnhjourney.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.nbt.NBTTagCompound;

/** Keeps the first valid migrated occurrence for each canonical research key. */
final class MigratedEntryAccumulator {

    private final Map<ResearchKey, NBTTagCompound> entries = new LinkedHashMap<ResearchKey, NBTTagCompound>();

    boolean accept(ResearchKey key, NBTTagCompound template) {
        if (key == null || entries.containsKey(key)) return false;
        entries.put(key, copy(template));
        return true;
    }

    List<ResearchKey> keys() {
        return new ArrayList<ResearchKey>(entries.keySet());
    }

    NBTTagCompound template(ResearchKey key) {
        return key == null ? null : copy(entries.get(key));
    }

    Map<ResearchKey, NBTTagCompound> templateCopies() {
        Map<ResearchKey, NBTTagCompound> out = new LinkedHashMap<ResearchKey, NBTTagCompound>();
        for (Map.Entry<ResearchKey, NBTTagCompound> entry : entries.entrySet()) {
            out.put(entry.getKey(), copy(entry.getValue()));
        }
        return out;
    }

    private static NBTTagCompound copy(NBTTagCompound tag) {
        return tag == null ? null : (NBTTagCompound) tag.copy();
    }
}
