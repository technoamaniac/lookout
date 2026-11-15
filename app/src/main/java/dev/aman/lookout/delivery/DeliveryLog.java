package dev.aman.lookout.delivery;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The most recent delivery attempts, newest first, kept across restarts by its store. Thread-safe: the service writes
 * from its worker thread while the settings screen reads from the UI thread and tests webhooks from its own.
 */
public final class DeliveryLog {
    /** How many attempts the Deliveries tab lists. */
    public static final int CAPACITY = 10;

    private final int capacity;
    private final DeliveryLogStore store;
    private final Deque<DeliveryRecord> records = new ArrayDeque<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    /** A log held in memory only. */
    public DeliveryLog(int capacity) {
        this(capacity, DeliveryLogStore.NONE);
    }

    /** Starts with the records the store kept, and saves every change to it. */
    public DeliveryLog(int capacity, DeliveryLogStore store) {
        this.capacity = capacity;
        this.store = store;
        for (DeliveryRecord record : store.load()) {
            if (records.size() == capacity) break;
            records.addLast(record);
        }
    }

    public void add(DeliveryRecord record) {
        synchronized (this) {
            records.addFirst(record);
            if (records.size() > capacity) records.removeLast();
            // Saved under the lock, so two threads' saves cannot land in the wrong order
            store.save(new ArrayList<>(records));
        }
        for (Runnable listener : listeners) listener.run();
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
