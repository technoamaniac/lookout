package dev.aman.lookout.delivery;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.EventCategory;
import dev.aman.lookout.event.JsonFields;
import dev.aman.lookout.event.TvState;

import java.util.Objects;
import java.util.UUID;

/**
 * An event as it is sent: its payload plus an {@code id}, so a receiver or the Deliveries tab can tell messages apart.
 * Immutable, so it can be saved and shown again unchanged.
 */
public final class Message {
    private final String id;
    private final String summary;
    private final JsonFields fields;
    private final long occurredAtMillis;
    private final EventCategory category;

    /** Restores a saved message; {@code fields} already include the id. */
    public Message(String id, String summary, EventCategory category, JsonFields fields, long occurredAtMillis) {
        this.id = Objects.requireNonNull(id);
        this.summary = Objects.requireNonNull(summary);
        this.fields = fields.copy();
        this.occurredAtMillis = occurredAtMillis;
        this.category = Objects.requireNonNull(category);
    }

    /**
     * A new message for {@code event}, with a fresh id. Every message ends with the same fields, so a receiver reads
     * them alike whatever the event: the TV's state, its name, when it happened in Unix seconds, and the id.
     *
     * @param tv     the TV's state once the event happened
     * @param device the TV's name, so one webhook can serve several; null or blank to leave it out
     */
    public static Message of(Event event, TvState tv, String device, long occurredAtMillis) {
        return stamped(event, event.fields().object("tv", tv.fields()), device, occurredAtMillis);
    }

    /** A message without the TV's state, for a test from the settings screen, which knows nothing of it. */
    public static Message withoutState(Event event, String device, long occurredAtMillis) {
        return stamped(event, event.fields(), device, occurredAtMillis);
    }

    private static Message stamped(Event event, JsonFields fields, String device, long occurredAtMillis) {
        String id = UUID.randomUUID().toString();
        fields.optionalString("device", device)
                .number("timestamp", occurredAtMillis / 1_000)
                .string("id", id);
        return new Message(id, event.summary(), event.category(), fields, occurredAtMillis);
    }

    public String id() {
        return id;
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
