package dev.aman.lookout.playback;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.JsonFields;

import java.util.Objects;

/** An app starting, pausing or stopping playback, or changing what it plays. */
public final class PlaybackEvent implements Event {
    private static final String TYPE = "playback_changed";

    private final PlaybackSnapshot session;

    /** @param session what the app's session says now; never buffering, which is not reported */
    public PlaybackEvent(PlaybackSnapshot session) {
        this.session = Objects.requireNonNull(session);
        if (session.state() == PlayState.BUFFERING) throw new IllegalArgumentException("Buffering is not reported");
    }

    public String packageName() {
        return session.packageName();
    }

    public PlayState state() {
        return session.state();
    }

    @Override
    public String summary() {
        return session.packageName() + " " + session.state().json();
    }

    // Apps play independently, so only a newer event from the same app makes an older one stale
    @Override
    public String supersessionKey() {
        return "playback:" + session.packageName();
    }

    @Override
    public JsonFields fields() {
        JsonFields fields = new JsonFields()
                .string("event", TYPE)
                .string("package", session.packageName())
                .string("state", session.state().json());
        session.details().addTo(fields);
        if (session.positionSeconds() >= 0) {
            fields.number("position", session.positionSeconds());
        }
        return fields;
    }
}
