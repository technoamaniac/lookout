package dev.aman.lookout.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.foreground.AppEvent;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class DeliveryLogTest {
    private static DeliveryRecord record(String pkg) {
        return new DeliveryRecord(new AppEvent(pkg, 1), DeliveryResult.DELIVERED, 1000);
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
        assertEquals("b", recent.get(0).event().packageName());
        assertEquals("a", recent.get(1).event().packageName());
    }

    @Test
    public void dropsTheOldestBeyondCapacity() {
        DeliveryLog log = new DeliveryLog(2);
        log.add(record("a"));
        log.add(record("b"));
        log.add(record("c"));
        List<DeliveryRecord> recent = log.recent();
        assertEquals(2, recent.size());
        assertEquals("c", recent.get(0).event().packageName());
        assertEquals("b", recent.get(1).event().packageName());
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
