package dev.gtnhjourney.safety;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ThaumcraftWarpCleanserTest {

    @Test
    void resetAllThreeWarpCategoriesWithoutTouchingResearch() {
        assertArrayEquals(new String[] {
            "tc warp Player_123 set 0",
            "tc warp Player_123 set 0 TEMP",
            "tc warp Player_123 set 0 PERM"
        }, ThaumcraftWarpCleanser.resetCommands("Player_123"));
    }

    @Test
    void invalidCommandSenderCannotInjectAdminCommands() {
        assertEquals(0, ThaumcraftWarpCleanser.resetCommands("Player;op Other").length);
        assertEquals(0, ThaumcraftWarpCleanser.resetCommands("../player").length);
        assertEquals(0, ThaumcraftWarpCleanser.resetCommands(null).length);
    }
}
