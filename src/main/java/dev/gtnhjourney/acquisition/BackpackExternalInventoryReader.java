package dev.gtnhjourney.acquisition;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import cpw.mods.fml.common.registry.GameRegistry;
import dev.gtnhjourney.minecraft.EmbeddedInventoryPolicy;

/** Optional read-only bridge for Backpack Edited, whose actual inventory is stored outside the ItemStack NBT. */
public final class BackpackExternalInventoryReader {

    private static final String BACKPACK = "Backpack:backpack";
    private static final String WORKBENCH_BACKPACK = "Backpack:workbenchbackpack";
    private static final String UID = "backpack-UID";
    private static final String INVENTORIES = "backpackInventories";
    private static final String[] OWNED_INVENTORIES = { "backpack", "craftingGrid" };

    private BackpackExternalInventoryReader() {}

    /**
     * Reads a Backpack Edited external save through reflection so GTNH Journey remains optional-mod safe.
     * The returned compounds are defensive copies and the external save is never modified.
     */
    public static List<NBTTagCompound> serializedStacks(ItemStack stack, int maxEntries) {
        if (!isSupportedBackpack(stack) || maxEntries <= 0 || !stack.hasTagCompound()) {
            return Collections.emptyList();
        }
        String uuid = stack.getTagCompound().getString(UID);
        if (uuid == null || uuid.length() == 0) return Collections.emptyList();

        try {
            Class<?> backpackClass = Class.forName("de.eydamos.backpack.Backpack");
            Field handlerField = backpackClass.getField("saveFileHandler");
            Object handler = handlerField.get(null);
            if (handler == null) return Collections.emptyList();
            Method loadBackpack = handler.getClass().getMethod("loadBackpack", String.class);
            Object loaded = loadBackpack.invoke(handler, uuid);
            if (!(loaded instanceof NBTTagCompound)) return Collections.emptyList();
            return serializedStacksFromSave((NBTTagCompound) loaded, maxEntries);
        } catch (ReflectiveOperationException ignored) {
            return Collections.emptyList();
        } catch (RuntimeException ignored) {
            return Collections.emptyList();
        } catch (LinkageError ignored) {
            return Collections.emptyList();
        }
    }

    /** Pure decoder kept separate so the external Backpack format is regression-testable without loading the mod. */
    static List<NBTTagCompound> serializedStacksFromSave(NBTTagCompound save, int maxEntries) {
        if (save == null || maxEntries <= 0 || !save.hasKey(INVENTORIES, 10)) return Collections.emptyList();
        NBTTagCompound inventories = save.getCompoundTag(INVENTORIES);
        List<NBTTagCompound> out = new ArrayList<NBTTagCompound>();
        for (String inventoryName : OWNED_INVENTORIES) {
            if (out.size() >= maxEntries || !inventories.hasKey(inventoryName, 9)) continue;
            NBTTagList list = inventories.getTagList(inventoryName, 10);
            if (!EmbeddedInventoryPolicy.isSerializedItemList(list)) continue;
            for (int i = 0; i < list.tagCount() && out.size() < maxEntries; i++) {
                out.add((NBTTagCompound) list.getCompoundTagAt(i).copy());
            }
        }
        return Collections.unmodifiableList(out);
    }

    public static String externalInstanceId(ItemStack stack) {
        if (!isSupportedBackpack(stack) || !stack.hasTagCompound()) return null;
        String uuid = stack.getTagCompound().getString(UID);
        return uuid == null || uuid.length() == 0 ? null : uuid;
    }

    private static boolean isSupportedBackpack(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        try {
            GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(stack.getItem());
            if (id == null) return false;
            String registryId = id.toString();
            return BACKPACK.equals(registryId) || WORKBENCH_BACKPACK.equals(registryId);
        } catch (RuntimeException ignored) {
            return false;
        } catch (LinkageError ignored) {
            return false;
        }
    }
}
