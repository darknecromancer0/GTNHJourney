package dev.gtnhjourney.minecraft;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

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

    /** TCon ammunition stores roughly one usable projectile per ten durability points, rounded up. */
    static int fullAmmoForDurability(int totalDurability) {
        if (totalDurability <= 0) return 0;
        return (int) (((long) totalDurability + 9L) / 10L);
    }

    /** Normalizes depleted ArrowAmmo/BoltAmmo-like ToolCore stacks back to their proven full ammunition endpoint. */
    static void normalizeAmmoState(NBTTagCompound root) {
        if (root == null) return;
        NBTBase raw = root.getTag("InfiTool");
        if (!(raw instanceof NBTTagCompound)) return;
        NBTTagCompound infiTool = (NBTTagCompound) raw;
        if (!infiTool.hasKey("Ammo", 99) || !infiTool.hasKey("TotalDurability", 99)) return;
        if (infiTool.getInteger("Ammo") < 0) return;
        int fullAmmo = fullAmmoForDurability(infiTool.getInteger("TotalDurability"));
        if (fullAmmo > 0) infiTool.setInteger("Ammo", fullAmmo);
    }

    /** Iguana/TCon can repeatedly prepend the same white format code while updating a tool's generated name. */
    static void normalizeDisplayName(NBTTagCompound root) {
        if (root == null) return;
        NBTBase raw = root.getTag("display");
        if (!(raw instanceof NBTTagCompound)) return;
        NBTTagCompound display = (NBTTagCompound) raw;
        if (!display.hasKey("Name", 8)) return;
        String name = display.getString("Name");
        while (name.startsWith("§f§f")) name = name.substring(2);
        display.setString("Name", name);
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
