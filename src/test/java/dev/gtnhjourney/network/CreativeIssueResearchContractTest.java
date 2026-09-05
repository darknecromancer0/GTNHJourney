package dev.gtnhjourney.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class CreativeIssueResearchContractTest {

    @Test
    void successfulCreativeIssueUsesAuthoritativeResearchObservationImmediately() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/network/ServerRequestQueue.java");

        assertTrue(source.contains("ResearchObservationService"),
            "C issuance must use the shared authoritative research observation path");
        assertTrue(source.contains("observeCreativeIssue(player, template)"),
            "both single-stack and fill-inventory C success paths must research the exact issued template immediately");
        assertTrue(source.contains("observations.observe(player, template);"),
            "C research must happen from the server-side issuance template instead of later inventory interaction");
        assertFalse(source.contains("CreativeIssueResearchSuppressor.mark(player, template)"),
            "C issuance must no longer suppress the issued identity from research");
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
