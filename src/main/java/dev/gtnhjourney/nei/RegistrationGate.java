package dev.gtnhjourney.nei;

/** Tiny thread-safe one-shot gate used to keep NEI config reloads from registering duplicate handlers. */
public final class RegistrationGate {

    private boolean acquired;

    public synchronized boolean acquire() {
        if (acquired) return false;
        acquired = true;
        return true;
    }
}
