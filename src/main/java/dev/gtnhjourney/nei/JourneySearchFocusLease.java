package dev.gtnhjourney.nei;

/**
 * Remembers the user's intent to keep typing into NEI search after Ctrl+Backspace. The lease survives asynchronous
 * Journey panel refreshes but is cancelled by the next genuine input intent, mouse click or screen transition.
 * Never takes focus spontaneously unless the search field was focused when the original chord was pressed.
 */
public final class JourneySearchFocusLease {

    private Object screen;

    public synchronized boolean activeFor(Object currentScreen) {
        if (screen != null && screen != currentScreen) screen = null;
        return screen != null;
    }

    public synchronized void arm(Object currentScreen) {
        screen = currentScreen;
    }

    public synchronized void clear() {
        screen = null;
    }

    public synchronized boolean shouldRestore(Object currentScreen, boolean visible, boolean focused) {
        return activeFor(currentScreen) && visible && !focused;
    }

    /**
     * Head-of-input decision. The caller restores focus before invoking this when an earlier lease is active.
     * A deliberate new key press consumes the lease unless it starts another Ctrl+Backspace chord.
     */
    public synchronized boolean onKeyDown(Object currentScreen, boolean ctrlBackspace, boolean visible,
        boolean focused) {
        boolean consume = ctrlBackspace && visible && focused && currentScreen != null;
        if (consume) arm(currentScreen);
        else clear();
        return consume;
    }
}
