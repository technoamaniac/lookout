package dev.aman.lookout.playback;

import java.util.Objects;

/**
 * One app's media session as it looks right now; equal snapshots mean nothing worth reporting changed.
 */
public final class PlaybackSnapshot {
    private final String packageName;
    private final PlayState state;
    private final MediaDetails details;

    public PlaybackSnapshot(String packageName, PlayState state, MediaDetails details) {
        this.packageName = Objects.requireNonNull(packageName);
        this.state = Objects.requireNonNull(state);
        this.details = Objects.requireNonNull(details);
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

    /** The same session in another state, as when buffering is read as what the app was doing before. */
    PlaybackSnapshot inState(PlayState newState) {
        return new PlaybackSnapshot(packageName, newState, details);
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
