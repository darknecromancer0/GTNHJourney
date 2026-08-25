package dev.gtnhjourney.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class DebugResearchToolStateTest {

    @Test
    public void modeCycleIsExact() {
        assertEquals(DebugResearchMode.CONTENTS, DebugResearchMode.BLOCK.next());
        assertEquals(DebugResearchMode.AREA_16, DebugResearchMode.CONTENTS.next());
        assertEquals(DebugResearchMode.BLOCK, DebugResearchMode.AREA_16.next());
    }

    @Test
    public void absentOrInvalidModeDefaultsToBlock() {
        ItemStack stack = new ItemStack(new Item());
        assertEquals(DebugResearchMode.BLOCK, DebugResearchToolState.read(stack));

        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("GTNHJourneyDebugResearchMode", "NOPE");
        stack.setTagCompound(tag);
        assertEquals(DebugResearchMode.BLOCK, DebugResearchToolState.read(stack));
    }

    @Test
    public void writeRoundTripsAndPreservesUnrelatedNbt() {
        ItemStack stack = new ItemStack(new Item());
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("Unrelated", "keep-me");
        stack.setTagCompound(tag);

        DebugResearchToolState.write(stack, DebugResearchMode.CONTENTS);
        assertEquals(DebugResearchMode.CONTENTS, DebugResearchToolState.read(stack));
        assertEquals("keep-me", stack.getTagCompound().getString("Unrelated"));

        DebugResearchToolState.write(stack, DebugResearchMode.AREA_16);
        assertEquals(DebugResearchMode.AREA_16, DebugResearchToolState.read(stack));
        assertEquals("keep-me", stack.getTagCompound().getString("Unrelated"));
    }
}
