package dev.gtnhjourney.client;

/** Small policy seam for repairing a leaked shared Tessellator batch before another mod starts drawing. */
public final class RenderBoundaryRecovery {

    private RenderBoundaryRecovery() {}

    public static boolean finishDanglingBatch(boolean drawing, Runnable finish) {
        if (!drawing) return false;
        finish.run();
        return true;
    }
}
