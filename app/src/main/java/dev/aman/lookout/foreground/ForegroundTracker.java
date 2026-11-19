package dev.aman.lookout.foreground;

import java.util.Optional;

/** Turns raw window-change notifications into foreground-app changes. */
public final class ForegroundTracker {
    private volatile PackageFilter filter;
    private volatile String foregroundPackage;
    private volatile boolean paused;

    public ForegroundTracker(PackageFilter filter) {
        this.filter = filter;
    }

    /**
     * Applies new settings without forgetting the app in front, so a settings change is not
     * reported as the same app coming to the front again.
     */
    public void setFilter(PackageFilter filter) {
        this.filter = filter;
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
     * Returns an event only when the foreground app actually changed, not for later screens inside the same app.
     */
    public Optional<AppEvent> onWindowChanged(String packageName) {
        if (paused || packageName == null || !filter.accepts(packageName)
                || packageName.equals(foregroundPackage)) {
            return Optional.empty();
        }
        foregroundPackage = packageName;
        return Optional.of(new AppEvent(packageName));
    }
}
