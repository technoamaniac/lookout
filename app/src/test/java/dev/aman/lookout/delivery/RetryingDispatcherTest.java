package dev.aman.lookout.delivery;

import static dev.aman.lookout.delivery.DeliveryResult.DELIVERED;
import static dev.aman.lookout.delivery.DeliveryResult.PERMANENT_FAILURE;
import static dev.aman.lookout.delivery.DeliveryResult.TEMPORARY_FAILURE;
import static org.junit.Assert.assertEquals;

import dev.aman.lookout.foreground.AppEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.junit.Test;

public class RetryingDispatcherTest {
    private static final AppEvent EVENT = new AppEvent("com.example.a", 1);

    /** A single-threaded scheduler on a virtual clock that only moves when told to. */
    private static final class FakeScheduler implements RetryingDispatcher.Scheduler {
        private static final class Task {
            final long dueAt;
            final long order;
            final Runnable runnable;

            Task(long dueAt, long order, Runnable runnable) {
                this.dueAt = dueAt;
                this.order = order;
                this.runnable = runnable;
            }
        }

        // Due time first, then submission order, like a single-threaded ScheduledExecutorService
        private final PriorityQueue<Task> queue = new PriorityQueue<>(
                Comparator.<Task>comparingLong(t -> t.dueAt).thenComparingLong(t -> t.order));
        final List<Long> delays = new ArrayList<>();
        private long now;
        private long submitted;

        @Override
        public void schedule(Runnable task, long delayMillis) {
            delays.add(delayMillis);
            queue.add(new Task(now + delayMillis, submitted++, task));
        }

        /** Runs every task that is due without moving the clock. */
        void runDue() {
            while (!queue.isEmpty() && queue.peek().dueAt <= now) queue.poll().runnable.run();
        }

        /** Moves the clock forward task by task until nothing is left. */
        void runAll() {
            while (!queue.isEmpty()) {
                now = Math.max(now, queue.peek().dueAt);
                runDue();
            }
        }
    }

    private final FakeScheduler scheduler = new FakeScheduler();
    private int attempts;

    private RetryingDispatcher dispatcher(EventSink sink, boolean relevant) {
        return new RetryingDispatcher(sink, scheduler, new long[] {0, 100, 200}, e -> relevant);
    }

    @Test
    public void stopsAfterFirstSuccess() {
        dispatcher(e -> { attempts++; return DELIVERED; }, true).dispatch(EVENT);
        scheduler.runAll();
        assertEquals(1, attempts);
        assertEquals(List.of(0L), scheduler.delays);
    }

    @Test
    public void retriesWithBackoffUntilSuccess() {
        dispatcher(e -> ++attempts == 3 ? DELIVERED : TEMPORARY_FAILURE, true).dispatch(EVENT);
        scheduler.runAll();
        assertEquals(3, attempts);
        assertEquals(List.of(0L, 100L, 200L), scheduler.delays);
    }

    @Test
    public void givesUpAfterTheLastBackoffStep() {
        dispatcher(e -> { attempts++; return TEMPORARY_FAILURE; }, true).dispatch(EVENT);
        scheduler.runAll();
        assertEquals(3, attempts);
    }

    @Test
    public void doesNotRetryAPermanentFailure() {
        dispatcher(e -> { attempts++; return PERMANENT_FAILURE; }, true).dispatch(EVENT);
        scheduler.runAll();
        assertEquals(1, attempts);
        assertEquals(List.of(0L), scheduler.delays);
    }

    @Test
    public void dropsEventsThatHaveBeenSuperseded() {
        dispatcher(e -> { attempts++; return DELIVERED; }, false).dispatch(EVENT);
        scheduler.runAll();
        assertEquals(0, attempts);
    }

    @Test
    public void aNewerEventIsNotHeldUpByAnOlderOnesBackoff() {
        List<String> sent = new ArrayList<>();
        RetryingDispatcher dispatcher = dispatcher(e -> {
            sent.add(e.packageName());
            return TEMPORARY_FAILURE;
        }, true);

        dispatcher.dispatch(new AppEvent("com.example.a", 1));
        scheduler.runDue(); // A fails; its retry waits 100 ms
        dispatcher.dispatch(new AppEvent("com.example.b", 1));
        scheduler.runDue(); // B goes out at once, not after A's backoff

        assertEquals(List.of("com.example.a", "com.example.b"), sent);
    }

    @Test(expected = IllegalArgumentException.class)
    public void needsAtLeastOneAttempt() {
        new RetryingDispatcher(e -> DELIVERED, scheduler, new long[0], e -> true);
    }
}
