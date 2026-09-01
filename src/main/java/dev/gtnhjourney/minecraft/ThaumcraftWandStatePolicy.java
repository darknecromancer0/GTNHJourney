package dev.gtnhjourney.minecraft;

import java.lang.reflect.Method;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Collapses Thaumcraft casting-wand vis into two Journey endpoints: completely empty or completely full. Construction
 * identity (rod/cap/sceptre/focus and unrelated NBT) is preserved exactly.
 */
public final class ThaumcraftWandStatePolicy {

    private static final String WAND_ID = "Thaumcraft:WandCasting";
    private static final String DEFAULT_WOOD_ROD = "wood";
    private static final String GREATWOOD_ROD = "greatwood";
    private static final String GREATWOOD_STAFF_ROD = "greatwood_staff";
    private static final int DEFAULT_WOOD_MAX_VIS = 2500;
    private static final int GREATWOOD_MAX_VIS = 5000;
    private static final int GREATWOOD_STAFF_MAX_VIS = 12500;
    private static final String[] VIS_KEYS = { "aer", "aqua", "ignis", "ordo", "perditio", "terra" };
    private static final String[] OBJECT_IN_USE_KEYS = { "IIUX", "IIUY", "IIUZ" };

    private ThaumcraftWandStatePolicy() {}

    public static void normalize(ItemStack owner, NBTTagCompound tag) {
        if (tag == null || !matches(owner)) return;
        int maxVis = readMaxVis(owner);
        if (maxVis <= 0) maxVis = fallbackMaxVis(tag);
        normalizeTag(WAND_ID, tag, maxVis);
    }

    /** Registry-id-only migration used before optional Thaumcraft classes are reconstructed. */
    static void normalizePersisted(String itemId, int meta, NBTTagCompound tag) {
        if (!WAND_ID.equals(itemId) || tag == null) return;
        normalizeTag(itemId, tag, fallbackMaxVis(tag));
    }

    /** Pure NBT endpoint transform kept package-private for regression tests. */
    static void normalizeTag(String registryId, NBTTagCompound tag, int maxVis) {
        if (!WAND_ID.equals(registryId) || tag == null) return;

        for (String key : OBJECT_IN_USE_KEYS) tag.removeTag(key);

        boolean filled = hasAnyVis(tag);
        if (!filled) {
            for (String key : VIS_KEYS) tag.setInteger(key, 0);
            return;
        }

        // Fail closed for unknown custom rods if Thaumcraft's runtime method is unavailable. Interaction coordinates are
        // still transient and safe to remove, but partial vis remains exact until the real item can provide a capacity.
        if (maxVis <= 0) return;
        for (String key : VIS_KEYS) tag.setInteger(key, maxVis);
    }

    public static boolean matches(ItemStack stack) {
        return WAND_ID.equals(registryId(stack));
    }

    private static boolean hasAnyVis(NBTTagCompound tag) {
        for (String key : VIS_KEYS) {
            if (tag.hasKey(key, 99) && tag.getInteger(key) > 0) return true;
        }
        return false;
    }

    private static int readMaxVis(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return -1;
        try {
            Method method = stack.getItem()
                .getClass()
                .getMethod("getMaxVis", ItemStack.class);
            Object value = method.invoke(stack.getItem(), stack);
            if (!(value instanceof Number)) return -1;
            int maxVis = ((Number) value).intValue();
            return maxVis > 0 ? maxVis : -1;
        } catch (ReflectiveOperationException ignored) {
            return -1;
        } catch (RuntimeException ignored) {
            return -1;
        } catch (LinkageError ignored) {
            return -1;
        }
    }

    /**
     * Core Thaumcraft capacities are safe persisted-data fallbacks before registry reconstruction. Runtime stacks always
     * prefer ItemWandCasting#getMaxVis so addon rods keep their exact registered capacity. Values are centivis.
     */
    private static int fallbackMaxVis(NBTTagCompound tag) {
        if (tag == null) return -1;
        String rod = tag.hasKey("rod", 8) ? tag.getString("rod") : "";
        int baseCapacity;
        if (rod.isEmpty() || DEFAULT_WOOD_ROD.equalsIgnoreCase(rod)) {
            baseCapacity = DEFAULT_WOOD_MAX_VIS;
        } else if (GREATWOOD_ROD.equalsIgnoreCase(rod)) {
            baseCapacity = GREATWOOD_MAX_VIS;
        } else if (GREATWOOD_STAFF_ROD.equalsIgnoreCase(rod)) {
            baseCapacity = GREATWOOD_STAFF_MAX_VIS;
        } else {
            return -1;
        }
        return tag.getBoolean("sceptre") ? baseCapacity * 3 / 2 : baseCapacity;
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
}
