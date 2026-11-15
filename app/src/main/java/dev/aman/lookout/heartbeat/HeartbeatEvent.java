package dev.aman.lookout.heartbeat;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.JsonFields;

/**
 * A periodic "still here", and nothing else: whether the TV is reachable is all it says. It carries its own interval,
 * so the receiver can work out when one is overdue without hard-coding it.
 */
public final class HeartbeatEvent implements Event {
    private static final String TYPE = "heartbeat";

    private final long intervalSeconds;
    private final String device;
    private final long epochSeconds;

    public HeartbeatEvent(long intervalSeconds, long epochSeconds) {
        this(intervalSeconds, null, epochSeconds);
    }

    private HeartbeatEvent(long intervalSeconds, String device, long epochSeconds) {
        this.intervalSeconds = intervalSeconds;
        this.device = device;
        this.epochSeconds = epochSeconds;
    }

    /** A copy naming this TV so one webhook can serve several; null or blank for none. */
    public HeartbeatEvent withDevice(String device) {
        return new HeartbeatEvent(intervalSeconds, device, epochSeconds);
    }

    @Override
    public String summary() {
        return TYPE;
    }

    @Override
    public String supersessionKey() {
        return TYPE;
    }

    // One every few minutes would push real events out of the short Deliveries list
    @Override
    public boolean listedWhenDelivered() {
        return false;
    }

    @Override
    public JsonFields fields() {
        return new JsonFields()
                .string("event", TYPE)
                .optionalString("device", device)
                .number("interval", intervalSeconds)
                .number("timestamp", epochSeconds);
    }
}
