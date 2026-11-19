package dev.aman.lookout.event;

/** The sort of thing an {@link Event} reports; the Deliveries tab can filter by it. */
public enum EventCategory {
    /** A change on the TV: the app in front, the screen, the screensaver or playback. */
    CHANGE(true),
    /** The periodic "still here", with the TV's state. */
    HEARTBEAT(false);

    private final boolean savedAcrossRestarts;

    EventCategory(boolean savedAcrossRestarts) {
        this.savedAcrossRestarts = savedAcrossRestarts;
    }

    /**
     * Whether attempts are saved across restarts. Routine messages are not, delivered or failed: one every 30 seconds
     * would rewrite the file all day, and during an outage every one of them fails. They show on the Deliveries tab
     * from memory only.
     */
    public boolean savedAcrossRestarts() {
        return savedAcrossRestarts;
    }

    /** The category saved under {@code name}; a name this version does not know counts as a change. */
    public static EventCategory fromName(String name) {
        for (EventCategory category : values()) {
            if (category.name().equals(name)) return category;
        }
        return CHANGE;
    }
}
