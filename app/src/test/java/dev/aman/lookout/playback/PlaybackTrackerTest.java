package dev.aman.lookout.playback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class PlaybackTrackerTest {
    private final PlaybackTracker tracker = new PlaybackTracker(() -> "Den");

    private static PlaybackSnapshot snapshot(String app, PlayState state, String title) {
        return new PlaybackSnapshot(app, state, title, null);
    }

    @Test
    public void reportsAnAppThatStartsPlaying() {
        List<PlaybackEvent> events = tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")), 10);
        assertEquals(1, events.size());
        assertEquals("a", events.get(0).packageName());
        assertEquals(PlayState.PLAYING, events.get(0).state());
        assertTrue(events.get(0).toJson().contains("\"title\":\"Song\""));
        assertTrue(events.get(0).toJson().contains("\"device\":\"Den\""));
    }

    @Test
    public void reportsNothingWhenNothingChanged() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")), 10);
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")), 11).isEmpty());
    }

    @Test
    public void reportsPausingAndANewTitle() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")), 10);
        assertEquals(PlayState.PAUSED,
                tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PAUSED, "Song")), 11).get(0).state());
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PAUSED, "Next")), 12)
                .get(0).toJson().contains("\"title\":\"Next\""));
    }

    @Test
    public void doesNotAnnounceAnIdleSessionThatAnAppKeepsOpen() {
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.STOPPED, null)), 10).isEmpty());
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.STOPPED, "Last")), 11).isEmpty());
    }

    @Test
    public void reportsAnAppThatStopsAfterPlaying() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")), 10);
        assertEquals(PlayState.STOPPED,
                tracker.onSessionsChanged(List.of(snapshot("a", PlayState.STOPPED, "Song")), 11).get(0).state());
    }

    @Test
    public void aSessionThatVanishesWhilePlayingHasStopped() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")), 10);
        List<PlaybackEvent> events = tracker.onSessionsChanged(List.of(), 11);
        assertEquals(1, events.size());
        assertEquals(PlayState.STOPPED, events.get(0).state());
        assertTrue(events.get(0).toJson().contains("\"timestamp\":11"));
    }

    @Test
    public void aSessionThatVanishesAfterStoppingIsNotReportedTwice() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")), 10);
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.STOPPED, "Song")), 11);
        assertTrue(tracker.onSessionsChanged(List.of(), 12).isEmpty());
    }

    @Test
    public void followsEachAppSeparately() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")), 10);
        List<PlaybackEvent> events = tracker.onSessionsChanged(
                List.of(snapshot("a", PlayState.PLAYING, "Song"), snapshot("b", PlayState.PLAYING, "Show")), 11);
        assertEquals(1, events.size());
        assertEquals("b", events.get(0).packageName());
    }

    @Test
    public void thePlayingSessionSpeaksForAnAppWithSeveral() {
        List<PlaybackEvent> events = tracker.onSessionsChanged(
                List.of(snapshot("a", PlayState.PAUSED, "Old"), snapshot("a", PlayState.PLAYING, "Now")), 10);
        assertEquals(1, events.size());
        assertEquals(PlayState.PLAYING, events.get(0).state());
    }

    @Test
    public void bufferingWhilePlayingIsStillPlaying() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")), 10);
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.BUFFERING, "Song")), 11).isEmpty());
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")), 12).isEmpty());
    }

    @Test
    public void bufferingWhileStoppedOrPausedDoesNotStartPlayback() {
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.STOPPED, null)), 10).isEmpty());
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.BUFFERING, null)), 11).isEmpty());
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.STOPPED, null)), 12).isEmpty());
        tracker.onSessionsChanged(List.of(snapshot("b", PlayState.PAUSED, "Show")), 13);
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("b", PlayState.BUFFERING, "Show")), 14).isEmpty());
    }

    @Test
    public void anAppThatBuffersFirstIsAnnouncedWhenItStartsPlaying() {
        assertTrue(tracker.onSessionsChanged(List.of(snapshot("a", PlayState.BUFFERING, "Song")), 10).isEmpty());
        List<PlaybackEvent> events = tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")), 11);
        assertEquals(1, events.size());
        assertEquals(PlayState.PLAYING, events.get(0).state());
    }

    @Test
    public void aTitleChangeWhileBufferingIsReportedInTheStateTheAppWasIn() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "One")), 10);
        List<PlaybackEvent> events = tracker.onSessionsChanged(List.of(snapshot("a", PlayState.BUFFERING, "Two")), 11);
        assertEquals(1, events.size());
        assertEquals(PlayState.PLAYING, events.get(0).state());
        assertTrue(events.get(0).toJson().contains("\"title\":\"Two\""));
    }

    @Test
    public void aBufferingSessionSpeaksForItsAppOverAnIdleSiblingInAnyOrder() {
        tracker.onSessionsChanged(List.of(snapshot("a", PlayState.PLAYING, "Song")), 10);
        assertTrue(tracker.onSessionsChanged(
                List.of(snapshot("a", PlayState.STOPPED, null), snapshot("a", PlayState.BUFFERING, "Song")), 11).isEmpty());
        assertTrue(tracker.onSessionsChanged(
                List.of(snapshot("a", PlayState.BUFFERING, "Song"), snapshot("a", PlayState.STOPPED, null)), 12).isEmpty());
    }
}
