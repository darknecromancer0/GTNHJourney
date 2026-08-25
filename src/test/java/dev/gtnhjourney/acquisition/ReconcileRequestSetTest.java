package dev.gtnhjourney.acquisition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class ReconcileRequestSetTest {

    @Test
    public void repeatedRequestsCollapseIntoOneConsume() {
        ReconcileRequestSet requests = new ReconcileRequestSet();
        UUID player = UUID.randomUUID();

        requests.request(player);
        requests.request(player);

        assertTrue(requests.consume(player));
        assertFalse(requests.consume(player));
    }

    @Test
    public void differentPlayersAreIndependent() {
        ReconcileRequestSet requests = new ReconcileRequestSet();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        requests.request(first);
        requests.request(second);

        assertTrue(requests.consume(first));
        assertFalse(requests.consume(first));
        assertTrue(requests.consume(second));
    }
}
