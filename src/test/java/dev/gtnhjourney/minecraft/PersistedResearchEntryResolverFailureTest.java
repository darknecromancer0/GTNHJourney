package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class PersistedResearchEntryResolverFailureTest {

    @Test
    public void optionalModLinkageFailureFallsBackInsteadOfEscapingMigration() {
        ResearchKey fallback = new ResearchKey("test:legacy", 7, "");
        ItemStack broken = new ItemStack(Items.stick, 1, 7);
        broken.setTagCompound(new LinkageFailingCopyTag());

        PersistedResearchEntryResolver.ResolvedEntry resolved = PersistedResearchEntryResolver
            .resolveReconstructed(fallback, null, broken);

        assertNotNull(resolved);
        assertEquals(fallback, resolved.key());
    }

    private static final class LinkageFailingCopyTag extends NBTTagCompound {

        @Override
        public NBTTagCompound copy() {
            throw new NoClassDefFoundError("missing optional dependency");
        }
    }
}
