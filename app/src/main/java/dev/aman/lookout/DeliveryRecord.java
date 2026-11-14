package dev.aman.lookout;

import java.util.Objects;

/** One attempt to deliver an event, and how it went. */
public final class DeliveryRecord {
    private final AppEvent event;
    private final DeliveryResult result;
    private final long attemptedAtMillis;

    public DeliveryRecord(AppEvent event, DeliveryResult result, long attemptedAtMillis) {
        this.event = Objects.requireNonNull(event);
        this.result = Objects.requireNonNull(result);
        this.attemptedAtMillis = attemptedAtMillis;
    }

    public AppEvent event() {
        return event;
    }

    public DeliveryResult result() {
        return result;
    }

    public long attemptedAtMillis() {
        return attemptedAtMillis;
    }
}
