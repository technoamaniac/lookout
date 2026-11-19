package dev.aman.lookout.playback;

import java.util.Objects;

/**
 * One app's media session as it looks right now; equal snapshots mean nothing worth reporting changed. The position
 * is not compared: it moves all the time, and is only worth sending along with a change that matters.
 */
public final class PlaybackSnapshot {
    /** The {@code positionSeconds} for a session that does not say. */
    public static final long NO_POSITION = -1;

    private final String packageName;
    private final PlayState state;
    private final MediaDetails details;
    private final long positionSeconds;

    /** @param positionSeconds how far into the media the app is, or {@link #NO_POSITION} */
    public PlaybackSnapshot(String packageName, PlayState state, MediaDetails details, long positionSeconds) {
        this.packageName = Objects.requireNonNull(packageName);
        this.state = Objects.requireNonNull(state);
        this.details = Objects.requireNonNull(details);
        this.positionSeconds = positionSeconds;
    }

    public String packageName() {
        return packageName;
    }

    public PlayState state() {
        return state;
    }

    public MediaDetails details() {
        return details;
    }

    public long positionSeconds() {
        return positionSeconds;
    }

    /** The same session in another state, as when buffering is read as what the app was doing before. */
    PlaybackSnapshot inState(PlayState newState) {
        return new PlaybackSnapshot(packageName, newState, details, positionSeconds);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PlaybackSnapshot)) return false;
        PlaybackSnapshot other = (PlaybackSnapshot) o;
        return packageName.equals(other.packageName) && state == other.state && details.equals(other.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, state, details);
    }
}
