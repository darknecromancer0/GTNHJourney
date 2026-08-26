package dev.gtnhjourney.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class BackpackExternalInventoryReaderTest {

    @Test
    public void extractsSerializedStacksFromBackpackExternalSaveWithoutMutatingIt() {
        NBTTagCompound save = new NBTTagCompound();
        NBTTagCompound inventories = new NBTTagCompound();
        NBTTagList backpack = new NBTTagList();
        backpack.appendTag(serialized(391, 4, 0));
        backpack.appendTag(serialized(297, 2, 1));
        inventories.setTag("backpack", backpack);
        save.setTag("backpackInventories", inventories);

        List<NBTTagCompound> found = BackpackExternalInventoryReader.serializedStacksFromSave(save, 64);

        assertEquals(2, found.size());
        assertEquals(391, found.get(0).getShort("id"));
        assertEquals(4, found.get(0).getByte("Count"));
        assertEquals(297, found.get(1).getShort("id"));
        assertEquals(2, found.get(1).getByte("Count"));
        assertEquals(2, save.getCompoundTag("backpackInventories").getTagList("backpack", 10).tagCount());
    }

    @Test
    public void extractionIsBoundedAndIgnoresNonItemLists() {
        NBTTagCompound save = new NBTTagCompound();
        NBTTagCompound inventories = new NBTTagCompound();
        NBTTagList backpack = new NBTTagList();
        backpack.appendTag(serialized(391, 1, 0));
        backpack.appendTag(serialized(297, 1, 1));
        backpack.appendTag(serialized(50, 1, 2));
        inventories.setTag("backpack", backpack);

        NBTTagList recipeMetadata = new NBTTagList();
        NBTTagCompound notAnItem = new NBTTagCompound();
        notAnItem.setString("name", "metadata");
        recipeMetadata.appendTag(notAnItem);
        inventories.setTag("recipes", recipeMetadata);
        save.setTag("backpackInventories", inventories);

        List<NBTTagCompound> found = BackpackExternalInventoryReader.serializedStacksFromSave(save, 2);

        assertEquals(2, found.size());
    }

    private static NBTTagCompound serialized(int id, int count, int slot) {
        NBTTagCompound stack = new NBTTagCompound();
        stack.setShort("id", (short) id);
        stack.setByte("Count", (byte) count);
        stack.setShort("Damage", (short) 0);
        stack.setByte("slot", (byte) slot);
        return stack;
    }
}
