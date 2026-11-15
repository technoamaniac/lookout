package dev.aman.lookout.event;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Remembers the newest event per supersession key, so older undelivered ones can be dropped. Synchronized rather
 * than concurrent because it keeps the order events happened in, which a ConcurrentHashMap cannot.
 */
public final class LatestEvents {
    private final Map<String, Event> latest = new LinkedHashMap<>();

    // Removed first so a re-recorded kind moves to the end, keeping the order of the newest events
    public synchronized void record(Event event) {
        latest.remove(event.supersessionKey());
        latest.put(event.supersessionKey(), event);
    }

    /** The newest event of each kind, oldest first, so replaying them ends in the current state. */
    public synchronized List<Event> all() {
        return new ArrayList<>(latest.values());
    }

    /** Makes every recorded event stale, so retries already scheduled for them are dropped. */
    public synchronized void forgetAll() {
        latest.clear();
    }

    /**
     * True while no newer event with the same key has replaced this one. Compares identity, not
     * content: after A, B, A the first A is stale even though A is in front again.
     */
    public synchronized boolean isCurrent(Event event) {
        return latest.get(event.supersessionKey()) == event;
    }
}
