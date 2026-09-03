package dev.gtnhjourney.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class ServerPacketLifecycleContractTest {

    @Test
    void everyServerToClientJourneyPacketUsesOneDisconnectSafeSendGate() throws IOException {
        String source = read("src/main/java/dev/gtnhjourney/network/JourneyNetwork.java");

        assertTrue(source.contains("private static boolean sendToConnected("),
            "JourneyNetwork must centralize all server-to-client packet sends behind one gate");
        assertTrue(source.contains("player.playerNetServerHandler == null"),
            "the send gate must reject players whose NetHandlerPlayServer is already gone");
        assertTrue(source.contains("catch (NullPointerException disconnectedRace)"),
            "the send gate must absorb the narrow handler-disappeared race inside SimpleNetworkWrapper.sendTo");
        assertTrue(source.contains("if (player.playerNetServerHandler == null) return false;"),
            "an NPE may only be swallowed when the player actually disconnected during the send");
        assertEquals(1, occurrences(source, "CHANNEL.sendTo("),
            "direct SimpleNetworkWrapper.sendTo calls must exist only inside the safe send gate");
    }

    @Test
    void logoutCancelsResearchSyncAndPendingJourneyRequestsForThatPlayer() throws IOException {
        String sync = read("src/main/java/dev/gtnhjourney/network/ServerResearchSyncQueue.java");
        String requests = read("src/main/java/dev/gtnhjourney/network/ServerRequestQueue.java");

        assertTrue(sync.contains("PlayerLoggedOutEvent"), "research sync queue must observe player logout");
        assertTrue(sync.contains("cancel((EntityPlayerMP) event.player)"),
            "logout must remove the active research sync session immediately");

        assertTrue(requests.contains("PlayerLoggedOutEvent"), "request queue must observe player logout");
        assertTrue(requests.contains("cancelPending((EntityPlayerMP) event.player)"),
            "logout must remove pending requests that still retain the stale EntityPlayerMP");
        assertTrue(requests.contains("REQUESTS.remove(request)"),
            "per-player cancellation must remove stale queued requests rather than clearing unrelated players");
        assertTrue(requests.contains("PER_PLAYER.release(request.playerId)"),
            "removing a queued request must also release its per-player limiter slot");
    }

    private static int occurrences(String source, String token) {
        int count = 0;
        int cursor = 0;
        while ((cursor = source.indexOf(token, cursor)) >= 0) {
            count++;
            cursor += token.length();
        }
        return count;
    }

    private static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
