package dev.aman.lookout.playback;

import java.util.Objects;

/** One app's media session as it looks right now; equal snapshots mean nothing worth reporting changed. */
public final class PlaybackSnapshot {
    private final String packageName;
    private final PlayState state;
    private final String title;
    private final String artist;

    /** @param title what is playing, or null when the app does not say; likewise for {@code artist} */
    public PlaybackSnapshot(String packageName, PlayState state, String title, String artist) {
        this.packageName = Objects.requireNonNull(packageName);
        this.state = Objects.requireNonNull(state);
        this.title = title;
        this.artist = artist;
    }

    public String packageName() {
        return packageName;
    }

    public PlayState state() {
        return state;
    }

    public String title() {
        return title;
    }

    public String artist() {
        return artist;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PlaybackSnapshot)) return false;
        PlaybackSnapshot other = (PlaybackSnapshot) o;
        return packageName.equals(other.packageName) && state == other.state
                && Objects.equals(title, other.title) && Objects.equals(artist, other.artist);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, state, title, artist);
    }
}
