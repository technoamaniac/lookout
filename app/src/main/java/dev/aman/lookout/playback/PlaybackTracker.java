package dev.aman.lookout.playback;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Turns the media sessions seen now into events for what changed since last time. Not thread-safe. */
public final class PlaybackTracker {
    // In the order the sessions were last reported, most important first
    private final Map<String, PlaybackSnapshot> known = new LinkedHashMap<>();

    /** @param sessions every active media session, most important first */
    public List<PlaybackEvent> onSessionsChanged(List<PlaybackSnapshot> sessions) {
        Map<String, PlaybackSnapshot> current = settled(onePerApp(sessions));
        List<PlaybackEvent> events = new ArrayList<>();
        for (PlaybackSnapshot snapshot : current.values()) {
            PlaybackSnapshot before = known.get(snapshot.packageName());
            if (!snapshot.equals(before) && !isStillIdle(before, snapshot)) {
                events.add(new PlaybackEvent(snapshot));
            }
        }
        // A session that disappears while playing has stopped, even though it never said so
        for (PlaybackSnapshot before : known.values()) {
            if (!current.containsKey(before.packageName()) && before.state() != PlayState.STOPPED) {
                events.add(new PlaybackEvent(new PlaybackSnapshot(before.packageName(), PlayState.STOPPED,
                        MediaDetails.NONE, PlaybackSnapshot.NO_POSITION)));
            }
        }
        known.clear();
        known.putAll(current);
        return events;
    }

    // Buffering is a pause for data, not a change of mind: apps pass through it while starting, seeking
    // and shutting down, and announcing it as playing would flap. An app keeps the state it was in.
    private Map<String, PlaybackSnapshot> settled(Map<String, PlaybackSnapshot> sessions) {
        Map<String, PlaybackSnapshot> settled = new LinkedHashMap<>();
        for (PlaybackSnapshot session : sessions.values()) {
            if (session.state() != PlayState.BUFFERING) {
                settled.put(session.packageName(), session);
                continue;
            }
            PlaybackSnapshot before = known.get(session.packageName());
            // Never seen before: say nothing until the app settles on something
            if (before != null) {
                settled.put(session.packageName(), session.inState(before.state()));
            }
        }
        return settled;
    }

    // Many apps keep an idle session open all day; announcing each one would say nothing
    private static boolean isStillIdle(PlaybackSnapshot before, PlaybackSnapshot now) {
        return now.state() == PlayState.STOPPED && (before == null || before.state() == PlayState.STOPPED);
    }

    // An app may hold several sessions; the busiest speaks for it, else the most important
    private static Map<String, PlaybackSnapshot> onePerApp(List<PlaybackSnapshot> sessions) {
        Map<String, PlaybackSnapshot> byApp = new LinkedHashMap<>();
        for (PlaybackSnapshot session : sessions) {
            PlaybackSnapshot kept = byApp.get(session.packageName());
            if (kept == null || activity(session.state()) > activity(kept.state())) {
                byApp.put(session.packageName(), session);
            }
        }
        return byApp;
    }

    // Playing beats buffering, which is about to play, and both beat a paused or idle sibling
    private static int activity(PlayState state) {
        switch (state) {
            case PLAYING:
                return 2;
            case BUFFERING:
                return 1;
            default:
                return 0;
        }
    }
}
