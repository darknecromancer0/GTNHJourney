package dev.gtnhjourney.nei;

/** Pure policy for the one proven GT volumetric-flask renderer failure. */
final class JourneyGlobalSafetyPolicy {

    private static final String VOLUMETRIC_FLASK = "gregtech.common.items.ItemVolumetricFlask";

    private JourneyGlobalSafetyPolicy() {}

    static boolean shouldHide(String itemClassName, boolean hasFluidPayload, boolean unsafeFluidIcon) {
        return VOLUMETRIC_FLASK.equals(itemClassName) && hasFluidPayload && unsafeFluidIcon;
    }
}
