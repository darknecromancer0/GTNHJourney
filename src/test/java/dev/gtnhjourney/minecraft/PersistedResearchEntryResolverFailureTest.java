package dev.gtnhjourney.minecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class PersistedResearchEntryResolverFailureTest {

    @Test
    public void optionalModLinkageFailureFallsBackInsteadOfEscapingMigration() {
        ResearchKey fallback = new ResearchKey("test:legacy", 7, "");
        ItemStack broken = new ItemStack(new Item(), 1, 7);
        broken.setTagCompound(new LinkageFailingCopyTag());

        PersistedResearchEntryResolver.ResolvedEntry resolved = PersistedResearchEntryResolver
            .resolveReconstructed(fallback, null, broken);

        assertNotNull(resolved);
        assertEquals(fallback, resolved.key());
    }

    @Test
    public void legacyIc2ReBatteryPlaceholderMigratesToRechargeableRegistryIdWithoutOptionalRuntime() {
        PersistedResearchEntryResolver.ResolvedEntry resolved = PersistedResearchEntryResolver
            .resolveEntry("IC2:itemBatREDischarged", 0, "", null);

        assertNotNull(resolved);
        assertEquals("IC2:itemBatRE", resolved.key().getItemId());
    }

    @Test
    public void legacyUntypedVanillaSpawnerIsDroppedInsteadOfBecomingPig() {
        assertNull(PersistedResearchEntryResolver.resolveEntry("minecraft:mob_spawner", 0, "", null));
    }

    private static final class LinkageFailingCopyTag extends NBTTagCompound {

        @Override
        public NBTTagCompound copy() {
            throw new NoClassDefFoundError("missing optional dependency");
        }
    }
}
