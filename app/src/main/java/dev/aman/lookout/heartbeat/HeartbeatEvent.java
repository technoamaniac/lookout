package dev.aman.lookout.heartbeat;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.EventCategory;
import dev.aman.lookout.event.JsonFields;
import dev.aman.lookout.event.TvState;

/**
 * A periodic "still here". Like every message it carries the TV's state, so a receiver that missed a change catches
 * up with the next one. It carries its own interval, so the receiver can work out when one is overdue without
 * hard-coding it.
 */
public final class HeartbeatEvent implements Event {
    private static final String TYPE = "heartbeat";

    private final long intervalSeconds;

    public HeartbeatEvent(long intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    @Override
    public String summary() {
        return TYPE;
    }

    @Override
    public TvState applyTo(TvState state) {
        return state;
    }

    @Override
    public EventCategory category() {
        return EventCategory.HEARTBEAT;
    }

    @Override
    public JsonFields fields() {
        return new JsonFields().string("event", TYPE).number("interval", intervalSeconds);
    }
}
