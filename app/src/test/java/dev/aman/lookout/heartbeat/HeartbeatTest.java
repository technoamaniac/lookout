package dev.aman.lookout.heartbeat;

import static org.junit.Assert.assertEquals;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.foreground.AppEvent;

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
    private final List<Long> beats = new ArrayList<>();
    private final Heartbeat heartbeat = new Heartbeat(tasks, interval -> {
        beats.add(interval);
        return new AppEvent("beat", 0);
    }, (Event event) -> {});

    @Test
    public void beatsAtOnceThenEveryInterval() {
        heartbeat.setInterval(300);
        tasks.runNext();
        assertEquals(List.of(300L), beats);
        assertEquals(List.of(0L, 300_000L), tasks.delays);

        tasks.runNext();
        assertEquals(List.of(300L, 300L), beats);
        assertEquals(1, tasks.pending.size());
    }

    // A starting service reads its playback after setting the interval; the first beat must include it
    @Test
    public void theFirstBeatWaitsForTheCallerToFinish() {
        heartbeat.setInterval(300);
        assertEquals(List.of(), beats);
        assertEquals(List.of(0L), tasks.delays);
    }

    @Test
    public void theSameIntervalAgainChangesNothing() {
        heartbeat.setInterval(300);
        tasks.runNext();
        heartbeat.setInterval(300);
        assertEquals(1, beats.size());
        assertEquals(1, tasks.pending.size());
    }

    @Test
    public void aNewIntervalReplacesTheOldSchedule() {
        heartbeat.setInterval(300);
        tasks.runNext();
        heartbeat.setInterval(60);
        tasks.runNext();
        assertEquals(List.of(300L, 60L), beats);
        assertEquals(1, tasks.pending.size());
        assertEquals(60_000L, (long) tasks.delays.get(tasks.delays.size() - 1));
    }

    @Test
    public void zeroStopsIt() {
        heartbeat.setInterval(300);
        tasks.runNext();
        heartbeat.setInterval(0);
        assertEquals(1, beats.size());
        assertEquals(0, tasks.pending.size());
    }

    @Test
    public void stoppingBeforeTheFirstBeatSendsNone() {
        heartbeat.setInterval(300);
        heartbeat.stop();
        assertEquals(0, beats.size());
        assertEquals(0, tasks.pending.size());
    }

    @Test
    public void startsAgainAfterStopping() {
        heartbeat.setInterval(300);
        tasks.runNext();
        heartbeat.stop();
        heartbeat.setInterval(300);
        tasks.runNext();
        assertEquals(2, beats.size());
        assertEquals(1, tasks.pending.size());
    }
}
