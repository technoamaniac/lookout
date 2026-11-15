package dev.aman.lookout.foreground;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.JsonFields;

import java.util.Objects;
import java.util.Optional;

/** An app coming to the foreground. */
public final class AppEvent implements Event {
    private static final String TYPE = "app_changed";

    private final String packageName;
    private final String activity;
    private final String device;
    private final long epochSeconds;

    public AppEvent(String packageName, long epochSeconds) {
        this(packageName, null, null, epochSeconds);
    }

    // Private so the two optional strings are always set by name, never swapped by position
    private AppEvent(String packageName, String activity, String device, long epochSeconds) {
        this.packageName = Objects.requireNonNull(packageName);
        this.activity = blankToNull(activity);
        this.device = blankToNull(device);
        this.epochSeconds = epochSeconds;
    }

    /** A copy naming the window class that appeared; null or blank when the system gave none. */
    public AppEvent withActivity(String activity) {
        return new AppEvent(packageName, activity, device, epochSeconds);
    }

    /** A copy naming this TV so one webhook can serve several; null or blank for none. */
    public AppEvent withDevice(String device) {
        return new AppEvent(packageName, activity, device, epochSeconds);
    }

    public String packageName() {
        return packageName;
    }

    public Optional<String> activity() {
        return Optional.ofNullable(activity);
    }

    public long epochSeconds() {
        return epochSeconds;
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
                .optionalString("activity", activity)
                .optionalString("device", device)
                .number("timestamp", epochSeconds);
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s;
    }
}
