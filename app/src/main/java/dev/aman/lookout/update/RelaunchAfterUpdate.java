package dev.aman.lookout.update;

/**
 * Remembers that the user started an update from the Updates tab, so the new version can reopen Lookout: installing
 * ends the old process, and nothing else would bring the screen back. The request expires, so a failed or abandoned
 * install never makes Lookout pop up later, for example at the next boot.
 */
public final class RelaunchAfterUpdate {
    /** Where the request is kept; it must survive the old process ending. */
    public interface Store {
        /** When the update was requested, by the wall clock; 0 for none. */
        long relaunchRequestedAtMillis();

        void setRelaunchRequestedAtMillis(long millis);
    }

    /** Long enough for a download already finished to install, including a confirmation screen. */
    static final long EXPIRES_AFTER_MILLIS = 10 * 60_000;

    private final Store store;

    public RelaunchAfterUpdate(Store store) {
        this.store = store;
    }

    public void request(long nowMillis) {
        store.setRelaunchRequestedAtMillis(nowMillis);
    }

    public void cancel() {
        store.setRelaunchRequestedAtMillis(0);
    }

    /** Whether to reopen now; answers true at most once per request. */
    public boolean consume(long nowMillis) {
        long requestedAt = store.relaunchRequestedAtMillis();
        if (requestedAt == 0) return false;
        cancel();
        // A clock set back since the request also counts as expired
        return nowMillis >= requestedAt && nowMillis - requestedAt <= EXPIRES_AFTER_MILLIS;
    }
}
