package dev.gtnhjourney.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Keeps retrievable Journey container templates empty while preserving unrelated semantic NBT.
 *
 * <p>Generic stripping is deliberately conservative: only non-empty NBT lists named {@code Items} whose every entry
 * looks like a serialized Minecraft ItemStack are treated as embedded inventory. Known GTNH container families also
 * have their instance/runtime wrapper fields removed explicitly.</p>
 */
public final class EmbeddedInventoryPolicy {

    private static final String LUNCH_BAG = "SpiceOfLife:lunchbag";
    private static final String IC2_TOOLBOX = "IC2:itemToolbox";
    private static final String BACKPACK = "Backpack:backpack";
    private static final String WORKBENCH_BACKPACK = "Backpack:workbenchbackpack";
    private static final int NORMALIZE_MAX_DEPTH = 8;

    private EmbeddedInventoryPolicy() {}

    public static void normalize(ItemStack owner, NBTTagCompound tag) {
        if (tag == null) return;
        normalize(registryId(owner), tag);
    }

    public static void normalize(String registryId, NBTTagCompound tag) {
        if (tag == null) return;
        if (LUNCH_BAG.equals(registryId)) {
            tag.removeTag("Inventory");
            tag.removeTag("Open");
            tag.removeTag("UUID");
        } else if (IC2_TOOLBOX.equals(registryId)) {
            tag.removeTag("Items");
            tag.removeTag("uid");
        } else if (BACKPACK.equals(registryId) || WORKBENCH_BACKPACK.equals(registryId)) {
            // Backpack Edited stores inventory externally under this UUID. A Journey retrieval template must never
            // retain that pointer, otherwise a copied backpack could address the original backpack's external save.
            tag.removeTag("backpack-UID");
        }
        stripSerializedItemLists(tag, 0);
    }

    /** Returns defensive copies of embedded serialized ItemStack compounds, bounded against hostile/deep NBT. */
    public static List<NBTTagCompound> embeddedItemTags(NBTTagCompound root, int maxDepth, int maxEntries) {
        if (root == null || maxDepth < 0 || maxEntries <= 0) return Collections.emptyList();
        List<NBTTagCompound> out = new ArrayList<NBTTagCompound>();
        collect(root, 0, maxDepth, maxEntries, out);
        return Collections.unmodifiableList(out);
    }

    public static boolean isSerializedItemList(NBTTagList list) {
        if (list == null || list.tagCount() <= 0) return false;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry;
            try {
                entry = list.getCompoundTagAt(i);
            } catch (RuntimeException failure) {
                return false;
            }
            if (!looksLikeSerializedItem(entry)) return false;
        }
        return true;
    }

    private static boolean looksLikeSerializedItem(NBTTagCompound entry) {
        if (entry == null) return false;
        boolean hasId = entry.hasKey("id") || entry.hasKey("strId", 8);
        return hasId && entry.hasKey("Count", 99);
    }

    private static void collect(
        NBTTagCompound compound,
        int depth,
        int maxDepth,
        int maxEntries,
        List<NBTTagCompound> out) {
        if (compound == null || depth > maxDepth || out.size() >= maxEntries) return;
        Set<String> keys = compound.func_150296_c();
        if (keys == null || keys.isEmpty()) return;
        for (String key : new ArrayList<String>(keys)) {
            if (out.size() >= maxEntries) return;
            NBTBase child = compound.getTag(key);
            if (child instanceof NBTTagList) {
                NBTTagList list = (NBTTagList) child;
                if ("Items".equals(key) && isSerializedItemList(list)) {
                    for (int i = 0; i < list.tagCount() && out.size() < maxEntries; i++) {
                        NBTTagCompound entry = list.getCompoundTagAt(i);
                        out.add((NBTTagCompound) entry.copy());
                        collect(entry, depth + 1, maxDepth, maxEntries, out);
                    }
                } else if (depth < maxDepth) {
                    collectList(list, depth + 1, maxDepth, maxEntries, out);
                }
            } else if (child instanceof NBTTagCompound && depth < maxDepth) {
                collect((NBTTagCompound) child, depth + 1, maxDepth, maxEntries, out);
            }
        }
    }

    private static void collectList(
        NBTTagList list,
        int depth,
        int maxDepth,
        int maxEntries,
        List<NBTTagCompound> out) {
        if (list == null || depth > maxDepth) return;
        for (int i = 0; i < list.tagCount() && out.size() < maxEntries; i++) {
            try {
                NBTTagCompound entry = list.getCompoundTagAt(i);
                collect(entry, depth, maxDepth, maxEntries, out);
            } catch (RuntimeException ignored) {
                // Non-compound or malformed optional-mod list entries are irrelevant to embedded inventory discovery.
            }
        }
    }

    private static void stripSerializedItemLists(NBTTagCompound compound, int depth) {
        if (compound == null || depth > NORMALIZE_MAX_DEPTH) return;
        Set<String> keys = compound.func_150296_c();
        if (keys == null || keys.isEmpty()) return;
        for (String key : new ArrayList<String>(keys)) {
            NBTBase child = compound.getTag(key);
            if (child instanceof NBTTagList) {
                NBTTagList list = (NBTTagList) child;
                if ("Items".equals(key) && isSerializedItemList(list)) {
                    compound.removeTag(key);
                    continue;
                }
                if (depth < NORMALIZE_MAX_DEPTH) stripInsideList(list, depth + 1);
            } else if (child instanceof NBTTagCompound && depth < NORMALIZE_MAX_DEPTH) {
                stripSerializedItemLists((NBTTagCompound) child, depth + 1);
            }
        }
    }

    private static void stripInsideList(NBTTagList list, int depth) {
        if (list == null || depth > NORMALIZE_MAX_DEPTH) return;
        for (int i = 0; i < list.tagCount(); i++) {
            try {
                stripSerializedItemLists(list.getCompoundTagAt(i), depth);
            } catch (RuntimeException ignored) {}
        }
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
