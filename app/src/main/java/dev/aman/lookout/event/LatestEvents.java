package dev.aman.lookout.event;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Remembers the newest event per supersession key, so older undelivered ones can be dropped. */
public final class LatestEvents {
    private final Map<String, Event> latest = new ConcurrentHashMap<>();

    public void record(Event event) {
        latest.put(event.supersessionKey(), event);
    }

    /**
     * True while no newer event with the same key has replaced this one. Compares identity, not
     * content: after A, B, A the first A is stale even though A is in front again.
     */
    public boolean isCurrent(Event event) {
        return latest.get(event.supersessionKey()) == event;
    }
}
