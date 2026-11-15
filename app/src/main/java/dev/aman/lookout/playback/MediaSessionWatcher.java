package dev.aman.lookout.playback;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;

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
            MediaMetadata metadata = controller.getMetadata();
            snapshots.add(new PlaybackSnapshot(controller.getPackageName(), stateOf(controller.getPlaybackState()),
                    metadata == null ? null : metadata.getString(MediaMetadata.METADATA_KEY_TITLE),
                    metadata == null ? null : metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)));
        }
        listener.accept(snapshots);
    }

    // Buffering is the pause before playing, not a stop; everything unlisted means nothing is playing
    private static PlayState stateOf(PlaybackState state) {
        if (state == null) return PlayState.STOPPED;
        switch (state.getState()) {
            case PlaybackState.STATE_PLAYING:
            case PlaybackState.STATE_BUFFERING:
                return PlayState.PLAYING;
            case PlaybackState.STATE_PAUSED:
                return PlayState.PAUSED;
            default:
                return PlayState.STOPPED;
        }
    }
}
