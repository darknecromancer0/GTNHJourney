package dev.gtnhjourney.acquisition;

/** Detects identity-oriented non-empty furnace output transitions without treating stack-count growth as new. */
public final class FurnaceOutputGate {

    private int signature;
    private boolean occupied;
    private boolean primed;

    public void prime(int signature, boolean occupied) {
        this.signature = signature;
        this.occupied = occupied;
        this.primed = true;
    }

    public boolean observe(int signature, boolean occupied) {
        if (!primed) {
            prime(signature, occupied);
            return false;
        }
        boolean changed = this.signature != signature || this.occupied != occupied;
        this.signature = signature;
        this.occupied = occupied;
        return changed && occupied;
    }
}
