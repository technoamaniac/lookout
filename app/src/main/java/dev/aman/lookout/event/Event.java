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

    String toJson();

    /** The same payload, indented for reading on screen; it is never sent. */
    String toPrettyJson();
}
