package dev.aman.lookout.delivery;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Runs tasks in order on the test's thread, with a clock the test moves. */
final class ManualScheduler implements OutboxSender.Scheduler {
    private static final class Timed {
        final Runnable task;
        final long atMillis;
        boolean cancelled;

        Timed(Runnable task, long atMillis) {
            this.task = task;
            this.atMillis = atMillis;
        }
    }

    private final Deque<Runnable> queue = new ArrayDeque<>();
    private final List<Timed> timed = new ArrayList<>();
    private long nowMillis;

    long now() {
        return nowMillis;
    }

    @Override
    public void execute(Runnable task) {
        queue.add(task);
    }

    @Override
    public Runnable schedule(Runnable task, long delayMillis) {
        Timed entry = new Timed(task, nowMillis + delayMillis);
        timed.add(entry);
        return () -> entry.cancelled = true;
    }

    /** Runs queued tasks, and those they queue, until none are left. */
    void runQueued() {
        while (!queue.isEmpty()) queue.poll().run();
    }

    /** Moves the clock on, running each timed task as its time comes. */
    void advance(long millis) {
        long until = nowMillis + millis;
        while (true) {
            runQueued();
            Timed next = null;
            for (Timed entry : timed) {
                if (!entry.cancelled && entry.atMillis <= until && (next == null || entry.atMillis < next.atMillis)) {
                    next = entry;
                }
            }
            if (next == null) break;
            timed.remove(next);
            nowMillis = next.atMillis;
            next.task.run();
        }
        nowMillis = until;
    }

    /** Timed tasks not yet run or cancelled. */
    long pendingTimed() {
        return timed.stream().filter(entry -> !entry.cancelled).count();
    }
}
