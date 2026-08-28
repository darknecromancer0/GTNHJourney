package dev.gtnhjourney.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.minecraft.NbtCanonicalizer;
import dev.gtnhjourney.recovery.DeletionRecord;
import dev.gtnhjourney.recovery.ResearchEntrySnapshot;
import dev.gtnhjourney.recovery.ResearchTransaction;
import dev.gtnhjourney.research.ResearchKey;

public class JourneyRecoveryDataTest {

    @Test
    public void undoAndRedoAreLifoAndBoundedToOneHundred() {
        UUID player = UUID.randomUUID();
        JourneyRecoveryData data = new JourneyRecoveryData();

        for (int i = 1; i <= 105; i++) data.pushUndo(player, tx(i));
        assertEquals(100, data.undoDepth(player));
        assertEquals(105L, data.popUndo(player).id());
        assertEquals(104L, data.popUndo(player).id());

        for (int i = 1; i <= 105; i++) data.pushRedo(player, tx(i));
        assertEquals(100, data.redoDepth(player));
        assertEquals(105L, data.popRedo(player).id());
        data.clearRedo(player);
        assertEquals(0, data.redoDepth(player));
        assertNull(data.popRedo(player));
    }

    @Test
    public void historiesSurviveNbtRoundTrip() {
        UUID player = UUID.randomUUID();
        JourneyRecoveryData original = new JourneyRecoveryData();
        original.pushUndo(player, tx(7));
        original.pushUndo(player, tx(8));
        original.pushRedo(player, tx(3));

        NBTTagCompound root = new NBTTagCompound();
        original.writeToNBT(root);

        JourneyRecoveryData restored = new JourneyRecoveryData();
        restored.readFromNBT(root);

        assertEquals(2, restored.undoDepth(player));
        assertEquals(1, restored.redoDepth(player));
        assertEquals(8L, restored.popUndo(player).id());
        assertEquals(3L, restored.popRedo(player).id());
    }

    @Test
    public void recoveryEntriesAreRecanonicalizedAndSemanticDuplicatesCollapseOnLoad() {
        UUID player = UUID.randomUUID();
        ResearchEntrySnapshot chiselOne = legacyChiselEntry("minecraft:stone", 4);
        ResearchEntrySnapshot chiselTwo = legacyChiselEntry("minecraft:dirt", 5);
        JourneyRecoveryData original = new JourneyRecoveryData();
        original.pushUndo(
            player,
            new ResearchTransaction(
                20L,
                200L,
                "legacy duplicate chisel target",
                Arrays.asList(chiselOne, chiselTwo),
                Collections.<ResearchEntrySnapshot>emptyList()));
        original.appendDeletion(player, new DeletionRecord(7L, 210L, chiselOne, true));

        NBTTagCompound root = new NBTTagCompound();
        original.writeToNBT(root);

        JourneyRecoveryData restored = new JourneyRecoveryData();
        restored.readFromNBT(root);
        ResearchTransaction transaction = restored.popUndo(player);

        assertEquals(1, transaction.added().size());
        assertEquals("chisel:chisel", transaction.added().get(0).key().getItemId());
        assertEquals("", transaction.added().get(0).key().getCanonicalNbt());
        assertNull(transaction.added().get(0).template());
        assertEquals(4, transaction.added().get(0).timelineIndex());
        assertEquals(1, restored.deletions(player).size());
        assertEquals("", restored.deletions(player).get(0).entry().key().getCanonicalNbt());
        assertNull(restored.deletions(player).get(0).entry().template());
    }

    private static ResearchTransaction tx(long id) {
        return new ResearchTransaction(
            id,
            id * 10L,
            "tx-" + id,
            Collections.emptyList(),
            Collections.emptyList());
    }

    private static ResearchEntrySnapshot legacyChiselEntry(String targetId, int timelineIndex) {
        NBTTagCompound target = new NBTTagCompound();
        target.setString("id", targetId);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("chiselTarget", target);
        return new ResearchEntrySnapshot(
            new ResearchKey("chisel:chisel", 0, NbtCanonicalizer.canonicalize(tag)),
            tag,
            timelineIndex);
    }
}
