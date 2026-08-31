package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class JourneyPanelCollapsibleIsolationContractTest {

    @Test
    public void journeyPanelBypassesNeiCollapsibleGroupsWithoutMutatingGlobalGroupState() throws IOException {
        Path mixin = Paths.get("src/main/java/dev/gtnhjourney/mixin/ItemsPanelGridJourneyMixin.java");
        assertTrue(Files.exists(mixin), "Journey must own collapsible-group visibility while J/N/Delete is active");

        String mixinText = new String(Files.readAllBytes(mixin), StandardCharsets.UTF_8);
        String configText = new String(
            Files.readAllBytes(Paths.get("src/main/resources/mixins.gtnhjourney.json")),
            StandardCharsets.UTF_8);

        assertTrue(mixinText.contains("CollapsibleItems;isEmpty()Z"));
        assertTrue(mixinText.contains("JourneyViewState.isEnabled()"));
        assertTrue(mixinText.contains("return journeyActive || originalEmpty;"));
        assertTrue(configText.contains("\"ItemsPanelGridJourneyMixin\""));

        assertFalse(mixinText.contains("CollapsibleItems.setExpanded"));
        assertFalse(mixinText.contains("CollapsibleItems.toggleGroups"));
    }
}
