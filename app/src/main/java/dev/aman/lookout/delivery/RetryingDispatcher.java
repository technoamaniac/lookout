package dev.aman.lookout.delivery;

import dev.aman.lookout.foreground.AppEvent;

import java.util.function.Predicate;

/** Delivers events to a sink off the caller's thread, retrying with backoff. */
public final class RetryingDispatcher {
    /**
     * Runs tasks one at a time after a delay; injectable so tests control time. Retries are
     * scheduled rather than slept, so a newer event never waits behind an older one's backoff.
     */
    public interface Scheduler {
        void schedule(Runnable task, long delayMillis);
    }

    private final EventSink sink;
    private final Scheduler scheduler;
    private final long[] backoffMillis;
    private final Predicate<AppEvent> stillRelevant;

    /**
     * @param backoffMillis delay before each attempt; its length is the attempt count
     * @param stillRelevant checked before every attempt so a stale event is dropped
     *                      once a newer one exists
     */
    public RetryingDispatcher(EventSink sink, Scheduler scheduler, long[] backoffMillis,
                              Predicate<AppEvent> stillRelevant) {
        if (backoffMillis.length == 0) throw new IllegalArgumentException("Needs at least one attempt");
        this.sink = sink;
        this.scheduler = scheduler;
        this.backoffMillis = backoffMillis.clone();
        this.stillRelevant = stillRelevant;
    }

    public void dispatch(AppEvent event) {
        scheduleAttempt(event, 0);
    }

    private void scheduleAttempt(AppEvent event, int attempt) {
        scheduler.schedule(() -> {
            if (!stillRelevant.test(event) || !sink.send(event).isRetryable()) return;
            if (attempt + 1 < backoffMillis.length) scheduleAttempt(event, attempt + 1);
        }, backoffMillis[attempt]);
    }
}
