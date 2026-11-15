package dev.aman.lookout.foreground;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/** An app coming to the foreground. */
public final class AppEvent {
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

    public String toJson() {
        return render(false);
    }

    /** The same payload, indented for reading on screen; it is never sent. */
    public String toPrettyJson() {
        return render(true);
    }

    private String render(boolean pretty) {
        String newline = pretty ? "\n" : "";
        String indent = pretty ? "  " : "";
        String colon = pretty ? ": " : ":";
        StringJoiner json = new StringJoiner("," + newline, "{" + newline, newline + "}");
        for (Map.Entry<String, String> field : fields().entrySet()) {
            json.add(indent + quote(field.getKey()) + colon + field.getValue());
        }
        return json.toString();
    }

    // Values are already JSON-encoded; optional fields are left out rather than sent as null
    private Map<String, String> fields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("event", quote(TYPE));
        fields.put("package", quote(packageName));
        if (activity != null) fields.put("activity", quote(activity));
        if (device != null) fields.put("device", quote(device));
        fields.put("timestamp", Long.toString(epochSeconds));
        return fields;
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s;
    }

    private static String quote(String s) {
        StringBuilder out = new StringBuilder(s.length() + 2).append('"');
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c < 0x20) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.append('"').toString();
    }
}
