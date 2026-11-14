package dev.aman.lookout;

import java.util.Optional;

/** Turns raw window-change notifications into foreground-app changes. */
public final class ForegroundTracker {
    private final PackageFilter filter;
    private volatile AppEvent current;

    public ForegroundTracker(PackageFilter filter) {
        this.filter = filter;
    }

    /** Returns an event only when the foreground app actually changed. */
    public Optional<AppEvent> onWindowChanged(String packageName, long epochSeconds) {
        if (packageName == null || !filter.accepts(packageName) || isInForeground(packageName)) {
            return Optional.empty();
        }
        AppEvent event = new AppEvent(packageName, epochSeconds);
        current = event;
        return Optional.of(event);
    }

    /**
     * True while no newer event has replaced this one. Compares identity, not package: after
     * A, B, A the first A event is stale even though A is in front again.
     */
    public boolean isCurrent(AppEvent event) {
        return event == current;
    }

    private boolean isInForeground(String packageName) {
        AppEvent last = current;
        return last != null && last.packageName().equals(packageName);
    }
}
