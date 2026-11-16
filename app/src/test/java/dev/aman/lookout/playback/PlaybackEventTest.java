package dev.aman.lookout.playback;

import static dev.aman.lookout.playback.PlaybackSnapshot.NO_POSITION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class PlaybackEventTest {
    private static PlaybackEvent event(String app, PlayState state, MediaDetails details, long positionSeconds) {
        return new PlaybackEvent(new PlaybackSnapshot(app, state, details, positionSeconds));
    }

    private static MediaDetails details(String title, String artist) {
        return MediaDetails.builder().title(title).artist(artist).build();
    }

    @Test
    public void serialisesWhatIsPlaying() {
        assertEquals("{\"event\":\"playback_changed\",\"package\":\"org.smarttube.stable\",\"state\":\"playing\","
                        + "\"title\":\"Song\",\"artist\":\"Band\"}",
                event("org.smarttube.stable", PlayState.PLAYING, details("Song", "Band"), NO_POSITION).fields().compact());
    }

    @Test
    public void leavesOutTitleAndArtistWhenTheAppGivesNone() {
        assertEquals("{\"event\":\"playback_changed\",\"package\":\"a\",\"state\":\"paused\"}",
                event("a", PlayState.PAUSED, details(null, " "), NO_POSITION).fields().compact());
    }

    @Test
    public void serialisesEverythingTheAppGivesInAFixedOrder() {
        MediaDetails all = MediaDetails.builder().title("Song").artist("Band").album("Record").albumArtist("Group")
                .subtitle("Live").description("A long night").genre("Rock").year(1999).durationMillis(215_900)
                .artwork("content://local/1", "https://example.com/a.jpg").build();
        assertEquals("{\"event\":\"playback_changed\",\"package\":\"a\",\"state\":\"playing\","
                        + "\"title\":\"Song\",\"artist\":\"Band\",\"album\":\"Record\",\"album_artist\":\"Group\","
                        + "\"subtitle\":\"Live\",\"description\":\"A long night\",\"genre\":\"Rock\",\"year\":1999,"
                        + "\"duration\":215,\"artwork\":\"https://example.com/a.jpg\",\"position\":42}",
                event("a", PlayState.PLAYING, all, 42).fields().compact());
    }

    @Test
    public void aPositionOfZeroIsReportedButAnUnknownOneIsNot() {
        assertEquals("{\"event\":\"playback_changed\",\"package\":\"a\",\"state\":\"playing\",\"position\":0}",
                event("a", PlayState.PLAYING, MediaDetails.NONE, 0).fields().compact());
    }

    @Test
    public void summaryNamesTheAppAndItsState() {
        assertEquals("a playing", event("a", PlayState.PLAYING, MediaDetails.NONE, NO_POSITION).summary());
    }

    @Test
    public void eachAppSupersedesOnlyItsOwnEvents() {
        assertEquals("playback:a", event("a", PlayState.PLAYING, MediaDetails.NONE, NO_POSITION).supersessionKey());
        assertNotEquals(event("a", PlayState.PLAYING, MediaDetails.NONE, NO_POSITION).supersessionKey(),
                event("b", PlayState.PLAYING, MediaDetails.NONE, NO_POSITION).supersessionKey());
    }

    @Test
    public void prettyJsonHasTheSameFieldsIndentedForReading() {
        assertEquals("{\n  \"event\": \"playback_changed\",\n  \"package\": \"a\",\n  \"state\": \"playing\"\n}",
                event("a", PlayState.PLAYING, MediaDetails.NONE, NO_POSITION).fields().pretty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void bufferingIsNeverReported() {
        event("a", PlayState.BUFFERING, MediaDetails.NONE, NO_POSITION);
    }
}
