package dev.aman.lookout.event;

/** Something Lookout reports to the webhook. */
public interface Event {
    /** What the Deliveries tab shows for this event: the app's package, or the kind of event. */
    String summary();

    /**
     * Events with the same key replace each other: once a newer one exists, an older one that has
     * not been delivered yet is stale, because the receiver only cares about the latest state.
     */
    String supersessionKey();

    /** The payload's fields; a new builder on each call, so callers may add to it. */
    JsonFields fields();
}
