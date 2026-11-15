package dev.aman.lookout.playback;

/** What an app's media session says it is doing. */
public enum PlayState {
    /** Waiting for data. Never reported: {@link PlaybackTracker} settles it into one of the others. */
    BUFFERING("buffering"),
    PLAYING("playing"),
    PAUSED("paused"),
    STOPPED("stopped");

    private final String json;

    PlayState(String json) {
        this.json = json;
    }

    String json() {
        return json;
    }
}
