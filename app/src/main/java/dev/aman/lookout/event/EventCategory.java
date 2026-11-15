package dev.aman.lookout.event;

/** The sort of thing an {@link Event} reports; the Deliveries tab can filter by it. */
public enum EventCategory {
    /** A change on the TV: the app in front, the screen, the screensaver or playback. */
    CHANGE(true),
    /** The periodic "still here". */
    HEARTBEAT(false);

    private final boolean keptWhenDelivered;

    EventCategory(boolean keptWhenDelivered) {
        this.keptWhenDelivered = keptWhenDelivered;
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
