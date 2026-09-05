package dev.gtnhjourney.nei;

import net.minecraft.item.ItemStack;

import dev.gtnhjourney.research.ResearchKey;

/** Immutable presentation metadata consumed by the pure Journey sorting planner. */
public final class JourneySortEntry {

    private final ResearchKey key;
    private final ItemStack stack;
    private final int nativeIndex;
    private final String nativeFamily;
    private final String modGroup;
    private final String typeGroup;
    private final String kindGroup;
    private final String displayName;
    private final long unlockSequence;
    private final long activitySequence;
    private final long favouriteSequence;
    private final int canonicalIndex;

    public JourneySortEntry(
        ResearchKey key,
        ItemStack stack,
        int nativeIndex,
        String nativeFamily,
        String modGroup,
        String typeGroup,
        String kindGroup,
        String displayName,
        long unlockSequence,
        long activitySequence,
        long favouriteSequence,
        int canonicalIndex) {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        this.key = key;
        this.stack = stack;
        this.nativeIndex = nativeIndex;
        this.nativeFamily = safe(nativeFamily, key.getItemId());
        this.modGroup = safe(modGroup, "misc");
        this.typeGroup = safe(typeGroup, "misc");
        this.kindGroup = safe(kindGroup, this.typeGroup);
        this.displayName = safe(displayName, key.getItemId());
        this.unlockSequence = unlockSequence;
        this.activitySequence = activitySequence;
        this.favouriteSequence = favouriteSequence;
        this.canonicalIndex = canonicalIndex;
    }

    public ResearchKey key() { return key; }
    public ItemStack stack() { return stack; }
    public int nativeIndex() { return nativeIndex; }
    public String nativeFamily() { return nativeFamily; }
    public String modGroup() { return modGroup; }
    public String typeGroup() { return typeGroup; }
    public String kindGroup() { return kindGroup; }
    public String displayName() { return displayName; }
    public long unlockSequence() { return unlockSequence; }
    public long activitySequence() { return activitySequence; }
    public long favouriteSequence() { return favouriteSequence; }
    public int canonicalIndex() { return canonicalIndex; }

    private static String safe(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
