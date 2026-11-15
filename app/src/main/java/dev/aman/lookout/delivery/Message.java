package dev.aman.lookout.delivery;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.JsonFields;

import java.util.Objects;
import java.util.UUID;

/**
 * An event as it is sent: its payload plus an {@code id} that stays the same on every attempt, so a receiver can drop
 * the duplicate an interrupted delivery can cause. Immutable, so it can be saved and sent again unchanged.
 */
public final class Message {
    private final String id;
    private final String supersessionKey;
    private final String summary;
    private final JsonFields fields;
    private final long occurredAtMillis;

    /** Restores a saved message; {@code fields} already include the id. */
    public Message(String id, String supersessionKey, String summary, JsonFields fields, long occurredAtMillis) {
        this.id = Objects.requireNonNull(id);
        this.supersessionKey = Objects.requireNonNull(supersessionKey);
        this.summary = Objects.requireNonNull(summary);
        this.fields = fields.copy();
        this.occurredAtMillis = occurredAtMillis;
    }

    /** A new message for {@code event}, with a fresh id. */
    public static Message of(Event event, long occurredAtMillis) {
        String id = UUID.randomUUID().toString();
        return new Message(id, event.supersessionKey(), event.summary(), event.fields().string("id", id),
                occurredAtMillis);
    }

    /** The same message flagged as sent well after it happened, so automations can skip it. Keeps the id. */
    public Message delayed() {
        return new Message(id, supersessionKey, summary, fields.copy().bool("delayed", true), occurredAtMillis);
    }

    public String id() {
        return id;
    }

    /** See {@link Event#supersessionKey()}. */
    public String supersessionKey() {
        return supersessionKey;
    }

    /** See {@link Event#summary()}. */
    public String summary() {
        return summary;
    }

    /** When the event happened, by the wall clock. */
    public long occurredAtMillis() {
        return occurredAtMillis;
    }

    public JsonFields fields() {
        return fields.copy();
    }

    public String toJson() {
        return fields.compact();
    }

    /** The same payload, indented for reading on screen; it is never sent. */
    public String toPrettyJson() {
        return fields.pretty();
    }
}
