package dev.gtnhjourney.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

/** Defensive, read-only inventory snapshot used by the migration debug tool. */
public final class ReadOnlyInventoryCollector {

    private ReadOnlyInventoryCollector() {}

    public static List<ItemStack> collect(SlotSource source) {
        if (source == null) return Collections.emptyList();

        final int size;
        try {
            size = source.size();
        } catch (RuntimeException | LinkageError ignored) {
            return Collections.emptyList();
        }
        if (size <= 0) return Collections.emptyList();

        List<ItemStack> result = new ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            final ItemStack stack;
            try {
                stack = source.get(slot);
            } catch (RuntimeException | LinkageError ignored) {
                continue;
            }
            if (stack == null || stack.getItem() == null || stack.stackSize <= 0) continue;
            result.add(stack.copy());
        }
        return result;
    }

    public interface SlotSource {

        int size();

        ItemStack get(int slot);
    }
}
