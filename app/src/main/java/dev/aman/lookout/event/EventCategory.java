package dev.aman.lookout.event;

/** The sort of thing an {@link Event} reports; the Deliveries tab can filter by it. */
public enum EventCategory {
    /** A change on the TV: the app in front, the screen, the screensaver or playback. */
    CHANGE,
    /** The periodic "still here". */
    HEARTBEAT;

    /** The category saved under {@code name}; a name this version does not know counts as a change. */
    public static EventCategory fromName(String name) {
        for (EventCategory category : values()) {
            if (category.name().equals(name)) return category;
        }
        return CHANGE;
    }
}
