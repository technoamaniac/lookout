package dev.aman.lookout.delivery;

/** Outcome of handing an event to an {@link EventSink}. */
public enum DeliveryResult {
    DELIVERED,
    /** Failed in a way that may clear up by itself, such as a network error or an overloaded server. */
    TEMPORARY_FAILURE,
    /** Refused by the receiver, such as a wrong path or bad auth; it will not clear up until the user acts. */
    PERMANENT_FAILURE
}
