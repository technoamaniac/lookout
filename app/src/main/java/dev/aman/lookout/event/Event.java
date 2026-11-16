package dev.aman.lookout.event;

/** Something Lookout reports to the webhook. */
public interface Event {
    /** What the Deliveries tab shows for this event: the app's package, or the type of event. */
    String summary();

    /**
     * Events with the same key replace each other: once a newer one exists, an older one that has
     * not been delivered yet is stale, because the receiver only cares about the latest state.
     */
    String supersessionKey();

    /**
     * What this event says, without the fields every message shares (the TV's name, the time and an id), which are
     * added when it is sent. A new builder on each call, so callers may add to it.
     */
    JsonFields fields();

    /** What sort of event this is; a change unless it says otherwise. */
    default EventCategory category() {
        return EventCategory.CHANGE;
    }
}
