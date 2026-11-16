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

    public DeviceEvent(Kind kind) {
        this.kind = Objects.requireNonNull(kind);
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
    public JsonFields fields() {
        return new JsonFields().string("event", kind.type);
    }
}
