package dev.gtnhjourney.safety;

/**
 * Separately gates vanilla Enderman block pickup and placement without changing the global mobGriefing gamerule.
 * The caller must still respect vanilla's mobGriefing rule when this Journey feature is enabled.
 */
public final class EndermanBlockCarryPolicy {

    private EndermanBlockCarryPolicy() {}

    public static boolean mayMoveBlocks(boolean vanillaMobGriefing, boolean enabled) {
        return enabled && vanillaMobGriefing;
    }
}
