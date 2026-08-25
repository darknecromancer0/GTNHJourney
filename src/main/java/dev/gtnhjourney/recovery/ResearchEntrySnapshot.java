package dev.gtnhjourney.recovery;

import net.minecraft.nbt.NBTTagCompound;

import dev.gtnhjourney.research.ResearchKey;

/** Immutable exact recovery payload for one researched semantic state. */
public final class ResearchEntrySnapshot {

    private final ResearchKey key;
    private final NBTTagCompound template;
    private final int timelineIndex;

    public ResearchEntrySnapshot(ResearchKey key, NBTTagCompound template, int timelineIndex) {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        if (timelineIndex < 0) throw new IllegalArgumentException("timelineIndex must be >= 0");
        this.key = key;
        this.template = template == null ? null : (NBTTagCompound) template.copy();
        this.timelineIndex = timelineIndex;
    }

    public ResearchKey key() {
        return key;
    }

    public NBTTagCompound template() {
        return template == null ? null : (NBTTagCompound) template.copy();
    }

    public int timelineIndex() {
        return timelineIndex;
    }
}
