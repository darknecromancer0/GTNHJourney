package dev.gtnhjourney.minecraft;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

/** Registry-scoped semantic cleanup for runtime/use state verified from live GTNH Journey dumps. */
public final class KnownTransientItemStatePolicy {

    private static final String AIR_FILTER = "miscutils:itemAirFilter";
    private static final String GOLDEN_LASSO = "ExtraUtilities:golden_lasso";
    private static final String JABBA_MOVER = "JABBA:mover";
    private static final String OVEN_GLOVE = "dreamcraft:OvenGlove";
    private static final String OVEN_GLOVE_ALT = "dreamcraft:item.OvenGlove";
    private static final int OVEN_GLOVE_FULL_DURABILITY = 1000;
    private static final String GT_META_ITEM_01 = "gregtech:gt.metaitem.01";
    private static final String GT_BLOCK_MACHINES = "gregtech:gt.blockmachines";
    private static final int GT_UNIVERSAL_FLUID_CELL_META = 32405;
    private static final int GT_UNIVERSAL_FLUID_CELL_CAPACITY = 8000;
    private static final String RAILCRAFT_MACHINE_BETA = "Railcraft:machine.beta";
    private static final String RAILCRAFT_MACHINE_ZETA = "Railcraft:machine.zeta";
    private static final int RAILCRAFT_DEFAULT_WHITE = 15;
    private static final String AE2_NETWORK_VISUALISER = "appliedenergistics2:item.ToolNetworkVisualiser";
    private static final String BETTER_P2P_ADVANCED_MEMORY_CARD = "betterp2p:advanced_memory_card";
    private static final String VANILLA_WATER = "minecraft:water";

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
            tag.removeTag("chiselTarget");
        }
        if (registryId.startsWith("betterbuilderswands:wand") && tag.hasKey("bbw")) {
            tag.removeTag("bbw");
        }

        if (AIR_FILTER.equals(registryId)) normalizeAirFilter(tag);
        if (GOLDEN_LASSO.equals(registryId) && meta == 1) normalizeCapturedEntityRuntime(tag);
        if (JABBA_MOVER.equals(registryId)) tag.removeTag("Container");
        if (OVEN_GLOVE.equals(registryId) || OVEN_GLOVE_ALT.equals(registryId)) normalizeOvenGlove(tag);
        if (GT_BLOCK_MACHINES.equals(registryId)) tag.removeTag("gt.covers");
        if (GT_META_ITEM_01.equals(registryId) && meta == GT_UNIVERSAL_FLUID_CELL_META) {
            normalizeUniversalFluidCell(tag);
        }
        if (isRailcraftTankStructure(registryId, meta)) normalizeRailcraftTankStructure(tag);
        if (AE2_NETWORK_VISUALISER.equals(registryId)) normalizeAe2NetworkVisualiser(tag);
        if (BETTER_P2P_ADVANCED_MEMORY_CARD.equals(registryId)) normalizeBetterP2pAdvancedMemoryCard(tag);
        if (VANILLA_WATER.equals(registryId)) normalizeGeneratedWaterAmountName(tag);
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

    private static boolean isRailcraftTankStructure(String registryId, int meta) {
        if (RAILCRAFT_MACHINE_BETA.equals(registryId)) {
            return meta == 0 || meta == 1 || meta == 2 || meta == 13 || meta == 14 || meta == 15;
        }
        if (RAILCRAFT_MACHINE_ZETA.equals(registryId)) return meta == 3 || meta == 4 || meta == 5;
        return false;
    }

    private static void normalizeRailcraftTankStructure(NBTTagCompound tag) {
        if (tag.hasKey("color", 99) && tag.getInteger("color") == RAILCRAFT_DEFAULT_WHITE) tag.removeTag("color");
    }

    private static void normalizeAe2NetworkVisualiser(NBTTagCompound tag) {
        remove(tag, "NETWORK_VISUALISER", "dim", "x", "y", "z");
    }

    private static void normalizeBetterP2pAdvancedMemoryCard(NBTTagCompound tag) {
        remove(tag, "frequency", "gui", "mode", "selectedIndex");
    }

    private static void normalizeGeneratedWaterAmountName(NBTTagCompound tag) {
        if (tag.func_150296_c().size() != 1 || !tag.hasKey("display", 10)) return;
        NBTTagCompound display = tag.getCompoundTag("display");
        if (display.func_150296_c().size() != 1 || !display.hasKey("Name", 8)) return;
        if (!isGeneratedWaterAmountName(display.getString("Name"))) return;
        tag.removeTag("display");
    }

    private static boolean isGeneratedWaterAmountName(String name) {
        if (name == null || !name.endsWith("L Water")) return false;
        String amount = name.substring(0, name.length() - "L Water".length());
        if (amount.isEmpty()) return false;
        for (int i = 0; i < amount.length(); i++) {
            if (amount.charAt(i) < '0' || amount.charAt(i) > '9') return false;
        }
        return true;
    }

    private static void normalizeCapturedEntityRuntime(NBTTagCompound tag) {
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
