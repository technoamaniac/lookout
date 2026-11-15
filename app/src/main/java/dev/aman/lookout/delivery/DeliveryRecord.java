package dev.aman.lookout.delivery;

import dev.aman.lookout.event.Event;

import java.util.Objects;

/** One attempt to deliver an event, and how it went. */
public final class DeliveryRecord {
    private final Event event;
    private final DeliveryResult result;
    private final long attemptedAtMillis;

    public DeliveryRecord(Event event, DeliveryResult result, long attemptedAtMillis) {
        this.event = Objects.requireNonNull(event);
        this.result = Objects.requireNonNull(result);
        this.attemptedAtMillis = attemptedAtMillis;
    }

    public Event event() {
        return event;
    }

    public DeliveryResult result() {
        return result;
    }

    public long attemptedAtMillis() {
        return attemptedAtMillis;
    }
}
