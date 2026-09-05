package dev.gtnhjourney.recovery;

import net.minecraft.nbt.NBTTagCompound;

/** Immutable before/after state for one reversible non-research Journey action. */
public final class JourneyActionTransaction {

    private final long id;
    private final long timestamp;
    private final JourneyActionKind kind;
    private final String description;
    private final NBTTagCompound before;
    private final NBTTagCompound after;

    public JourneyActionTransaction(
        long id,
        long timestamp,
        JourneyActionKind kind,
        String description,
        NBTTagCompound before,
        NBTTagCompound after) {
        if (kind == null) throw new IllegalArgumentException("kind must not be null");
        this.id = id;
        this.timestamp = timestamp;
        this.kind = kind;
        this.description = description == null ? "" : description;
        this.before = copy(before);
        this.after = copy(after);
    }

    public long id() { return id; }
    public long timestamp() { return timestamp; }
    public JourneyActionKind kind() { return kind; }
    public String description() { return description; }
    public NBTTagCompound before() { return copy(before); }
    public NBTTagCompound after() { return copy(after); }

    private static NBTTagCompound copy(NBTTagCompound value) {
        return value == null ? new NBTTagCompound() : (NBTTagCompound) value.copy();
    }
}
