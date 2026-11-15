package dev.aman.lookout.delivery;

import java.util.Objects;

/** One attempt to deliver a message, and how it went. */
public final class DeliveryRecord {
    private final Message message;
    private final DeliveryResult result;
    private final long attemptedAtMillis;

    public DeliveryRecord(Message message, DeliveryResult result, long attemptedAtMillis) {
        this.message = Objects.requireNonNull(message);
        this.result = Objects.requireNonNull(result);
        this.attemptedAtMillis = attemptedAtMillis;
    }

    public Message message() {
        return message;
    }

    public DeliveryResult result() {
        return result;
    }

    public long attemptedAtMillis() {
        return attemptedAtMillis;
    }
}
