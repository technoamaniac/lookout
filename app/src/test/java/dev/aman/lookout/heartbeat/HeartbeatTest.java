package dev.aman.lookout.heartbeat;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class HeartbeatTest {
    /** Holds scheduled tasks until the test runs them; the last one is the next beat. */
    private static final class Tasks implements Heartbeat.Scheduler {
        final List<Runnable> pending = new ArrayList<>();
        final List<Long> delays = new ArrayList<>();

        @Override
        public Runnable schedule(Runnable task, long delayMillis) {
            pending.add(task);
            delays.add(delayMillis);
            return () -> pending.remove(task);
        }

        void runNext() {
            pending.remove(pending.size() - 1).run();
        }
    }

    private final Tasks tasks = new Tasks();
    private int beats;
    private final Heartbeat heartbeat = new Heartbeat(tasks, 30_000, () -> beats++);

    @Test
    public void beatsAtOnceThenEveryInterval() {
        heartbeat.restart();
        assertEquals(1, beats);
        assertEquals(List.of(30_000L), tasks.delays);

        tasks.runNext();
        assertEquals(2, beats);
        assertEquals(1, tasks.pending.size());
    }

    @Test
    public void restartingBeatsAtOnceAndReplacesTheOldSchedule() {
        heartbeat.restart();
        heartbeat.restart();
        assertEquals(2, beats);
        assertEquals(1, tasks.pending.size());
    }

    @Test
    public void aFailedBeatDoesNotEndTheBeats() {
        Heartbeat failing = new Heartbeat(tasks, 30_000, () -> {
            throw new IllegalStateException();
        });
        try {
            failing.restart();
        } catch (IllegalStateException expected) {
            // The next beat must be queued anyway
        }
        assertEquals(1, tasks.pending.size());
    }

    @Test
    public void stopEndsTheBeats() {
        heartbeat.restart();
        heartbeat.stop();
        assertEquals(1, beats);
        assertEquals(0, tasks.pending.size());
    }

    @Test
    public void stoppingWhenNotRunningDoesNothing() {
        heartbeat.stop();
        assertEquals(0, beats);
    }

    @Test(expected = IllegalArgumentException.class)
    public void needsAPositiveInterval() {
        new Heartbeat(tasks, 0, () -> {});
    }
}
