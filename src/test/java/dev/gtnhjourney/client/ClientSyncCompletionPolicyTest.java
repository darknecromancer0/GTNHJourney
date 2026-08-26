package dev.gtnhjourney.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ClientSyncCompletionPolicyTest {

    @Test
    public void completeTransportMayPublishFewerUniqueSemanticStacks() {
        assertTrue(ClientSyncCompletionPolicy.mayPublish(176, 176, 161));
        assertTrue(ClientSyncCompletionPolicy.mayPublish(37, 37, 31));
    }

    @Test
    public void incompleteTransportStillFailsClosed() {
        assertFalse(ClientSyncCompletionPolicy.mayPublish(176, 175, 175));
        assertFalse(ClientSyncCompletionPolicy.mayPublish(176, 174, 161));
    }

    @Test
    public void impossibleCountsFailClosed() {
        assertFalse(ClientSyncCompletionPolicy.mayPublish(10, 11, 10));
        assertFalse(ClientSyncCompletionPolicy.mayPublish(10, 10, 11));
        assertFalse(ClientSyncCompletionPolicy.mayPublish(-1, 0, 0));
    }
}
