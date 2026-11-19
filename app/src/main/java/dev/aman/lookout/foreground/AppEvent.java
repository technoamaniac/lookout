package dev.aman.lookout.foreground;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.JsonFields;
import dev.aman.lookout.event.TvState;

import java.util.Objects;

/** An app coming to the foreground. */
public final class AppEvent implements Event {
    private static final String TYPE = "app_changed";

    private final String packageName;

    public AppEvent(String packageName) {
        this.packageName = Objects.requireNonNull(packageName);
    }

    public String packageName() {
        return packageName;
    }

    @Override
    public String summary() {
        return packageName;
    }

    @Override
    public TvState applyTo(TvState state) {
        return state.withApp(packageName);
    }

    @Override
    public JsonFields fields() {
        // The app itself is in the TV's state
        return new JsonFields().string("event", TYPE);
    }
}
