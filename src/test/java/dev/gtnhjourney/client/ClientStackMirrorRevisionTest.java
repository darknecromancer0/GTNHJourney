package dev.gtnhjourney.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ClientStackMirrorRevisionTest {

    private static final Item TEST_ITEM = new Item();

    @BeforeAll
    public static void registerTestItem() {
        if (GameRegistry.findUniqueIdentifierFor(TEST_ITEM) == null) {
            GameRegistry.registerItem(TEST_ITEM, "client_stack_revision_test", "gtnhjourney_test");
        }
    }

    @AfterEach
    public void clear() {
        ClientStackMirror.clear();
    }

    @Test
    public void repeatedServerTemplateForSameIdentityStillAdvancesStackRevision() {
        ClientStackMirror.clear();
        long beforeStack = ClientStackMirror.revision();
        long beforeResearch = ClientResearchMirror.revision();

        ClientStackMirror.addUnlock(new ItemStack(TEST_ITEM));
        long afterFirstStack = ClientStackMirror.revision();
        long afterFirstResearch = ClientResearchMirror.revision();
        assertTrue(afterFirstStack > beforeStack);
        assertTrue(afterFirstResearch > beforeResearch);
        assertEquals(1, ClientStackMirror.snapshot().size());
        assertEquals(1, ClientStackMirror.serverAvailableTotal());

        // Models a CropsNH seed whose canonical identity is unchanged while its stored best-stat template improves.
        ClientStackMirror.addUnlock(new ItemStack(TEST_ITEM));
        long afterSameKeyStack = ClientStackMirror.revision();
        long afterSameKeyResearch = ClientResearchMirror.revision();

        assertTrue(afterSameKeyStack > afterFirstStack);
        assertTrue(afterSameKeyResearch > afterFirstResearch);
        assertEquals(1, ClientStackMirror.snapshot().size());
        assertEquals(1, ClientStackMirror.serverAvailableTotal());
    }
}
