package dev.aman.lookout.playback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Turns the media sessions seen now into events for what changed since last time. Not thread-safe. */
public final class PlaybackTracker {
    private final Supplier<String> device;
    private final Map<String, PlaybackSnapshot> known = new HashMap<>();

    /** @param device supplies the name stamped on each event, or null for none */
    public PlaybackTracker(Supplier<String> device) {
        this.device = device;
    }

    /** @param sessions every active media session, most important first */
    public List<PlaybackEvent> onSessionsChanged(List<PlaybackSnapshot> sessions, long epochSeconds) {
        Map<String, PlaybackSnapshot> current = onePerApp(sessions);
        List<PlaybackEvent> events = new ArrayList<>();
        for (PlaybackSnapshot snapshot : current.values()) {
            PlaybackSnapshot before = known.get(snapshot.packageName());
            if (!snapshot.equals(before) && !isStillIdle(before, snapshot)) {
                events.add(event(snapshot.packageName(), snapshot.state(), snapshot.title(), snapshot.artist(), epochSeconds));
            }
        }
        // A session that disappears while playing has stopped, even though it never said so
        for (PlaybackSnapshot before : known.values()) {
            if (!current.containsKey(before.packageName()) && before.state() != PlayState.STOPPED) {
                events.add(event(before.packageName(), PlayState.STOPPED, null, null, epochSeconds));
            }
        }
        known.clear();
        known.putAll(current);
        return events;
    }

    // Many apps keep an idle session open all day; announcing each one would say nothing
    private static boolean isStillIdle(PlaybackSnapshot before, PlaybackSnapshot now) {
        return now.state() == PlayState.STOPPED && (before == null || before.state() == PlayState.STOPPED);
    }

    // An app may hold several sessions; the one that is playing speaks for it, else the most important
    private static Map<String, PlaybackSnapshot> onePerApp(List<PlaybackSnapshot> sessions) {
        Map<String, PlaybackSnapshot> byApp = new LinkedHashMap<>();
        for (PlaybackSnapshot session : sessions) {
            PlaybackSnapshot kept = byApp.get(session.packageName());
            if (kept == null || (kept.state() != PlayState.PLAYING && session.state() == PlayState.PLAYING)) {
                byApp.put(session.packageName(), session);
            }
        }
        return byApp;
    }

    private PlaybackEvent event(String packageName, PlayState state, String title, String artist, long epochSeconds) {
        return new PlaybackEvent(packageName, state, title, artist, epochSeconds).withDevice(device.get());
    }
}
