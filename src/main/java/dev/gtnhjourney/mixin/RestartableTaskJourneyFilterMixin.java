package dev.gtnhjourney.mixin;

/**
 * Legacy source marker kept only so old source-level regression references fail safely during the 1.1.26 migration.
 * Runtime observation moved to ItemPanelJourneyFilterMixin: RestartableTask.restart() is deliberately not intercepted.
 */
@Deprecated
public final class RestartableTaskJourneyFilterMixin {

    private RestartableTaskJourneyFilterMixin() {}
}
