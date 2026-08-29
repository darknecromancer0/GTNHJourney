package dev.gtnhjourney.time;

/** Narrow boundary between Journey speed state and the transformed Minecraft server loop. */
public interface ServerTickRateAdapter {

    boolean isSupported();

    boolean applyMultiplier(int multiplier);

    void reset();
}
