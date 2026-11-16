package dev.aman.lookout.foreground;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.JsonFields;

import java.util.Objects;

/** An app coming to the foreground. */
public final class AppEvent implements Event {
    private static final String TYPE = "app_changed";

    private final String packageName;
    private final String activity;

    public AppEvent(String packageName) {
        this(packageName, null);
    }

    /** @param activity the window class that appeared; null or blank when the system gave none */
    public AppEvent(String packageName, String activity) {
        this.packageName = Objects.requireNonNull(packageName);
        this.activity = activity;
    }

    public String packageName() {
        return packageName;
    }

    @Override
    public String summary() {
        return packageName;
    }

    // Only the app in front matters, so a newer app change makes an older one stale
    @Override
    public String supersessionKey() {
        return "foreground";
    }

    @Override
    public JsonFields fields() {
        return new JsonFields()
                .string("event", TYPE)
                .string("package", packageName)
                .optionalString("activity", activity);
    }
}
