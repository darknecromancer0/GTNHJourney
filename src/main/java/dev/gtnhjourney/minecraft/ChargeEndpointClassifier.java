package dev.gtnhjourney.minecraft;

/** Pure endpoint classifier shared by integer-backed charge adapters such as CoFH RF items. */
public final class ChargeEndpointClassifier {

    public enum State {
        EXACT,
        BASE,
        FULL
    }

    private ChargeEndpointClassifier() {}

    public static State classify(int current, int max, boolean positiveChargeExtractable) {
        if (max <= 0 || current < 0) return State.EXACT;
        if (current > 0 && !positiveChargeExtractable) return State.EXACT;
        return current >= max ? State.FULL : State.BASE;
    }
}
