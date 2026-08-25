package dev.gtnhjourney.acquisition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

public class ResearchObservationResultTest {

    @Test
    public void zeroEndpointsDoesNotCreateLogicalUnlock() {
        ResearchObservationResult result = ResearchObservationResult.of(Collections.<ItemStack>emptyList());

        assertFalse(result.isNewLogicalUnlock());
        assertEquals(0, result.endpointCount());
        assertEquals(0, result.notificationCount());
    }

    @Test
    public void oneEndpointCreatesOneLogicalNotification() {
        ResearchObservationResult result = ResearchObservationResult.of(
            Collections.singletonList(new ItemStack(new Item(), 1, 0)));

        assertTrue(result.isNewLogicalUnlock());
        assertEquals(1, result.endpointCount());
        assertEquals(1, result.notificationCount());
    }

    @Test
    public void multipleEndpointsStillCreateOneLogicalNotification() {
        ResearchObservationResult result = ResearchObservationResult.of(
            Arrays.asList(new ItemStack(new Item(), 1, 0), new ItemStack(new Item(), 1, 1)));

        assertTrue(result.isNewLogicalUnlock());
        assertEquals(2, result.endpointCount());
        assertEquals(1, result.notificationCount());
    }
}
