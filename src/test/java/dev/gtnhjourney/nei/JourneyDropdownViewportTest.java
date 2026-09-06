package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class JourneyDropdownViewportTest {

    @Test
    void clampsPopupInsideViewport() {
        Class<?> type = assertDoesNotThrow(() -> Class.forName("dev.gtnhjourney.nei.JourneyDropdownViewport"));
        Method clamp = assertDoesNotThrow(() -> type.getDeclaredMethod("clampPopupX", int.class, int.class, int.class));
        clamp.setAccessible(true);

        assertEquals(532, invoke(clamp, 590, 108, 640));
        assertEquals(420, invoke(clamp, 420, 108, 640));
        assertEquals(0, invoke(clamp, 12, 108, 80));
    }

    private static int invoke(Method method, int anchorX, int popupWidth, int screenWidth) {
        return assertDoesNotThrow(() -> ((Integer) method.invoke(null, anchorX, popupWidth, screenWidth)).intValue());
    }
}
