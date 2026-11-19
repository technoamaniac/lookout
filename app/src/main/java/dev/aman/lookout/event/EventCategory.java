package dev.aman.lookout.event;

/** The sort of thing an {@link Event} reports; the Deliveries tab can filter by it. */
public enum EventCategory {
    /** A change on the TV: the app in front, the screen, the screensaver or playback. */
    CHANGE,
    /** The periodic "still here", with the TV's state. */
    HEARTBEAT
}
