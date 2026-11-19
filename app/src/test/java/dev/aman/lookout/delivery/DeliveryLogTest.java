package dev.aman.lookout.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.event.EventCategory;
import dev.aman.lookout.foreground.AppEvent;
import dev.aman.lookout.heartbeat.HeartbeatEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class DeliveryLogTest {
    private static DeliveryRecord record(String pkg) {
        return new DeliveryRecord(Message.withoutState(new AppEvent(pkg), null, 1000), DeliveryResult.DELIVERED, 1000);
    }

    private static DeliveryRecord beat(long epochSeconds) {
        return new DeliveryRecord(Message.withoutState(new HeartbeatEvent(60), null, epochSeconds * 1000),
                DeliveryResult.DELIVERED, 1000);
    }

    private static List<Long> beatTimes(DeliveryLog log) {
        List<Long> times = new ArrayList<>();
        for (DeliveryRecord kept : log.recent()) {
            if (kept.message().category() == EventCategory.HEARTBEAT) times.add(kept.message().occurredAtMillis());
        }
        return times;
    }

    @Test
    public void startsEmpty() {
        assertTrue(new DeliveryLog(3).recent().isEmpty());
    }

    @Test
    public void listsNewestFirst() {
        DeliveryLog log = new DeliveryLog(3);
        log.add(record("a"));
        log.add(record("b"));
        List<DeliveryRecord> recent = log.recent();
        assertEquals("b", recent.get(0).message().summary());
        assertEquals("a", recent.get(1).message().summary());
    }

    @Test
    public void dropsTheOldestBeyondCapacity() {
        DeliveryLog log = new DeliveryLog(2);
        log.add(record("a"));
        log.add(record("b"));
        log.add(record("c"));
        List<DeliveryRecord> recent = log.recent();
        assertEquals(2, recent.size());
        assertEquals("c", recent.get(0).message().summary());
        assertEquals("b", recent.get(1).message().summary());
    }

    @Test
    public void eachKindKeepsItsOwnNewest() {
        DeliveryLog log = new DeliveryLog(2);
        log.add(record("a"));
        for (int i = 0; i < 5; i++) log.add(beat(i));
        log.add(record("b"));
        log.add(record("c"));

        List<String> summaries = new ArrayList<>();
        for (DeliveryRecord kept : log.recent()) {
            summaries.add(kept.message().category() + " " + kept.message().summary());
        }
        assertEquals(List.of("CHANGE c", "CHANGE b", "HEARTBEAT heartbeat", "HEARTBEAT heartbeat"), summaries);
        assertEquals(List.of(4_000L, 3_000L), beatTimes(log));
    }

    @Test
    public void notifiesListenersUntilRemoved() {
        DeliveryLog log = new DeliveryLog(2);
        AtomicInteger calls = new AtomicInteger();
        Runnable listener = calls::incrementAndGet;
        log.addListener(listener);
        log.add(record("a"));
        log.removeListener(listener);
        log.add(record("b"));
        assertEquals(1, calls.get());
    }
}
