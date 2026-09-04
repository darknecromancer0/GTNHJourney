package dev.gtnhjourney.nei;

/** Pure decision table for Journey panel ownership and normal NEI restoration. */
final class JourneyRefreshDecision {

    enum Action {
        PANEL_REFRESH,
        PANEL_ENSURE,
        NEI_FILTER_REFRESH,
        NONE
    }

    private JourneyRefreshDecision() {}

    static Action decide(JourneyViewState.Mode mode, boolean researchChanged, boolean viewChanged) {
        return decide(mode, researchChanged, viewChanged, false);
    }

    static Action decide(
        JourneyViewState.Mode mode,
        boolean researchChanged,
        boolean viewChanged,
        boolean filterChanged) {
        JourneyViewState.Mode effective = mode == null ? JourneyViewState.Mode.ALL : mode;
        if (effective != JourneyViewState.Mode.ALL) {
            return researchChanged || viewChanged || filterChanged ? Action.PANEL_REFRESH : Action.PANEL_ENSURE;
        }
        return viewChanged ? Action.NEI_FILTER_REFRESH : Action.NONE;
    }

    static boolean shouldResetPage(boolean researchChanged, boolean viewChanged, boolean filterChanged) {
        return viewChanged || filterChanged;
    }
}
