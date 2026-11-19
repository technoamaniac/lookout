package dev.aman.lookout.device;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.JsonFields;
import dev.aman.lookout.event.TvState;

import java.util.Objects;

/** A change in the TV itself rather than in the app in front: the screen or the screensaver. */
public final class DeviceEvent implements Event {
    public enum Kind {
        SCREEN_ON("screen_on"),
        SCREEN_OFF("screen_off"),
        SCREENSAVER_STARTED("screensaver_started"),
        SCREENSAVER_STOPPED("screensaver_stopped");

        private final String type;

        Kind(String type) {
            this.type = type;
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

    @Override
    public TvState applyTo(TvState state) {
        switch (kind) {
            case SCREEN_ON:
                return state.withScreenOn(true);
            case SCREEN_OFF:
                // A screen that is off shows no screensaver, even if its stop notice never came
                return state.withScreenOn(false).withScreensaver(false);
            case SCREENSAVER_STARTED:
                return state.withScreensaver(true);
            case SCREENSAVER_STOPPED:
                return state.withScreensaver(false);
            default:
                throw new IllegalStateException("No state for " + kind);
        }
    }

    @Override
    public JsonFields fields() {
        return new JsonFields().string("event", kind.type);
    }
}
