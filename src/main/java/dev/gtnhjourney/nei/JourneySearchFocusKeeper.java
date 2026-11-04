package dev.gtnhjourney.nei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Keyboard;

import codechicken.nei.LayoutManager;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.SearchField;

/**
 * Restores native NEI search focus only for a Ctrl+Backspace initiated in an already-focused visible search box.
 * A subsequent key press or click ends the retention window, so manual focus changes remain respected.
 */
public final class JourneySearchFocusKeeper {

    private static final JourneySearchFocusLease LEASE = new JourneySearchFocusLease();

    private JourneySearchFocusKeeper() {}

    /** Called before NEI dispatches the current keyboard event; returns whether it owns Ctrl+Backspace. */
    public static boolean beforeKeyEvent() {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        SearchField field = LayoutManager.searchField;
        if (field == null || screen == null) {
            LEASE.clear();
            return false;
        }

        // Key releases do not express a new focus choice and must not revoke the lease.
        if (!Keyboard.getEventKeyState()) return false;

        restore(screen, field);
        boolean chord = Keyboard.getEventKey() == Keyboard.KEY_BACK && NEIClientUtils.controlKey();
        return LEASE.onKeyDown(screen, chord, field.isVisible(), field.focused());
    }

    /** Called after NEI's own textboxKeyTyped/onTextChange, before vanilla global key dispatch. */
    public static void afterNativeCtrlBackspace() {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        SearchField field = LayoutManager.searchField;
        if (screen != null && field != null) restore(screen, field);
    }

    /** Called after NEI lays out widgets (which may briefly defocus its search during filter refresh). */
    public static void afterLayout() {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        SearchField field = LayoutManager.searchField;
        if (screen == null || field == null) {
            LEASE.clear();
            return;
        }
        restore(screen, field);
    }

    public static void onMouseClick() {
        LEASE.clear();
    }

    private static void restore(GuiScreen screen, SearchField field) {
        if (LEASE.shouldRestore(screen, field.isVisible(), field.focused())) field.setFocus(true);
    }
}
