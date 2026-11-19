package dev.aman.lookout.playback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.event.TvState;

import org.junit.Test;

import java.util.List;

public class PlaybackTrackerTest {
    private final PlaybackTracker tracker = new PlaybackTracker();

    private static PlaybackSnapshot snapshot(String app, PlayState state, String title) {
        return new PlaybackSnapshot(app, state, MediaDetails.builder().title(title).build());
    }

    @Test
    public void reportsAnAppThatStartsPlaying() {
        List<PlaybackEvent> events = tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")));
        assertEquals(1, events.size());
        assertEquals("a", events.get(0).packageName());
        assertEquals(PlayState.PLAYING, events.get(0).state());
        assertTrue(events.get(0).applyTo(TvState.UNKNOWN).fields().compact().contains("\"title\":\"Song\""));
    }

    @Test
    public void reportsNothingWhenNothingChanged() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")));
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song"))).isEmpty());
    }

    @Test
    public void aNewAlbumOrArtworkIsAChange() {
        tracker.onSessionsChanged(List.of(new PlaybackSnapshot("a", PlayState.PLAYING,
                MediaDetails.builder().title("Song").album("One").build())));
        assertEquals(1, tracker.onSessionsChanged(List.of(new PlaybackSnapshot("a", PlayState.PLAYING,
                MediaDetails.builder().title("Song").album("Two").build()))).size());
    }

    @Test
    public void reportsPausingAndANewTitle() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")));
        assertEquals(PlayState.PAUSED,
                tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PAUSED, "Song"))).get(0).state());
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PAUSED, "Next")))
                .get(0).applyTo(TvState.UNKNOWN).fields().compact().contains("\"title\":\"Next\""));
    }

    @Test
    public void doesNotAnnounceAnIdleSessionThatAnAppKeepsOpen() {
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.STOPPED, null))).isEmpty());
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.STOPPED, "Last"))).isEmpty());
    }

    @Test
    public void reportsAnAppThatStopsAfterPlaying() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")));
        assertEquals(PlayState.STOPPED,
                tracker.onSessionsChanged(List.of(snapshot("a", PlayState.STOPPED, "Song"))).get(0).state());
    }

    @Test
    public void aSessionThatVanishesWhilePlayingHasStopped() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")));
        List<PlaybackEvent> events = tracker.onSessionsChanged(List.of());
        assertEquals(1, events.size());
        assertEquals(PlayState.STOPPED, events.get(0).state());
        assertEquals("a stopped", events.get(0).summary());
    }

    @Test
    public void aSessionThatVanishesAfterStoppingIsNotReportedTwice() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")));
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.STOPPED, "Song")));
        assertTrue(tracker.onSessionsChanged(List.of()).isEmpty());
    }

    @Test
    public void followsEachAppSeparately() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")));
        List<PlaybackEvent> events = tracker.onSessionsChanged(
                List.of(snapshot("a", PlayState.PLAYING, "Song"), snapshot("b", PlayState.PLAYING, "Show")));
        assertEquals(1, events.size());
        assertEquals("b", events.get(0).packageName());
    }

    @Test
    public void thePlayingSessionSpeaksForAnAppWithSeveral() {
        List<PlaybackEvent> events = tracker.onSessionsChanged(
                List.of(snapshot("a", PlayState.PAUSED, "Old"), snapshot("a", PlayState.PLAYING, "Now")));
        assertEquals(1, events.size());
        assertEquals(PlayState.PLAYING, events.get(0).state());
    }

    @Test
    public void bufferingWhilePlayingIsStillPlaying() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")));
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.BUFFERING, "Song"))).isEmpty());
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song"))).isEmpty());
    }

    @Test
    public void bufferingWhileStoppedOrPausedDoesNotStartPlayback() {
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.STOPPED, null))).isEmpty());
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.BUFFERING, null))).isEmpty());
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.STOPPED, null))).isEmpty());
        tracker.onSessionsChanged(List.of(snapshot("b", PlayState.PAUSED, "Show")));
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("b", PlayState.BUFFERING, "Show"))).isEmpty());
    }

    @Test
    public void anAppThatBuffersFirstIsAnnouncedWhenItStartsPlaying() {
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.BUFFERING, "Song"))).isEmpty());
        List<PlaybackEvent> events = tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")));
        assertEquals(1, events.size());
        assertEquals(PlayState.PLAYING, events.get(0).state());
    }

    @Test
    public void aTitleChangeWhileBufferingIsReportedInTheStateTheAppWasIn() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "One")));
        List<PlaybackEvent> events = tracker.onSessionsChanged(List.of(snapshot("a", PlayState.BUFFERING, "Two")));
        assertEquals(1, events.size());
        assertEquals(PlayState.PLAYING, events.get(0).state());
        assertTrue(events.get(0).applyTo(TvState.UNKNOWN).fields().compact().contains("\"title\":\"Two\""));
    }

    @Test
    public void aBufferingSessionSpeaksForItsAppOverAnIdleSiblingInAnyOrder() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")));
        assertTrue(tracker.onSessionsChanged(
                List.of(snapshot("a", PlayState.STOPPED, null), snapshot("a", PlayState.BUFFERING, "Song"))).isEmpty());
        assertTrue(tracker.onSessionsChanged(
                List.of(snapshot("a", PlayState.BUFFERING, "Song"), snapshot("a", PlayState.STOPPED, null))).isEmpty());
    }
}
