package dev.gtnhjourney.client;

/** Explicit client connection lifecycle gate that rejects late packets from an already-disconnected session. */
public final class ClientSessionGate {

    private boolean active;

    public synchronized void begin() {
        active = true;
    }

    public synchronized void end() {
        active = false;
    }

    public synchronized boolean isActive() {
        return active;
    }
}
