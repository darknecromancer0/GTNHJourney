package dev.gtnhjourney.debug;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class LoadedWorldAccessPolicyTest {

    @Test
    public void onlyAllowsValidHeightInsideAlreadyLoadedChunks() {
        assertFalse(LoadedWorldAccess.canRead(-1, true));
        assertFalse(LoadedWorldAccess.canRead(256, true));
        assertFalse(LoadedWorldAccess.canRead(64, false));
        assertTrue(LoadedWorldAccess.canRead(0, true));
        assertTrue(LoadedWorldAccess.canRead(255, true));
    }
}
