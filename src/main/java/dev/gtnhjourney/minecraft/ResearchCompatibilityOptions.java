package dev.gtnhjourney.minecraft;

/** Runtime switches for narrowly targeted compatibility rules that can be disabled without rebuilding the mod. */
public final class ResearchCompatibilityOptions {

    private static volatile boolean normalizeGtTransientIdentity = true;
    private static volatile boolean resetGtToolTemplateState = true;
    private static volatile boolean normalizeGtChargeEndpoints = true;
    private static volatile boolean normalizeIc2ChargeEndpoints = true;
    private static volatile boolean normalizeTconToolWear = true;
    private static volatile boolean normalizeCofhChargeEndpoints = true;

    private ResearchCompatibilityOptions() {}

    public static boolean normalizeGtTransientIdentity() {
        return normalizeGtTransientIdentity;
    }

    public static boolean resetGtToolTemplateState() {
        return resetGtToolTemplateState;
    }

    public static boolean normalizeGtChargeEndpoints() {
        return normalizeGtChargeEndpoints;
    }

    public static boolean normalizeIc2ChargeEndpoints() {
        return normalizeIc2ChargeEndpoints;
    }

    public static boolean normalizeTconToolWear() {
        return normalizeTconToolWear;
    }

    public static boolean normalizeCofhChargeEndpoints() {
        return normalizeCofhChargeEndpoints;
    }

    public static void configure(boolean normalizeGtTransients, boolean resetGtToolState) {
        configure(
            normalizeGtTransients,
            resetGtToolState,
            normalizeGtChargeEndpoints,
            normalizeIc2ChargeEndpoints,
            normalizeTconToolWear,
            normalizeCofhChargeEndpoints);
    }

    public static void configure(boolean normalizeGtTransients, boolean resetGtToolState, boolean normalizeGtCharge) {
        configure(
            normalizeGtTransients,
            resetGtToolState,
            normalizeGtCharge,
            normalizeIc2ChargeEndpoints,
            normalizeTconToolWear,
            normalizeCofhChargeEndpoints);
    }

    public static void configure(boolean normalizeGtTransients, boolean resetGtToolState, boolean normalizeGtCharge,
        boolean normalizeIc2Charge) {
        configure(
            normalizeGtTransients,
            resetGtToolState,
            normalizeGtCharge,
            normalizeIc2Charge,
            normalizeTconToolWear,
            normalizeCofhChargeEndpoints);
    }

    public static void configure(boolean normalizeGtTransients, boolean resetGtToolState, boolean normalizeGtCharge,
        boolean normalizeIc2Charge, boolean normalizeTconWear, boolean normalizeCofhCharge) {
        normalizeGtTransientIdentity = normalizeGtTransients;
        resetGtToolTemplateState = resetGtToolState;
        normalizeGtChargeEndpoints = normalizeGtCharge;
        normalizeIc2ChargeEndpoints = normalizeIc2Charge;
        normalizeTconToolWear = normalizeTconWear;
        normalizeCofhChargeEndpoints = normalizeCofhCharge;
    }
}
