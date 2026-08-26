package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class JourneyButtonPresentationContractTest {

    @Test
    public void newestTooltipDescribesFullResearchSetAndActivityOrdering() {
        String inactive = JourneyButtonPresentation.newestTooltip(false);
        String active = JourneyButtonPresentation.newestTooltip(true);

        assertTrue(inactive.contains("all researched"));
        assertTrue(inactive.contains("recent Journey activity"));
        assertTrue(active.contains("all researched"));
        assertTrue(active.contains("recent Journey activity"));
    }
}
