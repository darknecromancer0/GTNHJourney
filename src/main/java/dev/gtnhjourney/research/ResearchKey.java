package dev.gtnhjourney.research;

import java.util.Objects;

/**
 * Stable identity for one researched item state.
 *
 * <p>Stack size is intentionally not part of the key. Version 0.1 keeps metadata and the complete canonical NBT text
 * distinct so that materially different GTNH stacks cannot be merged accidentally.</p>
 */
public final class ResearchKey implements Comparable<ResearchKey> {

    private final String itemId;
    private final int meta;
    private final String canonicalNbt;

    public ResearchKey(String itemId, int meta, String canonicalNbt) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        this.itemId = itemId;
        this.meta = meta;
        this.canonicalNbt = canonicalNbt == null ? "" : canonicalNbt;
    }

    public String getItemId() {
        return itemId;
    }

    public int getMeta() {
        return meta;
    }

    public String getCanonicalNbt() {
        return canonicalNbt;
    }

    @Override
    public int compareTo(ResearchKey other) {
        int byItem = itemId.compareTo(other.itemId);
        if (byItem != 0) {
            return byItem;
        }

        int byMeta = Integer.compare(meta, other.meta);
        if (byMeta != 0) {
            return byMeta;
        }

        return canonicalNbt.compareTo(other.canonicalNbt);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ResearchKey)) {
            return false;
        }
        ResearchKey other = (ResearchKey) obj;
        return meta == other.meta
            && itemId.equals(other.itemId)
            && canonicalNbt.equals(other.canonicalNbt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, meta, canonicalNbt);
    }

    @Override
    public String toString() {
        return itemId + "@" + meta + "#" + canonicalNbt;
    }
}
