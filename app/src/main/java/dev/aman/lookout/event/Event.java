package dev.aman.lookout.event;

/** Something Lookout reports to the webhook. */
public interface Event {
    /** What the Deliveries tab shows for this event: the app's package, or the type of event. */
    String summary();

    /** The TV's state once this event has happened; unchanged by an event that says nothing about the TV. */
    TvState applyTo(TvState state);

    /**
     * What this event says, without the fields every message shares (the TV's state and name, the time and an id),
     * which are added when it is sent. A new builder on each call, so callers may add to it.
     */
    JsonFields fields();

    /** What sort of event this is; a change unless it says otherwise. */
    default EventCategory category() {
        return EventCategory.CHANGE;
    }
}
