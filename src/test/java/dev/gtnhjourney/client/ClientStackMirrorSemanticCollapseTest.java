package dev.gtnhjourney.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class ClientStackMirrorSemanticCollapseTest {

    @AfterEach
    public void clear() {
        ClientStackMirror.clear();
    }

    @Test
    public void completeTransportMayPublishWhenSemanticKeysCollapse() {
        ClientStackMirror.clear();
        ItemStack first = new ItemStack(Items.stick, 1, 0);
        ItemStack equivalent = new ItemStack(Items.stick, 1, 0);

        ClientStackMirror.begin(42, 2, 2);
        ClientStackMirror.addChunk(42, Arrays.asList(first, equivalent));

        assertTrue(ClientStackMirror.finish(42));
        assertEquals(1, ClientStackMirror.snapshot().size());
        assertEquals(1, ClientResearchMirror.size());
        assertEquals(2, ClientStackMirror.expectedSyncedTotal());
    }
}
