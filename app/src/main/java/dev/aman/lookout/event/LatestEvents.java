package dev.aman.lookout.event;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Remembers the newest event per supersession key, so older undelivered ones can be dropped, and whether each has
 * settled (delivered, or refused for good). Synchronized rather than concurrent because it keeps the order events
 * happened in, which a ConcurrentHashMap cannot.
 */
public final class LatestEvents {
    private static final class Entry {
        final Event event;
        boolean settled;

        Entry(Event event) {
            this.event = event;
        }
    }

    private final Map<String, Entry> latest = new LinkedHashMap<>();

    // Removed first so a re-recorded kind moves to the end, keeping the order of the newest events
    public synchronized void record(Event event) {
        latest.remove(event.supersessionKey());
        latest.put(event.supersessionKey(), new Entry(event));
    }

    /** Marks an event as needing no more attempts; ignored once a newer event of its kind replaced it. */
    public synchronized void markSettled(Event event) {
        Entry entry = latest.get(event.supersessionKey());
        if (entry != null && entry.event == event) entry.settled = true;
    }

    /** Makes every event unsettled again, for a webhook that has not been sent any of them. */
    public synchronized void unsettleAll() {
        for (Entry entry : latest.values()) entry.settled = false;
    }

    /** The newest event of each kind that has not settled, oldest first, so sending them ends in the current state. */
    public synchronized List<Event> unsettled() {
        List<Event> events = new ArrayList<>();
        for (Entry entry : latest.values()) {
            if (!entry.settled) events.add(entry.event);
        }
        return events;
    }

    /**
     * True while no newer event with the same key has replaced this one. Compares identity, not
     * content: after A, B, A the first A is stale even though A is in front again.
     */
    public synchronized boolean isCurrent(Event event) {
        Entry entry = latest.get(event.supersessionKey());
        return entry != null && entry.event == event;
    }

    /** Current and not settled yet, so an attempt queued behind a delivery of the same event is skipped. */
    public synchronized boolean isPending(Event event) {
        Entry entry = latest.get(event.supersessionKey());
        return entry != null && entry.event == event && !entry.settled;
    }
}
