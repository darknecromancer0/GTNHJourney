package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class UnstableIngotStatePolicyTest {

    @Test
    public void tickingRuntimeFieldsDoNotParticipateInUnstableIngotIdentity() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("time", 123456L);
        tag.setInteger("dimension", -1);
        tag.setBoolean("creative", false);
        tag.setString("marker", "keep");

        UnstableIngotStatePolicy.normalizeIdentity("ExtraUtilities:unstableingot", 0, tag);

        assertFalse(tag.hasKey("time"));
        assertFalse(tag.hasKey("dimension"));
        assertTrue(tag.hasKey("creative"));
        assertEquals("keep", tag.getString("marker"));
    }

    @Test
    public void stableSiblingMetasRemainExact() {
        NBTTagCompound nugget = timerTag();
        NBTTagCompound mobius = timerTag();

        UnstableIngotStatePolicy.normalizeIdentity("ExtraUtilities:unstableingot", 1, nugget);
        UnstableIngotStatePolicy.normalizeIdentity("ExtraUtilities:unstableingot", 2, mobius);

        assertTrue(nugget.hasKey("time"));
        assertTrue(nugget.hasKey("dimension"));
        assertTrue(mobius.hasKey("time"));
        assertTrue(mobius.hasKey("dimension"));
    }

    @Test
    public void ordinaryRetrievalGetsFreshTimerWithoutLosingOtherState() {
        ItemStack stack = new ItemStack(new Item(), 1, 0);
        NBTTagCompound tag = timerTag();
        tag.setString("marker", "keep");
        stack.setTagCompound(tag);

        ItemStack refreshed = UnstableIngotStatePolicy
            .refreshForRetrieval(stack, "ExtraUtilities:unstableingot", 0, 987654L, 7);

        assertSame(stack, refreshed);
        assertEquals(987654L, stack.getTagCompound().getLong("time"));
        assertEquals(7, stack.getTagCompound().getInteger("dimension"));
        assertEquals("keep", stack.getTagCompound().getString("marker"));
    }

    @Test
    public void creativeUnstableIngotDoesNotGainExplosiveTimer() {
        ItemStack stack = new ItemStack(new Item(), 1, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setBoolean("creative", true);
        stack.setTagCompound(tag);

        UnstableIngotStatePolicy
            .refreshForRetrieval(stack, "ExtraUtilities:unstableingot", 0, 987654L, 7);

        assertTrue(stack.getTagCompound().getBoolean("creative"));
        assertFalse(stack.getTagCompound().hasKey("time"));
        assertFalse(stack.getTagCompound().hasKey("dimension"));
    }

    private static NBTTagCompound timerTag() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setLong("time", 123456L);
        tag.setInteger("dimension", -1);
        return tag;
    }
}
