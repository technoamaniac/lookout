package dev.aman.lookout;

import java.util.concurrent.Executor;
import java.util.function.Predicate;

/** Delivers events to a sink off the caller's thread, retrying with backoff. */
public final class RetryingDispatcher {
    /** Pauses between attempts; injectable so tests need not wait. */
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    private final EventSink sink;
    private final Executor executor;
    private final long[] backoffMillis;
    private final Sleeper sleeper;
    private final Predicate<AppEvent> stillRelevant;

    /**
     * @param backoffMillis delay before each attempt; its length is the attempt count
     * @param stillRelevant checked before every attempt so a stale event is dropped
     *                      once a newer one exists
     */
    public RetryingDispatcher(EventSink sink, Executor executor, long[] backoffMillis,
                              Sleeper sleeper, Predicate<AppEvent> stillRelevant) {
        this.sink = sink;
        this.executor = executor;
        this.backoffMillis = backoffMillis.clone();
        this.sleeper = sleeper;
        this.stillRelevant = stillRelevant;
    }

    public void dispatch(AppEvent event) {
        executor.execute(() -> deliver(event));
    }

    private void deliver(AppEvent event) {
        for (long delay : backoffMillis) {
            try {
                if (delay > 0) sleeper.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (!stillRelevant.test(event) || sink.send(event)) return;
        }
    }
}
