package dev.gtnhjourney.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

public class MainInventoryFillServiceTest {

    @Test
    public void fillsOnlyEmptyMainSlotsWithNaturalMaxStacks() {
        ItemStack[] slots = new ItemStack[5];
        slots[1] = new ItemStack(Items.stick, 7);
        slots[3] = new ItemStack(Items.iron_ingot, 2);
        ItemStack template = new ItemStack(Items.apple, 1);

        int filled = MainInventoryFillService.fillEmptySlots(slots, template, 64);

        assertEquals(3, filled);
        assertEquals(64, slots[0].stackSize);
        assertEquals(7, slots[1].stackSize);
        assertEquals(64, slots[2].stackSize);
        assertEquals(2, slots[3].stackSize);
        assertEquals(64, slots[4].stackSize);
    }

    @Test
    public void nonStackableItemsFillOnePerEmptySlot() {
        ItemStack[] slots = new ItemStack[3];
        slots[1] = new ItemStack(Items.stick, 1);

        int filled = MainInventoryFillService.fillEmptySlots(slots, new ItemStack(Items.iron_sword, 1), 64);

        assertEquals(2, filled);
        assertEquals(1, slots[0].stackSize);
        assertEquals(1, slots[2].stackSize);
    }

    @Test
    public void exactTemplateNbtIsCopiedIndependentlyIntoEverySlot() {
        ItemStack[] slots = new ItemStack[2];
        ItemStack template = new ItemStack(Items.apple, 1);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("JourneyExactState", "A");
        template.setTagCompound(tag);

        assertEquals(2, MainInventoryFillService.fillEmptySlots(slots, template, 64));
        assertNotNull(slots[0].getTagCompound());
        assertNotNull(slots[1].getTagCompound());
        assertEquals("A", slots[0].getTagCompound().getString("JourneyExactState"));
        assertEquals("A", slots[1].getTagCompound().getString("JourneyExactState"));

        slots[0].getTagCompound().setString("JourneyExactState", "mutated");
        assertEquals("A", slots[1].getTagCompound().getString("JourneyExactState"));
        assertEquals("A", template.getTagCompound().getString("JourneyExactState"));
    }

    @Test
    public void nullTemplateDoesNothing() {
        ItemStack[] slots = new ItemStack[2];

        assertEquals(0, MainInventoryFillService.fillEmptySlots(slots, null, 64));
        assertNull(slots[0]);
        assertNull(slots[1]);
    }
}
