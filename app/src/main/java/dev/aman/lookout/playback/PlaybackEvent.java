package dev.aman.lookout.playback;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.JsonFields;
import dev.aman.lookout.event.TvState;

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

    // A stopped app is no longer listed. The position is left out: the state is sent again every heartbeat, long after
    // the position it would carry was true.
    @Override
    public TvState applyTo(TvState state) {
        if (session.state() == PlayState.STOPPED) return state.withoutPlayback(session.packageName());
        return state.withPlayback(session.packageName(), sessionFields(new JsonFields()));
    }

    @Override
    public JsonFields fields() {
        JsonFields fields = sessionFields(new JsonFields().string("event", TYPE));
        if (session.positionSeconds() >= 0) {
            fields.number("position", session.positionSeconds());
        }
        return fields;
    }

    private JsonFields sessionFields(JsonFields fields) {
        fields.string("package", session.packageName()).string("state", session.state().json());
        session.details().addTo(fields);
        return fields;
    }
}
