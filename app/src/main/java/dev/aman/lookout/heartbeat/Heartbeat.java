package dev.aman.lookout.heartbeat;

import dev.aman.lookout.event.Event;

import java.util.function.Consumer;
import java.util.function.LongFunction;

/**
 * Sends a heartbeat at a fixed interval: one straight away when started or the interval changes, so the receiver
 * hears at once, then one per interval. The first is queued rather than sent inside {@link #setInterval}, so it reads
 * the state once the caller has finished setting up, such as the playback a starting service has yet to read. It runs
 * on a clock that stops while the TV sleeps, so a TV in deep standby goes quiet. Not thread-safe: use it on the thread
 * its scheduler runs tasks on.
 */
public final class Heartbeat {
    /** Runs a task once after a delay; injectable so tests control time. */
    public interface Scheduler {
        /** @return cancels the task if it has not run */
        Runnable schedule(Runnable task, long delayMillis);
    }

    private final Scheduler scheduler;
    private final LongFunction<Event> beatFor;
    private final Consumer<Event> send;
    private long intervalSeconds;
    private Runnable cancelNext = () -> {};

    /**
     * @param beatFor builds the heartbeat for an interval in seconds, from the state at that moment
     * @param send    delivers it once, without retries: a late heartbeat would wrongly say the TV is reachable
     */
    public Heartbeat(Scheduler scheduler, LongFunction<Event> beatFor, Consumer<Event> send) {
        this.scheduler = scheduler;
        this.beatFor = beatFor;
        this.send = send;
    }

    /** @param intervalSeconds 0 stops it; the same interval again changes nothing */
    public void setInterval(long intervalSeconds) {
        if (intervalSeconds == this.intervalSeconds) return;
        stop();
        this.intervalSeconds = intervalSeconds;
        if (intervalSeconds > 0) cancelNext = scheduler.schedule(this::beat, 0);
    }

    public void stop() {
        cancelNext.run();
        cancelNext = () -> {};
        intervalSeconds = 0;
    }

    private void beat() {
        send.accept(beatFor.apply(intervalSeconds));
        cancelNext = scheduler.schedule(this::beat, intervalSeconds * 1000);
    }
}
