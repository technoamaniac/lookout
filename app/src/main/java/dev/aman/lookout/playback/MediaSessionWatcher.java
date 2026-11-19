package dev.aman.lookout.playback;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reports every active media session whenever one starts, ends, changes state or changes what it
 * plays. Reading sessions needs notification listener access, which the user grants with adb.
 */
public final class MediaSessionWatcher {
    private final MediaSessionManager sessions;
    private final ComponentName access;
    private final Consumer<List<PlaybackSnapshot>> listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<MediaController> watched = new ArrayList<>();
    private final MediaSessionManager.OnActiveSessionsChangedListener onSessionsChanged = this::watch;
    private final MediaController.Callback onSessionChanged = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            publish();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            publish();
        }

        @Override
        public void onSessionDestroyed() {
            publish();
        }
    };

    /** @param access the {@link MediaAccessService} component the user enabled */
    public MediaSessionWatcher(Context context, ComponentName access, Consumer<List<PlaybackSnapshot>> listener) {
        this.sessions = context.getSystemService(MediaSessionManager.class);
        this.access = access;
        this.listener = listener;
    }

    /** Starts watching and reports the sessions open now; false while access has not been granted. */
    public boolean start() {
        stop();
        if (sessions == null) return false;
        try {
            sessions.addOnActiveSessionsChangedListener(onSessionsChanged, access, handler);
            watch(sessions.getActiveSessions(access));
            return true;
        } catch (SecurityException e) {
            // No sessions can be seen, so none are reported: once access is taken away, what played is not
            // claimed to play on in every message
            publish();
            return false;
        }
    }

    public void stop() {
        if (sessions != null) sessions.removeOnActiveSessionsChangedListener(onSessionsChanged);
        unwatchAll();
    }

    private void watch(List<MediaController> controllers) {
        unwatchAll();
        if (controllers != null) {
            for (MediaController controller : controllers) {
                controller.registerCallback(onSessionChanged, handler);
                watched.add(controller);
            }
        }
        publish();
    }

    private void unwatchAll() {
        for (MediaController controller : watched) controller.unregisterCallback(onSessionChanged);
        watched.clear();
    }

    private void publish() {
        List<PlaybackSnapshot> snapshots = new ArrayList<>();
        for (MediaController controller : watched) {
            PlaybackState state = controller.getPlaybackState();
            MediaMetadata metadata = controller.getMetadata();
            snapshots.add(new PlaybackSnapshot(controller.getPackageName(), stateOf(state), detailsOf(metadata),
                    positionOf(state, metadata)));
        }
        listener.accept(snapshots);
    }

    private static MediaDetails detailsOf(MediaMetadata m) {
        if (m == null) return MediaDetails.NONE;
        return MediaDetails.builder()
                .title(firstOf(m, MediaMetadata.METADATA_KEY_TITLE, MediaMetadata.METADATA_KEY_DISPLAY_TITLE))
                .artist(m.getString(MediaMetadata.METADATA_KEY_ARTIST))
                .album(m.getString(MediaMetadata.METADATA_KEY_ALBUM))
                .albumArtist(m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST))
                .subtitle(m.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE))
                .description(m.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION))
                .genre(m.getString(MediaMetadata.METADATA_KEY_GENRE))
                .year(m.getLong(MediaMetadata.METADATA_KEY_YEAR))
                .durationMillis(m.getLong(MediaMetadata.METADATA_KEY_DURATION))
                .artwork(m.getString(MediaMetadata.METADATA_KEY_ART_URI),
                        m.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI),
                        m.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI))
                .build();
    }

    private static String firstOf(MediaMetadata m, String... keys) {
        for (String key : keys) {
            String value = m.getString(key);
            if (value != null && !value.trim().isEmpty()) return value;
        }
        return null;
    }

    // The app reports the position as of its last update, so a playing session has moved on since;
    // the duration caps it for an app that stalls without saying so
    private static long positionOf(PlaybackState state, MediaMetadata metadata) {
        if (state == null || state.getPosition() == PlaybackState.PLAYBACK_POSITION_UNKNOWN) {
            return PlaybackSnapshot.NO_POSITION;
        }
        long millis = state.getPosition();
        if (state.getState() == PlaybackState.STATE_PLAYING && state.getLastPositionUpdateTime() > 0) {
            millis += (long) ((SystemClock.elapsedRealtime() - state.getLastPositionUpdateTime())
                    * state.getPlaybackSpeed());
        }
        long duration = metadata == null ? 0 : metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        if (duration > 0) millis = Math.min(millis, duration);
        return Math.max(0, millis) / 1_000;
    }

    // Everything unlisted means nothing is playing
    private static PlayState stateOf(PlaybackState state) {
        if (state == null) return PlayState.STOPPED;
        switch (state.getState()) {
            case PlaybackState.STATE_PLAYING:
                return PlayState.PLAYING;
            case PlaybackState.STATE_BUFFERING:
                return PlayState.BUFFERING;
            case PlaybackState.STATE_PAUSED:
                return PlayState.PAUSED;
            default:
                return PlayState.STOPPED;
        }
    }
}
