package dev.aman.lookout.heartbeat;

/**
 * Runs a beat at a fixed interval: one straight away when started, so the receiver hears at once, then one per
 * interval. Not thread-safe: use it on the thread its scheduler runs tasks on.
 */
public final class Heartbeat {
    /** Runs a task once after a delay; injectable so tests control time. */
    public interface Scheduler {
        /** @return cancels the task if it has not run */
        Runnable schedule(Runnable task, long delayMillis);
    }

    private final Scheduler scheduler;
    private final long intervalMillis;
    private final Runnable beat;
    private Runnable cancelNext = () -> {};

    /** @param beat sends one heartbeat */
    public Heartbeat(Scheduler scheduler, long intervalMillis, Runnable beat) {
        if (intervalMillis <= 0) throw new IllegalArgumentException("Interval must be positive");
        this.scheduler = scheduler;
        this.intervalMillis = intervalMillis;
        this.beat = beat;
    }

    /** Beats now, then every interval from now; if it was running, the old schedule ends. */
    public void restart() {
        stop();
        beat();
    }

    public void stop() {
        cancelNext.run();
        cancelNext = () -> {};
    }

    private void beat() {
        // Scheduled first, so a failure while sending one does not end the beats
        cancelNext = scheduler.schedule(this::beat, intervalMillis);
        beat.run();
    }
}
