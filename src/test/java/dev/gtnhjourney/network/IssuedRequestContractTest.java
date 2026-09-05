package dev.gtnhjourney.network;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class IssuedRequestContractTest {

    @Test
    void allSuccessfulJourneyIssuancePathsTouchTheIssuedTimeline() throws IOException {
        String queue = read("src/main/java/dev/gtnhjourney/network/ServerRequestQueue.java");
        String network = read("src/main/java/dev/gtnhjourney/network/JourneyNetwork.java");

        assertTrue(queue.contains("research.recordRetrieval(player, key);"),
            "J and fill issuance must persist only after a successful grant");
        assertTrue(queue.contains("if (filled <= 0) return;"),
            "fill inventory must not mark an issuance when no slot changed");
        assertTrue(queue.contains("recordCreativeIssued(player, template);"),
            "C issuance must persist native items independently from research observation");
        assertTrue(queue.contains("research.recordIssued(player, key);"),
            "creative issuance must use the issued-only server timeline");
        assertTrue(network.contains("sendIssuedTouch(EntityPlayerMP player, ResearchFingerprint fingerprint)"),
            "successful issuance must have an incremental client update path");
        assertTrue(network.contains("deferIssuedTouchIfActive"),
            "an issuance during login sync must be replayed after the older snapshot");
    }

    @Test
    void fullSyncTreatsIssuedChronologyAsPartOfTheAtomicVisibleEpoch() throws IOException {
        String begin = read("src/main/java/dev/gtnhjourney/network/ResearchSyncBeginMessage.java");
        String end = read("src/main/java/dev/gtnhjourney/network/ResearchSyncEndMessage.java");
        String queue = read("src/main/java/dev/gtnhjourney/network/ServerResearchSyncQueue.java");

        assertTrue(begin.contains("ClientIssuedMirror.begin(epoch, issuedTotal);"));
        assertTrue(end.contains("!ClientIssuedMirror.isComplete(epoch)"));
        assertTrue(end.contains("ClientIssuedMirror.finish(epoch);"));
        assertTrue(queue.contains("session.issuedCursor >= session.issuedChunks.size()"));
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
