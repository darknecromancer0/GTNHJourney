package dev.gtnhjourney.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.jupiter.api.Test;

import dev.gtnhjourney.recovery.ResearchTransaction;

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

    private static ResearchTransaction tx(long id) {
        return new ResearchTransaction(
            id,
            id * 10L,
            "tx-" + id,
            Collections.emptyList(),
            Collections.emptyList());
    }
}
