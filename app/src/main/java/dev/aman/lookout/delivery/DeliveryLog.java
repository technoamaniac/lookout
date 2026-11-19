package dev.aman.lookout.delivery;

import dev.aman.lookout.event.EventCategory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The most recent delivery attempts, newest first, held in memory only. Each category of event keeps its own newest
 * attempts, so a frequent category such as heartbeats never pushes the others out. Thread-safe: the service writes
 * from its worker thread while the settings screen reads from the UI thread and tests webhooks from its own.
 */
public final class DeliveryLog {
    /** How many attempts of each category the Deliveries tab can list. */
    public static final int CAPACITY = 10;

    private final int capacity;
    private final Deque<DeliveryRecord> records = new ArrayDeque<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public DeliveryLog(int capacity) {
        this.capacity = capacity;
    }

    public void add(DeliveryRecord record) {
        synchronized (this) {
            records.addFirst(record);
            if (countOf(record.message().category()) > capacity) removeOldest(record.message().category());
        }
        for (Runnable listener : listeners) listener.run();
    }

    private int countOf(EventCategory category) {
        int count = 0;
        for (DeliveryRecord record : records) {
            if (record.message().category() == category) count++;
        }
        return count;
    }

    // Newest first, so the oldest of a category is the last one met
    private void removeOldest(EventCategory category) {
        Iterator<DeliveryRecord> oldestFirst = records.descendingIterator();
        while (oldestFirst.hasNext()) {
            if (oldestFirst.next().message().category() == category) {
                oldestFirst.remove();
                return;
            }
        }
    }

    public synchronized List<DeliveryRecord> recent() {
        return new ArrayList<>(records);
    }

    /** Listeners run on whichever thread added the record. */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }
}
