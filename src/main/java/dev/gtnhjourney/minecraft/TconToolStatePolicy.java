package dev.gtnhjourney.minecraft;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;

/** Ownership check for Tinkers' Construct 1.7.10 ToolCore runtime durability and Iguana leveling state. */
public final class TconToolStatePolicy {

    private static final Class<?> TOOL_CORE = load("tconstruct.library.tools.ToolCore");
    private static final Set<String> TRANSIENT_IGUANA_COUNTERS = new HashSet<String>(Arrays.asList(
        "ExtraRedstone",
        "ExtraLuckLooting",
        "ExtraAutosmelt",
        "ExtraSilkTouch",
        "ExtraDiamond",
        "ExtraEmerald",
        "ExtraRepair",
        "ExtraReinforced",
        "ExtraAttack",
        "ExtraFiery",
        "ExtraSmite",
        "ExtraBaneOfArthropods",
        "ExtraBeheading",
        "ExtraLifeSteal",
        "ExtraKnockback",
        "ExtraJagged",
        "ExtraStonebound",
        "ExtraCritical"));

    private TconToolStatePolicy() {}

    public static boolean isVerifiedTool(ItemStack stack) {
        return stack != null && stack.getItem() != null && TOOL_CORE != null && TOOL_CORE.isInstance(stack.getItem());
    }

    static boolean isTransientIguanaCounterKey(String key) {
        return key != null && TRANSIENT_IGUANA_COUNTERS.contains(key);
    }

    public static boolean isRuntimeAvailable() {
        return TOOL_CORE != null;
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name, false, TconToolStatePolicy.class.getClassLoader());
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }
}
