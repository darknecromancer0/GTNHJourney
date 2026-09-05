package dev.gtnhjourney.nei;

import java.util.Locale;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraftforge.oredict.OreDictionary;

import dev.gtnhjourney.research.ResearchKey;

/** Fail-safe broad Type and narrow Kind classification used only for presentation sorting. */
final class JourneySemanticClassifier {

    private JourneySemanticClassifier() {}

    static String modGroup(ResearchKey key) {
        if (key == null || key.getItemId() == null) return "misc";
        int colon = key.getItemId().indexOf(':');
        return colon <= 0 ? "minecraft" : key.getItemId().substring(0, colon).toLowerCase(Locale.ROOT);
    }

    static String typeGroup(ItemStack stack, ResearchKey key) {
        return JourneyPanelPrecache.semantic(stack, key, currentDisplayName(stack, key)).typeGroup;
    }

    static String kindGroup(ItemStack stack, ResearchKey key) {
        return JourneyPanelPrecache.semantic(stack, key, currentDisplayName(stack, key)).kindGroup;
    }

    static String uncachedTypeGroup(ItemStack stack, ResearchKey key) {
        Item item = stack == null ? null : stack.getItem();
        if (item instanceof ItemArmor) return "01-armor";
        if (item instanceof ItemSword || item instanceof ItemBow) return "02-weapons";
        if (item instanceof ItemFood) return "07-food";
        if (item instanceof ItemTool || hasToolClass(stack)) return "03-tools";
        if (item instanceof ItemBlock) return "08-blocks";

        String evidence = evidence(stack, key);
        if (containsAny(evidence, "battery", "lapotron", "energy crystal", "energyorb", "powerunit")) return "06-power";
        if (containsAny(evidence, "fluid", "cell", "canister", "bucket", "capsule")) return "09-fluids";
        if (containsAny(evidence, "seed", "sapling", "crop")) return "11-crops";
        if (containsAny(evidence, "wand", "thaum", "aspect", "mana", "rune", "vis", "essence")) return "12-magic";
        if (containsAny(evidence, "chest", "crate", "barrel", "storage", "drawer", "tank")) return "13-storage";
        if (containsAny(evidence, "machine", "multiblock", "reactor", "furnace", "assembler", "centrifuge", "macerator")) return "04-machines";
        if (containsAny(evidence, "motor", "conveyor", "pump", "robotarm", "robot arm", "piston", "circuit", "hatch", "bus")) return "05-components";
        if (materialKind(evidence) != null) return "10-materials";
        return "99-misc";
    }

    static String uncachedKindGroup(ItemStack stack, ResearchKey key) {
        Item item = stack == null ? null : stack.getItem();
        if (item instanceof ItemArmor) {
            switch (((ItemArmor) item).armorType) {
                case 0: return "armor-helmet";
                case 1: return "armor-chestplate";
                case 2: return "armor-leggings";
                case 3: return "armor-boots";
                default: return "armor-other";
            }
        }
        if (item instanceof ItemSword) return "weapon-sword";
        if (item instanceof ItemBow) return "weapon-bow";
        if (item instanceof ItemPickaxe) return "tool-pickaxe";
        if (item instanceof ItemAxe) return "tool-axe";
        if (item instanceof ItemSpade) return "tool-shovel";
        if (item instanceof ItemHoe) return "tool-hoe";

        String evidence = evidence(stack, key);
        if (containsAny(evidence, "pickaxe")) return "tool-pickaxe";
        if (containsAny(evidence, "shovel", "spade")) return "tool-shovel";
        if (containsAny(evidence, "hammer", "mallet")) return "tool-hammer";
        if (containsAny(evidence, "wrench")) return "tool-wrench";
        if (containsAny(evidence, "cutter", "wirecutter")) return "tool-cutter";
        if (containsAny(evidence, "scanner")) return "tool-scanner";
        if (containsAny(evidence, "battery", "lapotron", "energy crystal")) return "power-battery";
        String material = materialKind(evidence);
        if (material != null) return "material-" + material;
        if (containsAny(evidence, "fluid", "cell", "canister", "capsule")) return "fluid-cell-container";
        return uncachedTypeGroup(stack, key);
    }

    private static boolean hasToolClass(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        try {
            Set<String> classes = stack.getItem().getToolClasses(stack);
            return classes != null && !classes.isEmpty();
        } catch (RuntimeException ignored) {
            return false;
        } catch (LinkageError ignored) {
            return false;
        }
    }

    private static String currentDisplayName(ItemStack stack, ResearchKey key) {
        try {
            String name = stack == null ? null : stack.getDisplayName();
            if (name != null && !name.isEmpty()) return name;
        } catch (RuntimeException ignored) {}
        return key == null ? "" : key.getItemId();
    }

    private static String evidence(ItemStack stack, ResearchKey key) {
        StringBuilder out = new StringBuilder();
        if (key != null) out.append(key.getItemId()).append(' ');
        if (stack != null && stack.getItem() != null) {
            try { out.append(stack.getDisplayName()).append(' '); } catch (RuntimeException ignored) {}
            try { out.append(stack.getItem().getUnlocalizedName()).append(' '); } catch (RuntimeException ignored) {}
            try {
                int[] ids = OreDictionary.getOreIDs(stack);
                for (int id : ids) out.append(OreDictionary.getOreName(id)).append(' ');
            } catch (RuntimeException ignored) {}
        }
        return out.toString().toLowerCase(Locale.ROOT).replace("_", "").replace(".", "");
    }

    private static String materialKind(String evidence) {
        String[] kinds = { "ingot", "plate", "rod", "stick", "wire", "cable", "gear", "gem", "ore", "dust", "screw", "foil", "ring", "bolt", "spring" };
        for (String kind : kinds) if (evidence.contains(kind)) return "stick".equals(kind) ? "rod" : kind;
        return null;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }
}
