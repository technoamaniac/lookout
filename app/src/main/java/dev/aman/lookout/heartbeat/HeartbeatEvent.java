package dev.aman.lookout.heartbeat;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.JsonFields;
import dev.aman.lookout.playback.PlaybackSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A periodic "still here" with the current state, so a receiver can tell the TV is reachable and re-sync anything it
 * missed. It carries its own interval, so the receiver can work out when one is overdue without hard-coding it.
 */
public final class HeartbeatEvent implements Event {
    private static final String TYPE = "heartbeat";

    private final long intervalSeconds;
    private final String packageName;
    private final boolean screenOn;
    private final List<PlaybackSnapshot> playback;
    private final String version;
    private final String device;
    private final long epochSeconds;

    /**
     * @param packageName the app in front, or null while Lookout does not know it yet
     * @param playback    every media session Lookout follows, empty without media access
     */
    public HeartbeatEvent(long intervalSeconds, String packageName, boolean screenOn, List<PlaybackSnapshot> playback,
                          String version, long epochSeconds) {
        this(intervalSeconds, packageName, screenOn, playback, version, null, epochSeconds);
    }

    private HeartbeatEvent(long intervalSeconds, String packageName, boolean screenOn, List<PlaybackSnapshot> playback,
                           String version, String device, long epochSeconds) {
        this.intervalSeconds = intervalSeconds;
        this.packageName = packageName;
        this.screenOn = screenOn;
        this.playback = List.copyOf(playback);
        this.version = Objects.requireNonNull(version);
        this.device = device;
        this.epochSeconds = epochSeconds;
    }

    /** A copy naming this TV so one webhook can serve several; null or blank for none. */
    public HeartbeatEvent withDevice(String device) {
        return new HeartbeatEvent(intervalSeconds, packageName, screenOn, playback, version, device, epochSeconds);
    }

    @Override
    public String summary() {
        return TYPE;
    }

    @Override
    public String supersessionKey() {
        return TYPE;
    }

    // One every few minutes would push real events out of the short Deliveries list
    @Override
    public boolean listedWhenDelivered() {
        return false;
    }

    @Override
    public JsonFields fields() {
        List<JsonFields> sessions = new ArrayList<>();
        for (PlaybackSnapshot session : playback) {
            sessions.add(new JsonFields()
                    .string("package", session.packageName())
                    .string("state", session.state().json())
                    .optionalString("title", session.title())
                    .optionalString("artist", session.artist()));
        }
        return new JsonFields()
                .string("event", TYPE)
                .number("interval", intervalSeconds)
                .optionalString("package", packageName)
                .string("screen", screenOn ? "on" : "off")
                .objects("playback", sessions)
                .string("version", version)
                .optionalString("device", device)
                .number("timestamp", epochSeconds);
    }
}
