package dev.aman.lookout.navigation;

/** Asks for Back twice in quick succession before leaving, so a stray press does not close the screen. */
public final class ExitConfirmation {
    private final long windowMillis;
    private boolean asked;
    private long askedAtMillis;

    /** @param windowMillis how long after the first press a second one still confirms */
    public ExitConfirmation(long windowMillis) {
        this.windowMillis = windowMillis;
    }

    /** @return whether to leave now; false the first time, when the user should be asked to press again */
    public boolean confirmed(long nowMillis) {
        if (asked && nowMillis - askedAtMillis <= windowMillis) {
            asked = false;
            return true;
        }
        asked = true;
        askedAtMillis = nowMillis;
        return false;
    }
}
