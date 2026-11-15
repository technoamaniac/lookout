package dev.aman.lookout.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.foreground.AppEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class DeliveryLogTest {
    private static DeliveryRecord record(String pkg) {
        return new DeliveryRecord(Message.of(new AppEvent(pkg, 1), 1000), DeliveryResult.DELIVERED, 1000);
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

    @Test
    public void startsWithWhatTheStoreKeptAndSavesEachChange() {
        List<List<DeliveryRecord>> saves = new ArrayList<>();
        DeliveryRecord kept = record("kept");
        DeliveryLog log = new DeliveryLog(2, new DeliveryLogStore() {
            @Override
            public List<DeliveryRecord> load() {
                return List.of(kept);
            }

            @Override
            public void save(List<DeliveryRecord> newestFirst) {
                saves.add(newestFirst);
            }
        });

        log.add(record("new"));

        assertEquals("kept", log.recent().get(1).message().summary());
        assertEquals(1, saves.size());
        assertEquals(log.recent().size(), saves.get(0).size());
        assertEquals("new", saves.get(0).get(0).message().summary());
    }

    @Test
    public void loadsNoMoreThanItsCapacity() {
        DeliveryLog log = new DeliveryLog(1, new DeliveryLogStore() {
            @Override
            public List<DeliveryRecord> load() {
                return List.of(record("a"), record("b"));
            }

            @Override
            public void save(List<DeliveryRecord> newestFirst) {}
        });
        assertEquals(1, log.recent().size());
        assertEquals("a", log.recent().get(0).message().summary());
    }
}
