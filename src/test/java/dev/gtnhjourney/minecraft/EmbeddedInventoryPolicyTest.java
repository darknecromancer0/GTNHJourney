package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class EmbeddedInventoryPolicyTest {

    @Test
    public void lunchBagBecomesEmptyButUnrelatedStateSurvives() {
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagCompound inventory = new NBTTagCompound();
        inventory.setTag("Items", itemList(391, 4));
        tag.setTag("Inventory", inventory);
        tag.setBoolean("Open", true);
        tag.setString("UUID", "bag-instance");
        tag.setString("CustomLabel", "Lunch");

        EmbeddedInventoryPolicy.normalize("SpiceOfLife:lunchbag", tag);

        assertFalse(tag.hasKey("Inventory"));
        assertFalse(tag.hasKey("Open"));
        assertFalse(tag.hasKey("UUID"));
        assertEquals("Lunch", tag.getString("CustomLabel"));
    }

    @Test
    public void toolboxBecomesEmptyButNameAndRepairCostSurvive() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("Items", itemList(7442, 1));
        tag.setInteger("uid", 12345);
        tag.setInteger("RepairCost", 2);
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", "Main Tool Box");
        tag.setTag("display", display);

        EmbeddedInventoryPolicy.normalize("IC2:itemToolbox", tag);

        assertFalse(tag.hasKey("Items"));
        assertFalse(tag.hasKey("uid"));
        assertEquals(2, tag.getInteger("RepairCost"));
        assertEquals("Main Tool Box", tag.getCompoundTag("display").getString("Name"));
    }

    @Test
    public void genericSerializedItemsListIsStrippedButLookalikeListIsPreserved() {
        NBTTagCompound real = new NBTTagCompound();
        real.setTag("Items", itemList(391, 1));
        real.setString("Mode", "keep");
        EmbeddedInventoryPolicy.normalize("test:portable", real);
        assertFalse(real.hasKey("Items"));
        assertEquals("keep", real.getString("Mode"));

        NBTTagCompound lookalike = new NBTTagCompound();
        NBTTagList arbitrary = new NBTTagList();
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString("name", "not-an-item-stack");
        arbitrary.appendTag(entry);
        lookalike.setTag("Items", arbitrary);
        EmbeddedInventoryPolicy.normalize("test:configuration", lookalike);
        assertTrue(lookalike.hasKey("Items"));
    }

    @Test
    public void embeddedScannerFindsNestedSerializedItemCompoundsWithoutMutatingOwnerTag() {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound inventory = new NBTTagCompound();
        inventory.setTag("Items", itemList(391, 4));
        root.setTag("Inventory", inventory);
        root.setString("UUID", "still-here");

        List<NBTTagCompound> found = EmbeddedInventoryPolicy.embeddedItemTags(root, 4, 64);

        assertEquals(1, found.size());
        assertEquals(391, found.get(0).getShort("id"));
        assertEquals(4, found.get(0).getByte("Count"));
        assertTrue(root.hasKey("Inventory"));
        assertEquals("still-here", root.getString("UUID"));
    }

    private static NBTTagList itemList(int numericId, int count) {
        NBTTagList list = new NBTTagList();
        NBTTagCompound stack = new NBTTagCompound();
        stack.setShort("id", (short) numericId);
        stack.setByte("Count", (byte) count);
        stack.setShort("Damage", (short) 0);
        stack.setByte("Slot", (byte) 0);
        list.appendTag(stack);
        return list;
    }
}
