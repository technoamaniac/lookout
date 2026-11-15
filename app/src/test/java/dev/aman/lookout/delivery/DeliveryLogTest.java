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
        return new DeliveryRecord(Message.of(new AppEvent(pkg, 1), 1000), DeliveryResult.DELIVERED, 1000);
    }

    private static DeliveryRecord beat(long epochSeconds) {
        return new DeliveryRecord(Message.of(new HeartbeatEvent(60, epochSeconds), epochSeconds * 1000),
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
        for (DeliveryRecord kept : log.recent()) summaries.add(kept.message().category() + " " + kept.message().summary());
        assertEquals(List.of("CHANGE c", "CHANGE b", "HEARTBEAT heartbeat", "HEARTBEAT heartbeat"), summaries);
        assertEquals(List.of(4_000L, 3_000L), beatTimes(log));
    }

    @Test
    public void keepsOnlyTheNewestOfEachKindWhenLoading() {
        DeliveryLog log = new DeliveryLog(1, new DeliveryLogStore() {
            @Override
            public List<DeliveryRecord> load() {
                return List.of(beat(2), record("b"), beat(1), record("a"));
            }

            @Override
            public void save(List<DeliveryRecord> newestFirst) {}
        });
        assertEquals(2, log.recent().size());
        assertEquals("b", log.recent().get(1).message().summary());
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

    private static DeliveryRecord failedBeat(long epochSeconds) {
        return new DeliveryRecord(Message.of(new HeartbeatEvent(60, epochSeconds), epochSeconds * 1000),
                DeliveryResult.TEMPORARY_FAILURE, 1000);
    }

    private static final class Saves implements DeliveryLogStore {
        final List<List<DeliveryRecord>> saves = new ArrayList<>();

        @Override
        public List<DeliveryRecord> load() {
            return List.of();
        }

        @Override
        public void save(List<DeliveryRecord> newestFirst) {
            saves.add(newestFirst);
        }
    }

    @Test
    public void deliveredHeartbeatsAreShownButNeverSaved() {
        Saves store = new Saves();
        DeliveryLog log = new DeliveryLog(2, store);

        log.add(beat(1));

        assertEquals(1, log.recent().size());
        assertTrue(store.saves.isEmpty());
    }

    @Test
    public void failedHeartbeatsAreSaved() {
        Saves store = new Saves();
        DeliveryLog log = new DeliveryLog(2, store);

        log.add(failedBeat(1));
        log.add(beat(2));

        assertEquals(1, store.saves.size());
        assertEquals(1, store.saves.get(0).size());
    }

    // A delivered heartbeat can push a saved failed one out of the log, and the file must follow
    @Test
    public void savesWhenAnUnsavedRecordEvictsASavedOne() {
        Saves store = new Saves();
        DeliveryLog log = new DeliveryLog(1, store);
        log.add(failedBeat(1));

        log.add(beat(2));

        assertEquals(2, store.saves.size());
        assertTrue(store.saves.get(1).isEmpty());
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
