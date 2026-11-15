package dev.aman.lookout.foreground;

import java.util.Optional;
import java.util.function.Supplier;

/** Turns raw window-change notifications into foreground-app changes. */
public final class ForegroundTracker {
    private final PackageFilter filter;
    private final Supplier<String> device;
    private volatile AppEvent current;

    /**
     * @param device supplies the name stamped on each event, or null for none; asked per event so
     *               renaming the TV takes effect without rebuilding the tracker
     */
    public ForegroundTracker(PackageFilter filter, Supplier<String> device) {
        this.filter = filter;
        this.device = device;
    }

    /**
     * Returns an event only when the foreground app actually changed, so the activity reported is
     * the one that opened the app, not later screens inside it.
     */
    public Optional<AppEvent> onWindowChanged(String packageName, String activity, long epochSeconds) {
        if (packageName == null || !filter.accepts(packageName) || isInForeground(packageName)) {
            return Optional.empty();
        }
        AppEvent event = new AppEvent(packageName, activity, device.get(), epochSeconds);
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
