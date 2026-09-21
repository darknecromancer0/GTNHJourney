package dev.gtnhjourney.minecraft;

import java.util.ArrayList;
import java.util.Set;

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
    private static final String GT_TOOLBOX = "gregtech:gt.Item_Toolbox";
    private static final String GT_DETRAV_TOOL = "gregtech:gt.detrav.metatool.01";
    private static final int GT_PORTABLE_SCANNER_META = 32762;
    private static final int GT_DETRAV_SCANNER_META = 100;
    private static final int GT_UNIVERSAL_FLUID_CELL_META = 32405;
    private static final int GT_UNIVERSAL_FLUID_CELL_CAPACITY = 8000;
    private static final String RAILCRAFT_MACHINE_BETA = "Railcraft:machine.beta";
    private static final String RAILCRAFT_MACHINE_ZETA = "Railcraft:machine.zeta";
    private static final int RAILCRAFT_DEFAULT_WHITE = 15;
    private static final String AE2_NETWORK_VISUALISER = "appliedenergistics2:item.ToolNetworkVisualiser";
    private static final String AE2FC_WIRELESS_ULTRA_TERMINAL = "ae2fc:wireless_ultra_terminal";
    private static final String BIBLIO_CLIPBOARD = "BiblioCraft:item.BiblioClipboard";
    private static final String GRAVI_ADV_JETPACK = "GraviSuite:advJetpack";
    private static final String GRAVI_ADV_NANO_CHEST = "GraviSuite:advNanoChestPlate";
    private static final String BETTER_P2P_ADVANCED_MEMORY_CARD = "betterp2p:advanced_memory_card";
    private static final String DRACONIC_ITEM_DISLOCATOR = "DraconicEvolution:magnet";
    private static final String ENDERIO_SOUL_VIAL = "EnderIO:itemSoulVessel";
    private static final String ENDERIO_WIRELESS_CHARGER = "EnderIO:blockWirelessCharger";
    private static final String ENDERIO_CAP_BANK = "EnderIO:blockCapBank";
    private static final String ENDERIO_EXPERIENCE_OBELISK = "EnderIO:blockExperienceObelisk";
    private static final String ENDERIO_STIRLING_GENERATOR = "EnderIO:blockStirlingGenerator";
    private static final String TAINTED_VILLAGER = "Thaumcraft.TaintedVillager";
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
        if (GT_TOOLBOX.equals(registryId)) normalizeGtToolbox(tag);
        if (GT_DETRAV_TOOL.equals(registryId) && meta == GT_DETRAV_SCANNER_META) normalizeDetravScanner(tag);
        if (GT_META_ITEM_01.equals(registryId) && meta == GT_PORTABLE_SCANNER_META) normalizePortableScanner(tag);
        if (GT_META_ITEM_01.equals(registryId) && meta == GT_UNIVERSAL_FLUID_CELL_META) {
            normalizeUniversalFluidCell(tag);
        }
        if (isRailcraftTankStructure(registryId, meta)) normalizeRailcraftTankStructure(tag);
        if (AE2_NETWORK_VISUALISER.equals(registryId)) normalizeAe2NetworkVisualiser(tag);
        if (AE2FC_WIRELESS_ULTRA_TERMINAL.equals(registryId)) normalizeAe2fcUltraTerminal(tag);
        if (BIBLIO_CLIPBOARD.equals(registryId)) normalizeBiblioClipboard(tag);
        if (GRAVI_ADV_JETPACK.equals(registryId) || GRAVI_ADV_NANO_CHEST.equals(registryId)) {
            normalizeGraviFlightRuntime(tag);
        }
        if (BETTER_P2P_ADVANCED_MEMORY_CARD.equals(registryId)) normalizeBetterP2pAdvancedMemoryCard(tag);
        if (DRACONIC_ITEM_DISLOCATOR.equals(registryId)) tag.removeTag("ConfigProfiles");
        if (ENDERIO_SOUL_VIAL.equals(registryId)) normalizeSoulVial(tag);
        if (ENDERIO_WIRELESS_CHARGER.equals(registryId) || ENDERIO_CAP_BANK.equals(registryId)) {
            tag.removeTag("storedEnergyRF");
        }
        if (ENDERIO_EXPERIENCE_OBELISK.equals(registryId)) normalizeExperienceObelisk(tag);
        if (ENDERIO_STIRLING_GENERATOR.equals(registryId)) normalizeStirlingGenerator(tag);
        if (ENHANCED_LOOT_BAG.equals(registryId)) remove(tag, "ench", "RepairCost");
        if (CLEANSING_TALISMAN.equals(registryId)) tag.removeTag("enabled");
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

    private static void normalizeGtToolbox(NBTTagCompound tag) {
        // Contents are instance inventory, not toolbox identity. Replaying them from Journey would duplicate tools.
        tag.removeTag("gt5u.toolbox:Contents");
        tag.removeTag("gt5u.toolbox:ToolboxOpen");
    }

    private static void normalizeDetravScanner(NBTTagCompound tag) {
        NBTBase raw = tag.getTag("GT.ToolStats");
        if (!(raw instanceof NBTTagCompound)) return;
        NBTTagCompound stats = (NBTTagCompound) raw;
        stats.removeTag("DetravData");
    }

    private static void normalizePortableScanner(NBTTagCompound tag) {
        tag.removeTag("dataLinesCount");
        Set<String> keys = tag.func_150296_c();
        if (keys == null) return;
        for (String key : new ArrayList<String>(keys)) {
            if (isNumberedKey(key, "scanLine")) tag.removeTag(key);
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
        if (RAILCRAFT_MACHINE_ZETA.equals(registryId)) {
            return meta == 3 || meta == 4 || meta == 5 || meta == 9 || meta == 10 || meta == 11;
        }
        return false;
    }

    private static void normalizeRailcraftTankStructure(NBTTagCompound tag) {
        if (tag.hasKey("color", 99) && tag.getInteger("color") == RAILCRAFT_DEFAULT_WHITE) tag.removeTag("color");
    }

    private static void normalizeAe2NetworkVisualiser(NBTTagCompound tag) {
        remove(tag, "NETWORK_VISUALISER", "dim", "x", "y", "z");
    }

    private static void normalizeAe2fcUltraTerminal(NBTTagCompound tag) {
        // The 3x3 crafting grid is a cached work surface. Copying it would duplicate ingredients and every recipe or
        // tool-damage change would otherwise become a separate Journey state. Binding/profile NBT is intentionally kept.
        tag.removeTag("crafting");
        tag.removeTag("searchString");
        if (tag.hasKey("MagnetMode", 99) && tag.getInteger("MagnetMode") == 0) tag.removeTag("MagnetMode");
        if (tag.hasKey("name", 8) && tag.getString("name").isEmpty()) tag.removeTag("name");
    }

    private static void normalizeBiblioClipboard(NBTTagCompound tag) {
        tag.removeTag("currentPage");
        tag.removeTag("totalPages");
        Set<String> keys = tag.func_150296_c();
        if (keys == null) return;
        for (String key : new ArrayList<String>(keys)) {
            if (isNumberedKey(key, "page")) tag.removeTag(key);
        }
    }

    private static boolean isNumberedKey(String key, String prefix) {
        if (key == null || prefix == null || !key.startsWith(prefix) || key.length() <= prefix.length()) return false;
        for (int i = prefix.length(); i < key.length(); i++) {
            char c = key.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private static void normalizeGraviFlightRuntime(NBTTagCompound tag) {
        remove(tag, "isFlyActive", "isHoverActive", "toggleTimer");
    }

    private static void normalizeBetterP2pAdvancedMemoryCard(NBTTagCompound tag) {
        remove(tag, "frequency", "gui", "mode", "selectedIndex");
    }

    private static void normalizeSoulVial(NBTTagCompound tag) {
        normalizeCapturedEntityRuntime(tag);
        normalizeBogusEntityName(tag);

        if (!tag.hasKey("id", 8) || !TAINTED_VILLAGER.equals(tag.getString("id"))) return;

        // Live 1.1.39 data contains several Tainted Villagers that are semantically the same mob but differ in
        // random/default entity attributes, legacy numeric NBT widths, empty infusion payloads and quoted-empty names.
        // For this exact entity keep only the mob id and an actually meaningful custom display name.
        Set<String> keys = tag.func_150296_c();
        if (keys == null) return;
        for (String key : new ArrayList<String>(keys)) {
            if ("id".equals(key) || "CustomName".equals(key) || "display".equals(key)) continue;
            tag.removeTag(key);
        }
        normalizeBogusEntityName(tag);
    }

    private static void normalizeBogusEntityName(NBTTagCompound tag) {
        if (tag.hasKey("CustomName", 8) && isBlankEntityName(tag.getString("CustomName"))) {
            tag.removeTag("CustomName");
        }
        if (!tag.hasKey("display", 10)) return;
        NBTTagCompound display = tag.getCompoundTag("display");
        if (display.hasKey("Name", 8) && isBlankEntityName(display.getString("Name"))) display.removeTag("Name");
        if (display.func_150296_c().isEmpty()) tag.removeTag("display");
    }

    private static boolean isBlankEntityName(String value) {
        if (value == null) return true;
        String trimmed = value.trim();
        return trimmed.isEmpty() || "\"\"".equals(trimmed);
    }

    private static void normalizeExperienceObelisk(NBTTagCompound tag) {
        remove(tag,
            "Items",
            "eio.abstractMachine",
            "experience",
            "experienceLevel",
            "experienceTotal",
            "redstoneControlMode");
        removeAutoConfiguredDisplay(tag, "Experience Obelisk (Configured)");
    }

    private static void normalizeStirlingGenerator(NBTTagCompound tag) {
        remove(tag, "Items", "eio.abstractMachine", "redstoneControlMode", "storedEnergyRF");
        if (tag.hasKey("capacitorType", 99) && tag.getInteger("capacitorType") == 0) tag.removeTag("capacitorType");
        removeAutoConfiguredDisplay(tag, "Stirling Generator (Configured)");
    }

    private static void removeAutoConfiguredDisplay(NBTTagCompound tag, String expectedName) {
        if (!tag.hasKey("display", 10)) return;
        NBTTagCompound display = tag.getCompoundTag("display");
        if (display.func_150296_c().size() != 1 || !display.hasKey("Name", 8)) return;
        if (!expectedName.equals(display.getString("Name"))) return;
        tag.removeTag("display");
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
