package dev.gtnhjourney.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import dev.gtnhjourney.research.ResearchKey;

public class ResearchCandidateDeduplicatorTest {

    @Test
    public void collapsesSameSemanticIdentityButPreservesDistinctNbtStatesAndSkipsBrokenCandidates() {
        Item common = new Item();
        ItemStack stoneLarge = new ItemStack(common, 64, 0);
        ItemStack stoneSmall = new ItemStack(common, 1, 0);
        ItemStack water = tagged(common, "water");
        ItemStack lava = tagged(common, "lava");
        ItemStack broken = new ItemStack(common, 1, 99);

        List<ItemStack> unique = ResearchCandidateDeduplicator.deduplicate(
            Arrays.asList(stoneLarge, stoneSmall, water, lava, broken),
            new ResearchCandidateDeduplicator.IdentityResolver() {

                @Override
                public ResearchKey identity(ItemStack stack) {
                    if (stack.getItemDamage() == 99) throw new IllegalArgumentException("broken optional integration");
                    if (!stack.hasTagCompound()) return new ResearchKey("minecraft:stone", 0, "");
                    String fluid = stack.getTagCompound().getString("fluid");
                    return new ResearchKey("example:tank", 0, "fluid=" + fluid);
                }
            });

        assertEquals(3, unique.size());
        assertNotSame(stoneLarge, unique.get(0));
        assertEquals(64, unique.get(0).stackSize);
        assertEquals("water", unique.get(1).getTagCompound().getString("fluid"));
        assertEquals("lava", unique.get(2).getTagCompound().getString("fluid"));
    }

    @Test
    public void linkageFailureOnOneOptionalCandidateDoesNotAbortTheMigrationBatch() {
        Item common = new Item();
        ItemStack broken = new ItemStack(common, 1, 99);
        ItemStack valid = new ItemStack(common, 1, 0);

        List<ItemStack> unique = ResearchCandidateDeduplicator.deduplicate(
            Arrays.asList(broken, valid),
            new ResearchCandidateDeduplicator.IdentityResolver() {

                @Override
                public ResearchKey identity(ItemStack stack) {
                    if (stack.getItemDamage() == 99) throw new NoClassDefFoundError("missing optional integration");
                    return new ResearchKey("minecraft:stone", 0, "");
                }
            });

        assertEquals(1, unique.size());
        assertEquals(0, unique.get(0).getItemDamage());
    }

    @Test
    public void scanResultTracksRequiredMigrationMetricsAndCanAttachMutationCount() {
        ItemStack candidate = new ItemStack(new Item(), 1, 0);
        DebugResearchScanResult scanned = new DebugResearchScanResult(
            Arrays.asList(candidate),
            4096,
            12,
            4,
            19,
            0);

        assertEquals(4096, scanned.getPositionsVisited());
        assertEquals(12, scanned.getBlockCandidates());
        assertEquals(4, scanned.getInventoriesVisited());
        assertEquals(19, scanned.getRawStacks());
        assertEquals(1, scanned.getUniqueCandidates());
        assertEquals(0, scanned.getNewlyUnlockedStates());

        DebugResearchScanResult applied = scanned.withNewlyUnlockedStates(7);
        assertEquals(7, applied.getNewlyUnlockedStates());
        assertEquals(1, applied.getCandidates().size());
        assertNotSame(candidate, applied.getCandidates().get(0));
    }

    private static ItemStack tagged(Item item, String fluid) {
        ItemStack stack = new ItemStack(item, 1, 0);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("fluid", fluid);
        stack.setTagCompound(tag);
        return stack;
    }
}
