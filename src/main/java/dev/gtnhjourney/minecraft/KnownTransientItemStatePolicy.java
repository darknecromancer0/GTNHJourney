package dev.gtnhjourney.minecraft;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

/** Registry-scoped semantic cleanup for runtime/use state verified from live GTNH Journey dumps. */
public final class KnownTransientItemStatePolicy {

    private static final String AIR_FILTER = "miscutils:itemAirFilter";
    private static final String GOLDEN_LASSO = "ExtraUtilities:golden_lasso";
    private static final String OVEN_GLOVE = "dreamcraft:OvenGlove";
    private static final String OVEN_GLOVE_ALT = "dreamcraft:item.OvenGlove";
    private static final int OVEN_GLOVE_FULL_DURABILITY = 1000;
    private static final String GT_META_ITEM_01 = "gregtech:gt.metaitem.01";
    private static final int GT_UNIVERSAL_FLUID_CELL_META = 32405;
    private static final int GT_UNIVERSAL_FLUID_CELL_CAPACITY = 8000;

    private KnownTransientItemStatePolicy() {}

    public static void normalize(ItemStack stack, NBTTagCompound tag) {
        if (stack == null || stack.getItem() == null || tag == null) return;
        String registryId = registryId(stack);
        if (registryId == null) return;
        normalize(registryId, stack.getItemDamage(), tag);
    }

    static void normalize(String registryId, int meta, NBTTagCompound tag) {
        if (registryId == null || tag == null) return;

        if (registryId.startsWith("chisel:") && tag.hasKey("chiselTarget")) {
            // Chisel writes the most recently targeted block stack here. It is use/UI state, not a different chisel.
            tag.removeTag("chiselTarget");
        }

        if (registryId.startsWith("betterbuilderswands:wand") && tag.hasKey("bbw")) {
            // Better Builder's Wands stores last-block and placement-coordinate history in this wrapper.
            tag.removeTag("bbw");
        }

        if (AIR_FILTER.equals(registryId)) normalizeAirFilter(tag);
        if (GOLDEN_LASSO.equals(registryId) && meta == 1) normalizeCapturedEntityRuntime(tag);
        if (OVEN_GLOVE.equals(registryId) || OVEN_GLOVE_ALT.equals(registryId)) normalizeOvenGlove(tag);
        if (GT_META_ITEM_01.equals(registryId) && meta == GT_UNIVERSAL_FLUID_CELL_META) {
            normalizeUniversalFluidCell(tag);
        }
    }

    private static void normalizeAirFilter(NBTTagCompound tag) {
        NBTBase raw = tag.getTag("AirFilter");
        if (!(raw instanceof NBTTagCompound)) return;
        NBTTagCompound filter = (NBTTagCompound) raw;
        filter.removeTag("Damage");
        if (filter.func_150296_c().isEmpty()) tag.removeTag("AirFilter");
    }

    private static void normalizeOvenGlove(NBTTagCompound tag) {
        if (!tag.hasKey("Durability", 99)) return;
        int observed = tag.getInteger("Durability");
        if (observed > 0 && observed <= OVEN_GLOVE_FULL_DURABILITY) {
            tag.setInteger("Durability", OVEN_GLOVE_FULL_DURABILITY);
        }
    }

    private static void normalizeUniversalFluidCell(NBTTagCompound tag) {
        NBTBase raw = tag.getTag("GT.FluidContent");
        if (!(raw instanceof NBTTagCompound)) return;
        NBTTagCompound fluid = (NBTTagCompound) raw;
        if (!fluid.hasKey("FluidName", 8) || fluid.getString("FluidName").isEmpty()) return;
        if (!fluid.hasKey("Amount", 99) || fluid.getInteger("Amount") <= 0) return;
        fluid.setInteger("Amount", GT_UNIVERSAL_FLUID_CELL_CAPACITY);
    }

    private static void normalizeCapturedEntityRuntime(NBTTagCompound tag) {
        // Golden Lasso stores a serialized entity. Preserve entity type, age/baby state, colour, equipment, custom
        // name, attributes and mod-specific permanent upgrades while dropping only moment-to-moment world/runtime data.
        remove(tag,
            "UUIDMost",
            "UUIDLeast",
            "Pos",
            "Motion",
            "Rotation",
            "Dimension",
            "OnGround",
            "FallDistance",
            "Air",
            "Fire",
            "HurtTime",
            "DeathTime",
            "AttackTime",
            "PortalCooldown",
            "Health",
            "HealF",
            "AbsorptionAmount",
            "InLove",
            "oiltweak.inOil");
    }

    private static void remove(NBTTagCompound tag, String... keys) {
        for (String key : keys) tag.removeTag(key);
    }

    private static String registryId(ItemStack stack) {
        try {
            GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
            return id == null ? null : id.toString();
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }
}
