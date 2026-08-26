package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class PersistedResearchEntryResolverFailureTest {

    @Test
    public void optionalModLinkageFailureFallsBackInsteadOfEscapingMigration() {
        ResearchKey fallback = new ResearchKey("test:legacy", 7, "");
        ItemStack broken = new ItemStack(new Item(), 1, 7) {

            @Override
            public Item getItem() {
                throw new NoClassDefFoundError("missing optional dependency");
            }
        };

        PersistedResearchEntryResolver.ResolvedEntry resolved = PersistedResearchEntryResolver
            .resolveReconstructed(fallback, null, broken);

        assertNotNull(resolved);
        assertEquals(fallback, resolved.key());
    }
}
