package dev.gtnhjourney.debug;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Reads and writes only the Journey-owned mode tag on a Debug Researcher Tool stack. */
public final class DebugResearchToolState {

    public static final String MODE_TAG = "GTNHJourneyDebugResearchMode";

    private DebugResearchToolState() {}

    public static DebugResearchMode read(ItemStack stack) {
        if (stack == null || stack.getTagCompound() == null) return DebugResearchMode.BLOCK;
        String stored = stack.getTagCompound().getString(MODE_TAG);
        if (stored == null || stored.isEmpty()) return DebugResearchMode.BLOCK;
        try {
            return DebugResearchMode.valueOf(stored);
        } catch (IllegalArgumentException ignored) {
            return DebugResearchMode.BLOCK;
        }
    }

    public static void write(ItemStack stack, DebugResearchMode mode) {
        if (stack == null) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setString(MODE_TAG, (mode == null ? DebugResearchMode.BLOCK : mode).name());
    }
}
