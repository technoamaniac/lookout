package dev.aman.lookout.delivery;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.EventCategory;
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
    private final EventCategory category;

    /** Restores a saved message; {@code fields} already include the id. */
    public Message(String id, String supersessionKey, String summary, EventCategory category, JsonFields fields,
                   long occurredAtMillis) {
        this.id = Objects.requireNonNull(id);
        this.supersessionKey = Objects.requireNonNull(supersessionKey);
        this.summary = Objects.requireNonNull(summary);
        this.fields = fields.copy();
        this.occurredAtMillis = occurredAtMillis;
        this.category = Objects.requireNonNull(category);
    }

    /** A new message for {@code event}, with a fresh id. */
    public static Message of(Event event, long occurredAtMillis) {
        String id = UUID.randomUUID().toString();
        return new Message(id, event.supersessionKey(), event.summary(), event.category(), event.fields().string("id", id),
                occurredAtMillis);
    }

    /** The same message flagged as sent well after it happened, so automations can skip it. Keeps the id. */
    public Message markedDelayed() {
        return new Message(id, supersessionKey, summary, category, fields.copy().bool("delayed", true), occurredAtMillis);
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

    /** See {@link Event#category()}. */
    public EventCategory category() {
        return category;
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
