package dev.aman.lookout.delivery;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Runs the outbox's tasks on a single-threaded executor, which keeps them in order on one thread. */
public final class ExecutorScheduler implements OutboxSender.Scheduler {
    private final ScheduledExecutorService executor;

    /** @param executor must have exactly one thread */
    public ExecutorScheduler(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }

    @Override
    public Runnable schedule(Runnable task, long delayMillis) {
        ScheduledFuture<?> future = executor.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }
}
