package dev.gtnhjourney.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CommandHintKeyHandlerTest {

    @Test
    public void plainArrowsAreNeverPopupNavigation() {
        assertFalse(CommandHintKeyHandler.shouldMoveSelection(200, false));
        assertFalse(CommandHintKeyHandler.shouldMoveSelection(208, false));
    }

    @Test
    public void shiftArrowsNavigatePopup() {
        assertTrue(CommandHintKeyHandler.shouldMoveSelection(200, true));
        assertTrue(CommandHintKeyHandler.shouldMoveSelection(208, true));
    }

    @Test
    public void unrelatedKeysNeverNavigatePopup() {
        assertFalse(CommandHintKeyHandler.shouldMoveSelection(15, true));
        assertFalse(CommandHintKeyHandler.shouldMoveSelection(28, true));
    }
}
