package dev.gtnhjourney.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.gtnhjourney.research.ResearchKey;

public class ClientResearchMirrorRemoveTest {

    @AfterEach
    public void clearMirror() {
        ClientResearchMirror.clear();
    }

    @Test
    public void removingPresentKeyIsIncrementalAndIdempotent() {
        ResearchKey first = new ResearchKey("test:first", 0, "");
        ResearchKey second = new ResearchKey("test:second", 0, "");
        ClientResearchMirror.replace(Arrays.asList(first, second));
        long before = ClientResearchMirror.revision();

        assertTrue(ClientResearchMirror.remove(first));
        assertFalse(ClientResearchMirror.contains(first));
        assertTrue(ClientResearchMirror.contains(second));
        assertEquals(before + 1L, ClientResearchMirror.revision());

        assertFalse(ClientResearchMirror.remove(first));
        assertEquals(before + 1L, ClientResearchMirror.revision());
    }
}
