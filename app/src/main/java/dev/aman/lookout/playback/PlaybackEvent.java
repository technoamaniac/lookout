package dev.aman.lookout.playback;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.JsonFields;

import java.util.Objects;

/** An app starting, pausing or stopping playback, or changing what it plays. */
public final class PlaybackEvent implements Event {
    private static final String TYPE = "playback_changed";

    private final String packageName;
    private final PlayState state;
    private final String title;
    private final String artist;
    private final String device;
    private final long epochSeconds;

    /** @param title what is playing, or null; likewise for {@code artist} */
    public PlaybackEvent(String packageName, PlayState state, String title, String artist, long epochSeconds) {
        this(packageName, state, title, artist, null, epochSeconds);
    }

    // Private so the optional strings are always set by name, never swapped by position
    private PlaybackEvent(String packageName, PlayState state, String title, String artist, String device,
                          long epochSeconds) {
        this.packageName = Objects.requireNonNull(packageName);
        this.state = Objects.requireNonNull(state);
        if (state == PlayState.BUFFERING) throw new IllegalArgumentException("Buffering is not reported");
        this.title = title;
        this.artist = artist;
        this.device = device;
        this.epochSeconds = epochSeconds;
    }

    /** A copy naming this TV so one webhook can serve several; null or blank for none. */
    public PlaybackEvent withDevice(String device) {
        return new PlaybackEvent(packageName, state, title, artist, device, epochSeconds);
    }

    public String packageName() {
        return packageName;
    }

    public PlayState state() {
        return state;
    }

    @Override
    public String summary() {
        return packageName + " " + state.json();
    }

    // Apps play independently, so only a newer event from the same app makes an older one stale
    @Override
    public String supersessionKey() {
        return "playback:" + packageName;
    }

    @Override
    public JsonFields fields() {
        return new JsonFields()
                .string("event", TYPE)
                .string("package", packageName)
                .string("state", state.json())
                .optionalString("title", title)
                .optionalString("artist", artist)
                .optionalString("device", device)
                .number("timestamp", epochSeconds);
    }
}
