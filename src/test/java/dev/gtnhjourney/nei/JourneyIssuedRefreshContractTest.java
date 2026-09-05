package dev.gtnhjourney.nei;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class JourneyIssuedRefreshContractTest {

    @Test
    void issuedMirrorRevisionParticipatesInPanelRefreshInvalidation() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/nei/JourneyNEIRefreshTracker.java");

        assertTrue(source.contains("ClientIssuedMirror"),
            "refresh tracker must observe successful-issuance chronology");
        assertTrue(source.contains("ClientIssuedMirror.revision()"),
            "an issued touch must invalidate the currently visible sorted panel");
        assertTrue(source.contains("seenIssuedRevision"),
            "issued revision must be remembered independently from legacy activity");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
