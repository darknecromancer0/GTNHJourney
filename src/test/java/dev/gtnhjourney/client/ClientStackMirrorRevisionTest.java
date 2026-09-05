package dev.gtnhjourney.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class ClientStackMirrorRevisionTest {

    @AfterEach
    public void clear() {
        ClientStackMirror.clear();
    }

    @Test
    public void repeatedServerTemplateForSameIdentityStillAdvancesStackRevision() {
        ClientStackMirror.clear();
        long before = ClientStackMirror.revision();

        ClientStackMirror.addUnlock(new ItemStack(Items.apple));
        long afterFirst = ClientStackMirror.revision();
        assertTrue(afterFirst > before);
        assertEquals(1, ClientStackMirror.snapshot().size());

        // Models a CropsNH seed whose canonical identity is unchanged while its stored best-stat template improves.
        ClientStackMirror.addUnlock(new ItemStack(Items.apple));
        long afterSameKeyUpdate = ClientStackMirror.revision();

        assertTrue(afterSameKeyUpdate > afterFirst);
        assertEquals(1, ClientStackMirror.snapshot().size());
    }
}
