package dev.aman.lookout.event;

/** The sort of thing an {@link Event} reports; the Deliveries tab can filter by it. */
public enum EventCategory {
    /** A change on the TV: the app in front, the screen, the screensaver or playback. */
    CHANGE(true, true),
    /** The periodic "still here". */
    HEARTBEAT(false, false);

    private final boolean keptWhenDelivered;
    private final boolean resentWhenFailed;

    EventCategory(boolean keptWhenDelivered, boolean resentWhenFailed) {
        this.keptWhenDelivered = keptWhenDelivered;
        this.resentWhenFailed = resentWhenFailed;
    }

    /** Whether a failed attempt is tried again later. A heartbeat is sent once: a late one would say the TV is reachable. */
    public boolean resentWhenFailed() {
        return resentWhenFailed;
    }

    /**
     * Whether a delivered attempt is worth saving across restarts. Routine messages are not: one every minute would
     * rewrite the file for nothing, and the next one says all a delivered one would. Failures are always kept.
     */
    public boolean keptWhenDelivered() {
        return keptWhenDelivered;
    }

    /** The category saved under {@code name}; a name this version does not know counts as a change. */
    public static EventCategory fromName(String name) {
        for (EventCategory category : values()) {
            if (category.name().equals(name)) return category;
        }
        return CHANGE;
    }
}
