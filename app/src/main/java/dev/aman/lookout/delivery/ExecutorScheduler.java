package dev.aman.lookout.delivery;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Runs the reporter's tasks on a single-threaded executor, which keeps them in order on one thread. */
public final class ExecutorScheduler implements Reporter.Scheduler {
    private final ScheduledExecutorService executor;

    /** @param executor must have exactly one thread */
    public ExecutorScheduler(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void execute(Runnable task) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            // The service shut the executor down; a system callback already in flight has nothing left to report to
        }
    }

    @Override
    public Runnable schedule(Runnable task, long delayMillis) {
        try {
            ScheduledFuture<?> future = executor.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
            return () -> future.cancel(false);
        } catch (RejectedExecutionException e) {
            // The service shut the executor down; nothing is left to run it
            return () -> {};
        }
    }
}
