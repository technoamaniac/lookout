package dev.aman.lookout.heartbeat;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.EventCategory;
import dev.aman.lookout.event.JsonFields;

/**
 * A periodic "still here", and nothing else: whether the TV is reachable is all it says. It carries its own interval,
 * so the receiver can work out when one is overdue without hard-coding it.
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
    public String supersessionKey() {
        return TYPE;
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
