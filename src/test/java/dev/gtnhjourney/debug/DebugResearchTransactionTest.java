package dev.gtnhjourney.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import dev.gtnhjourney.research.ResearchKey;

public class DebugResearchTransactionTest {

    @Test
    public void oneActionUsesOneBulkMutationAndExactlyOneSyncAndSummary() {
        final Item item = new Item();
        DebugResearchScanService service = serviceWithSingleReadableBlock(item);
        final List<String> events = new ArrayList<String>();

        DebugResearchScanResult result = service.apply(
            DebugResearchMode.BLOCK,
            10,
            20,
            30,
            mutation(events, 2),
            effects(events));

        assertEquals(2, result.getNewlyUnlockedStates());
        assertEquals(
            java.util.Arrays.asList("bulk:Migration BLOCK:1", "sync", "summary:BLOCK:+2"),
            events);
    }

    @Test
    public void area16CreatesSafetySnapshotBeforeItsSingleBulkMutation() {
        final Item item = new Item();
        DebugResearchScanService service = serviceWithSingleReadableBlock(item);
        final List<String> events = new ArrayList<String>();

        DebugResearchScanResult result = service.apply(
            DebugResearchMode.AREA_16,
            10,
            20,
            30,
            mutation(events, 1),
            effects(events));

        assertEquals(4096, result.getPositionsVisited());
        assertEquals(1, result.getNewlyUnlockedStates());
        assertEquals(
            java.util.Arrays.asList(
                "safety:before-migration-area16",
                "bulk:Migration AREA_16:1",
                "sync",
                "summary:AREA_16:+1"),
            events);
    }

    @Test
    public void zeroNewStatesStillProducesOneActionSummaryWithoutExtraMutationCalls() {
        final Item item = new Item();
        DebugResearchScanService service = serviceWithSingleReadableBlock(item);
        final List<String> events = new ArrayList<String>();

        DebugResearchScanResult result = service.apply(
            DebugResearchMode.CONTENTS,
            10,
            20,
            30,
            mutation(events, 0),
            effects(events));

        assertEquals(0, result.getNewlyUnlockedStates());
        assertEquals(
            java.util.Arrays.asList("bulk:Migration CONTENTS:1", "sync", "summary:CONTENTS:+0"),
            events);
    }

    private static DebugResearchScanService serviceWithSingleReadableBlock(final Item item) {
        return new DebugResearchScanService(
            new DebugResearchScanService.WorldAdapter() {

                @Override
                public boolean canRead(int x, int y, int z) {
                    return x == 10 && y == 20 && z == 30;
                }

                @Override
                public ItemStack blockCandidate(int x, int y, int z) {
                    return new ItemStack(item, 1, 0);
                }

                @Override
                public List<ItemStack> inventoryContents(int x, int y, int z) {
                    return Collections.singletonList(new ItemStack(item, 32, 0));
                }
            },
            stack -> new ResearchKey("example:state", 0, ""));
    }

    private static DebugResearchScanService.MutationAdapter mutation(final List<String> events, final int added) {
        return new DebugResearchScanService.MutationAdapter() {

            @Override
            public void createSafetySnapshot(String name) {
                events.add("safety:" + name);
            }

            @Override
            public int applyBulkAdd(List<ItemStack> candidates, String description) {
                events.add("bulk:" + description + ":" + candidates.size());
                return added;
            }
        };
    }

    private static DebugResearchScanService.ActionEffects effects(final List<String> events) {
        return new DebugResearchScanService.ActionEffects() {

            @Override
            public void sync() {
                events.add("sync");
            }

            @Override
            public void summary(DebugResearchMode mode, DebugResearchScanResult result) {
                events.add("summary:" + mode.name() + ":+" + result.getNewlyUnlockedStates());
            }
        };
    }
}
