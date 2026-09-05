package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

public class JourneyEmptySearchPolicyTest {

    @Test
    public void ordinaryQueriesDoNotChangeNativeSearchResult() {
        ItemStack water = new ItemStack(Items.water_bucket);
        assertTrue(JourneyEmptySearchPolicy.resolveSearchMatch("water", water, true));
        assertFalse(JourneyEmptySearchPolicy.resolveSearchMatch("water", water, false));
    }

    @Test
    public void emptyQueryRejectsFilledFluidContainerTooltipFalsePositive() {
        ItemStack water = new ItemStack(Items.water_bucket);
        assertFalse(JourneyEmptySearchPolicy.resolveSearchMatch("empty", water, true));
        assertFalse(JourneyEmptySearchPolicy.resolveSearchMatch("\"empty\"", water, true));
    }

    @Test
    public void emptyQueryAddsHiddenAliasToActuallyEmptyFluidContainers() {
        ItemStack emptyBucket = new ItemStack(Items.bucket);
        assertTrue(JourneyEmptySearchPolicy.resolveSearchMatch("empty", emptyBucket, false));
        assertTrue(JourneyEmptySearchPolicy.resolveSearchMatch("\"empty\"", emptyBucket, false));
    }

    @Test
    public void prefixedEmptySearchRemainsCompletelyNative() {
        ItemStack emptyBucket = new ItemStack(Items.bucket);
        assertFalse(JourneyEmptySearchPolicy.resolveSearchMatch("@empty", emptyBucket, false));
        assertTrue(JourneyEmptySearchPolicy.resolveSearchMatch("@empty", emptyBucket, true));
        assertFalse(JourneyEmptySearchPolicy.resolveSearchMatch("#empty", emptyBucket, false));
    }
}
