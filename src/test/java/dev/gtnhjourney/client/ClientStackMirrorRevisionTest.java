package dev.gtnhjourney.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import dev.gtnhjourney.research.ResearchKey;

public class ClientStackMirrorRevisionTest {

    private static final Item TEST_ITEM = new Item();
    private static final ResearchKey TEST_KEY = new ResearchKey("gtnhjourney_test:seed", 0, "species");

    @AfterEach
    public void clear() {
        ClientStackMirror.clear();
    }

    @Test
    public void repeatedServerTemplateForSameIdentityStillAdvancesStackRevision() {
        ClientStackMirror.clear();
        long beforeStack = ClientStackMirror.revision();
        long beforeResearch = ClientResearchMirror.revision();

        ClientStackMirror.addUnlock(TEST_KEY, seed(5));
        long afterFirstStack = ClientStackMirror.revision();
        long afterFirstResearch = ClientResearchMirror.revision();
        assertTrue(afterFirstStack > beforeStack);
        assertTrue(afterFirstResearch > beforeResearch);
        assertEquals(1, ClientStackMirror.snapshot().size());
        assertEquals(1, ClientStackMirror.serverAvailableTotal());
        assertEquals(5, ClientStackMirror.template(TEST_KEY).getTagCompound().getInteger("re"));

        // Models a CropsNH seed whose canonical identity is unchanged while its stored best-stat template improves.
        ClientStackMirror.addUnlock(TEST_KEY, seed(6));
        long afterSameKeyStack = ClientStackMirror.revision();
        long afterSameKeyResearch = ClientResearchMirror.revision();

        assertTrue(afterSameKeyStack > afterFirstStack);
        assertTrue(afterSameKeyResearch > afterFirstResearch);
        assertEquals(1, ClientStackMirror.snapshot().size());
        assertEquals(1, ClientStackMirror.serverAvailableTotal());
        assertEquals(6, ClientStackMirror.template(TEST_KEY).getTagCompound().getInteger("re"));
    }

    private static ItemStack seed(int resistance) {
        ItemStack stack = new ItemStack(TEST_ITEM);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("re", resistance);
        stack.setTagCompound(tag);
        return stack;
    }
}
