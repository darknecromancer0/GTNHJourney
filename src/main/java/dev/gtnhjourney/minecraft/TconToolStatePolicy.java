package dev.gtnhjourney.minecraft;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import cpw.mods.fml.common.registry.GameRegistry;
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

    /** Normalizes generated TCon display noise while preserving intentional custom names. */
    static void normalizeDisplayName(ItemStack stack, NBTTagCompound root) {
        normalizeDisplayName(registryId(stack), root);
    }

    /** Backward-compatible tag-only normalization used where the owning stack is unavailable. */
    static void normalizeDisplayName(NBTTagCompound root) {
        normalizeDisplayName(null, root);
    }

    private static void normalizeDisplayName(String registryId, NBTTagCompound root) {
        if (root == null) return;
        NBTBase raw = root.getTag("display");
        if (!(raw instanceof NBTTagCompound)) return;
        NBTTagCompound display = (NBTTagCompound) raw;
        if (!display.hasKey("Name", 8)) return;
        display.setString("Name", normalizeGeneratedDisplayName(registryId, display.getString("Name")));
    }

    static String normalizeGeneratedDisplayName(String registryId, String name) {
        if (name == null) return "";
        String normalized = name;
        while (normalized.startsWith("§f§f")) normalized = normalized.substring(2);
        if ("TConstruct:shovel".equals(registryId) && normalized.startsWith("§f") && normalized.endsWith(" Hatchet")) {
            return normalized.substring(0, normalized.length() - " Hatchet".length()) + " Shovel";
        }
        return normalized;
    }

    public static boolean isRuntimeAvailable() {
        return TOOL_CORE != null;
    }

    private static String registryId(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        try {
            GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
            return id == null ? null : id.toString();
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
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
