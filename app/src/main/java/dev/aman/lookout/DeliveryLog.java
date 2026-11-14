package dev.aman.lookout;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The most recent delivery attempts, newest first. Thread-safe: the service writes from its
 * worker thread while the settings screen reads from the UI thread. Held in memory only, so
 * it starts empty when the process restarts.
 */
public final class DeliveryLog {
    private static final int CAPACITY = 50;
    private static final DeliveryLog SHARED = new DeliveryLog(CAPACITY);

    private final int capacity;
    private final Deque<DeliveryRecord> records = new ArrayDeque<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public DeliveryLog(int capacity) {
        this.capacity = capacity;
    }

    /** The log shared by the service and the settings screen, which run in one process. */
    public static DeliveryLog shared() {
        return SHARED;
    }

    public void add(DeliveryRecord record) {
        synchronized (this) {
            records.addFirst(record);
            if (records.size() > capacity) records.removeLast();
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
