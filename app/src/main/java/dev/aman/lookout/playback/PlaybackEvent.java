package dev.aman.lookout.playback;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.JsonFields;

import java.util.Objects;

/** An app starting, pausing or stopping playback, or changing what it plays. */
public final class PlaybackEvent implements Event {
    private static final String TYPE = "playback_changed";

    private final String packageName;
    private final PlayState state;
    private final MediaDetails details;
    private final long positionSeconds;
    private final String device;
    private final long epochSeconds;

    /** @param positionSeconds how far into the media the app is, or {@link PlaybackSnapshot#NO_POSITION} */
    public PlaybackEvent(String packageName, PlayState state, MediaDetails details, long positionSeconds,
                         long epochSeconds) {
        this(packageName, state, details, positionSeconds, null, epochSeconds);
    }

    private PlaybackEvent(String packageName, PlayState state, MediaDetails details, long positionSeconds,
                          String device, long epochSeconds) {
        this.packageName = Objects.requireNonNull(packageName);
        this.state = Objects.requireNonNull(state);
        if (state == PlayState.BUFFERING) throw new IllegalArgumentException("Buffering is not reported");
        this.details = Objects.requireNonNull(details);
        this.positionSeconds = positionSeconds;
        this.device = device;
        this.epochSeconds = epochSeconds;
    }

    /** A copy naming this TV so one webhook can serve several; null or blank for none. */
    public PlaybackEvent withDevice(String device) {
        return new PlaybackEvent(packageName, state, details, positionSeconds, device, epochSeconds);
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
        JsonFields fields = new JsonFields()
                .string("event", TYPE)
                .string("package", packageName)
                .string("state", state.json());
        details.addTo(fields);
        if (positionSeconds >= 0) fields.number("position", positionSeconds);
        return fields.optionalString("device", device).number("timestamp", epochSeconds);
    }
}
