package dev.gtnhjourney.backup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cpw.mods.fml.common.gameevent.TickEvent;
import org.junit.jupiter.api.Test;

class WorldBackupTickerTest {

    @Test
    void automaticWorkOnlyStartsAtEndPhaseWhenDue() {
        assertFalse(WorldBackupTicker.shouldRun(TickEvent.Phase.START, true));
        assertFalse(WorldBackupTicker.shouldRun(TickEvent.Phase.END, false));
        assertTrue(WorldBackupTicker.shouldRun(TickEvent.Phase.END, true));
    }

    @Test
    void workerCompletionIsPolledOnlyFromServerEndTick() {
        assertFalse(WorldBackupTicker.shouldPollCompletion(TickEvent.Phase.START));
        assertTrue(WorldBackupTicker.shouldPollCompletion(TickEvent.Phase.END));
    }
}
