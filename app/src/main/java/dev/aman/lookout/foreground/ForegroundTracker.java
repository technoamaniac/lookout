package dev.aman.lookout.foreground;

import java.util.Optional;
import java.util.function.Supplier;

/** Turns raw window-change notifications into foreground-app changes. */
public final class ForegroundTracker {
    private volatile PackageFilter filter;
    private volatile Supplier<String> device;
    private volatile AppEvent current;
    private volatile boolean paused;

    /**
     * @param device supplies the name stamped on each event, or null for none; asked per event so
     *               renaming the TV takes effect without rebuilding the tracker
     */
    public ForegroundTracker(PackageFilter filter, Supplier<String> device) {
        this.filter = filter;
        this.device = device;
    }

    /**
     * Applies new settings without forgetting the app in front, so a settings change is not
     * reported as the same app coming to the front again.
     */
    public void reconfigure(PackageFilter filter, Supplier<String> device) {
        this.filter = filter;
        this.device = device;
    }

    /**
     * Stops reporting while something other than an app owns the screen, such as the screensaver:
     * its window is not an app the user opened, and the app that was in front is still there
     * afterwards.
     */
    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    /**
     * Returns an event only when the foreground app actually changed, so the activity reported is
     * the one that opened the app, not later screens inside it.
     */
    public Optional<AppEvent> onWindowChanged(String packageName, String activity, long epochSeconds) {
        if (paused || packageName == null || !filter.accepts(packageName) || isInForeground(packageName)) {
            return Optional.empty();
        }
        AppEvent event = new AppEvent(packageName, epochSeconds).withActivity(activity).withDevice(device.get());
        current = event;
        return Optional.of(event);
    }

    /** The app last reported in front, or null before the first. */
    public String currentPackage() {
        AppEvent last = current;
        return last == null ? null : last.packageName();
    }

    private boolean isInForeground(String packageName) {
        AppEvent last = current;
        return last != null && last.packageName().equals(packageName);
    }
}
