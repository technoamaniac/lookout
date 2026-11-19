package dev.aman.lookout.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything Lookout knows about the TV right now, sent with every message so a receiver that missed one is back in
 * step with the next. Immutable: each {@link Event} makes the next state from the last with {@link Event#applyTo}.
 */
public final class TvState {
    /** What is known before any event: nothing in front, the screen and screensaver not yet seen, no playback. */
    public static final TvState UNKNOWN = new TvState(null, null, null, Map.of());

    private final String app;
    // Null until seen: Android cannot be asked whether the screensaver is running, only told when it starts or stops
    private final Boolean screenOn;
    private final Boolean screensaver;
    // By package, in the order each app started playing; the fields are copies nothing else holds
    private final Map<String, JsonFields> playback;

    private TvState(String app, Boolean screenOn, Boolean screensaver, Map<String, JsonFields> playback) {
        this.app = app;
        this.screenOn = screenOn;
        this.screensaver = screensaver;
        this.playback = Collections.unmodifiableMap(playback);
    }

    public TvState withApp(String packageName) {
        return new TvState(packageName, screenOn, screensaver, playback);
    }

    public TvState withScreenOn(boolean on) {
        return new TvState(app, on, screensaver, playback);
    }

    public TvState withScreensaver(boolean running) {
        return new TvState(app, screenOn, running, playback);
    }

    /** @param session what the app plays now, without the {@code event} field; replaces what it played before */
    public TvState withPlayback(String packageName, JsonFields session) {
        Map<String, JsonFields> next = new LinkedHashMap<>(playback);
        next.put(packageName, session.copy());
        return new TvState(app, screenOn, screensaver, next);
    }

    /** The app stopped playing, so it is no longer listed. */
    public TvState withoutPlayback(String packageName) {
        Map<String, JsonFields> next = new LinkedHashMap<>(playback);
        next.remove(packageName);
        return new TvState(app, screenOn, screensaver, next);
    }

    /** The state as fields of a message. What is not known yet is left out, rather than guessed. */
    public JsonFields fields() {
        JsonFields fields = new JsonFields().optionalString("app", app);
        if (screenOn != null) fields.string("screen", screenOn ? "on" : "off");
        if (screensaver != null) fields.bool("screensaver", screensaver);
        List<JsonFields> sessions = new ArrayList<>();
        for (JsonFields session : playback.values()) sessions.add(session.copy());
        return fields.objects("playback", sessions);
    }
}
