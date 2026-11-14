package dev.aman.lookout.foreground;

import java.util.Objects;

/** An app coming to the foreground. */
public final class AppEvent {
    private final String packageName;
    private final long epochSeconds;

    public AppEvent(String packageName, long epochSeconds) {
        this.packageName = Objects.requireNonNull(packageName);
        this.epochSeconds = epochSeconds;
    }

    public String packageName() {
        return packageName;
    }

    public long epochSeconds() {
        return epochSeconds;
    }

    public String toJson() {
        return "{\"package\":\"" + escape(packageName) + "\",\"timestamp\":" + epochSeconds + "}";
    }

    private static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c < 0x20) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
