package dev.aman.lookout.playback;

import static org.junit.Assert.assertEquals;

import dev.aman.lookout.event.TvState;

import org.junit.Test;

public class PlaybackEventTest {
    private static PlaybackEvent event(String app, PlayState state, MediaDetails details) {
        return new PlaybackEvent(new PlaybackSnapshot(app, state, details));
    }

    private static MediaDetails details(String title, String artist) {
        return MediaDetails.builder().title(title).artist(artist).build();
    }

    @Test
    public void sendsOnlyTheEventName() {
        assertEquals("{\"event\":\"playback_changed\"}",
                event("a", PlayState.PLAYING, details("Song", "Band")).fields().compact());
    }

    @Test
    public void listsInTheTvsStateEverythingTheAppGivesInAFixedOrder() {
        MediaDetails all = MediaDetails.builder().title("Song").artist("Band").album("Record").albumArtist("Group")
                .subtitle("Live").description("A long night").genre("Rock").year(1999).durationMillis(215_900)
                .artwork("content://local/1", "https://example.com/a.jpg").build();
        assertEquals("{\"playback\":[{\"package\":\"a\",\"state\":\"playing\","
                        + "\"title\":\"Song\",\"artist\":\"Band\",\"album\":\"Record\",\"album_artist\":\"Group\","
                        + "\"subtitle\":\"Live\",\"description\":\"A long night\",\"genre\":\"Rock\",\"year\":1999,"
                        + "\"duration\":215,\"artwork\":\"https://example.com/a.jpg\"}]}",
                event("a", PlayState.PLAYING, all).applyTo(TvState.UNKNOWN).fields().compact());
    }

    @Test
    public void summaryNamesTheAppAndItsState() {
        assertEquals("a playing", event("a", PlayState.PLAYING, MediaDetails.NONE).summary());
    }

    @Test
    public void listsTheSessionInTheTvsState() {
        TvState state = event("a", PlayState.PLAYING, MediaDetails.builder().title("T").build())
                .applyTo(TvState.UNKNOWN);
        assertEquals("{\"playback\":[{\"package\":\"a\",\"state\":\"playing\",\"title\":\"T\"}]}",
                state.fields().compact());
    }

    @Test
    public void eachAppKeepsItsOwnPlaceAndANewerEventReplacesOnlyItsOwn() {
        TvState state = event("a", PlayState.PLAYING, MediaDetails.NONE).applyTo(TvState.UNKNOWN);
        state = event("b", PlayState.PLAYING, MediaDetails.NONE).applyTo(state);
        state = event("a", PlayState.PAUSED, MediaDetails.NONE).applyTo(state);
        assertEquals("{\"playback\":[{\"package\":\"a\",\"state\":\"paused\"},"
                + "{\"package\":\"b\",\"state\":\"playing\"}]}", state.fields().compact());
    }

    @Test
    public void aStoppedAppIsNoLongerListed() {
        TvState state = event("a", PlayState.PLAYING, MediaDetails.NONE).applyTo(TvState.UNKNOWN);
        state = event("a", PlayState.STOPPED, MediaDetails.NONE).applyTo(state);
        assertEquals("{\"playback\":[]}", state.fields().compact());
    }

    @Test
    public void prettyJsonHasTheSameFieldsIndentedForReading() {
        assertEquals("{\n  \"event\": \"playback_changed\"\n}",
                event("a", PlayState.PLAYING, MediaDetails.NONE).fields().pretty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void bufferingIsNeverReported() {
        event("a", PlayState.BUFFERING, MediaDetails.NONE);
    }
}
