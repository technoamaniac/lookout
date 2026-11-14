package dev.aman.lookout;

/** Outcome of handing an event to an {@link EventSink}. */
public enum DeliveryResult {
    DELIVERED,
    /** Failed in a way that may succeed later, such as a network error or an overloaded server. */
    TEMPORARY_FAILURE,
    /** Refused by the receiver; sending the same event again will not help. */
    PERMANENT_FAILURE;

    public boolean isRetryable() {
        return this == TEMPORARY_FAILURE;
    }
}
