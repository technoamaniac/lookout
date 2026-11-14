package dev.aman.lookout;

import java.util.Optional;

/** Turns raw window-change notifications into foreground-app changes. */
public final class ForegroundTracker {
    private final PackageFilter filter;
    private volatile String current = "";

    public ForegroundTracker(PackageFilter filter) {
        this.filter = filter;
    }

    /** Returns an event only when the foreground app actually changed. */
    public Optional<AppEvent> onWindowChanged(String packageName, long epochSeconds) {
        if (packageName == null || !filter.accepts(packageName) || packageName.equals(current)) {
            return Optional.empty();
        }
        current = packageName;
        return Optional.of(new AppEvent(packageName, epochSeconds));
    }

    public boolean isCurrent(AppEvent event) {
        return event.packageName().equals(current);
    }
}
