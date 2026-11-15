package dev.aman.lookout.delivery;

import dev.aman.lookout.event.Event;

import java.util.function.Predicate;

/** Delivers events to a sink off the caller's thread, retrying with backoff. */
public final class RetryingDispatcher implements ReportingGate.Sender {
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
    private final Predicate<Event> stillRelevant;
    // Set from the main thread, read by attempts on the scheduler's thread
    private volatile boolean stopped;

    /**
     * @param backoffMillis delay before each attempt; its length is the attempt count
     * @param stillRelevant checked before every attempt so a stale event is dropped
     *                      once a newer one replaces it
     */
    public RetryingDispatcher(EventSink sink, Scheduler scheduler, long[] backoffMillis,
                              Predicate<Event> stillRelevant) {
        if (backoffMillis.length == 0) throw new IllegalArgumentException("Needs at least one attempt");
        this.sink = sink;
        this.scheduler = scheduler;
        this.backoffMillis = backoffMillis.clone();
        this.stillRelevant = stillRelevant;
    }

    @Override
    public void dispatch(Event event) {
        scheduleAttempt(event, 0);
    }

    /** Drops every attempt not yet made, including retries already scheduled. */
    @Override
    public void stop() {
        stopped = true;
    }

    private void scheduleAttempt(Event event, int attempt) {
        scheduler.schedule(() -> {
            if (stopped || !stillRelevant.test(event) || !sink.send(event).isRetryable()) return;
            if (attempt + 1 < backoffMillis.length) scheduleAttempt(event, attempt + 1);
        }, backoffMillis[attempt]);
    }
}
