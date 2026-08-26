package dev.gtnhjourney.command;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

/** Prepares the physical held stack for an explicit server-side Journey research refresh. */
public final class HeldItemResearchCommand {

    private HeldItemResearchCommand() {}

    public static List<ItemStack> candidates(ItemStack held) {
        if (held == null || held.getItem() == null || held.stackSize <= 0) return Collections.emptyList();
        return Collections.singletonList(held.copy());
    }
}
