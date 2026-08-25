package dev.gtnhjourney.recovery;

public final class JourneySnapshotTicker {

    private JourneySnapshotTicker() {}

    public static boolean isCadenceTick(long worldTick) {
        return worldTick > 0L && worldTick % JourneySnapshotService.AUTO_INTERVAL_TICKS == 0L;
    }
}
