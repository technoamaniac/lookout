package dev.aman.lookout.device;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.JsonFields;

import java.util.Objects;

/** A change in the TV itself rather than in the app in front: the screen or the screensaver. */
public final class DeviceEvent implements Event {
    public enum Kind {
        SCREEN_ON("screen_on", "screen"),
        SCREEN_OFF("screen_off", "screen"),
        SCREENSAVER_STARTED("screensaver_started", "screensaver"),
        SCREENSAVER_STOPPED("screensaver_stopped", "screensaver");

        private final String type;
        private final String state;

        Kind(String type, String state) {
            this.type = type;
            this.state = state;
        }
    }

    private final Kind kind;
    private final String device;
    private final long epochSeconds;

    public DeviceEvent(Kind kind, long epochSeconds) {
        this(kind, null, epochSeconds);
    }

    private DeviceEvent(Kind kind, String device, long epochSeconds) {
        this.kind = Objects.requireNonNull(kind);
        this.device = device;
        this.epochSeconds = epochSeconds;
    }

    /** A copy naming this TV so one webhook can serve several; null or blank for none. */
    public DeviceEvent withDevice(String device) {
        return new DeviceEvent(kind, device, epochSeconds);
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public String summary() {
        return kind.type;
    }

    // Screen on and off replace each other, as do screensaver start and stop; the two states are
    // independent, so a screensaver event never makes a screen event stale
    @Override
    public String supersessionKey() {
        return "device:" + kind.state;
    }

    @Override
    public String toJson() {
        return json().compact();
    }

    @Override
    public String toPrettyJson() {
        return json().pretty();
    }

    private JsonFields json() {
        return new JsonFields()
                .string("event", kind.type)
                .optionalString("device", device)
                .number("timestamp", epochSeconds);
    }
}
