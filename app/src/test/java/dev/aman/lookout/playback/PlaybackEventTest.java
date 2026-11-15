package dev.aman.lookout.playback;

import static dev.aman.lookout.playback.PlaybackSnapshot.NO_POSITION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class PlaybackEventTest {
    private static MediaDetails details(String title, String artist) {
        return MediaDetails.builder().title(title).artist(artist).build();
    }

    @Test
    public void serialisesWhatIsPlaying() {
        assertEquals("{\"event\":\"playback_changed\",\"package\":\"org.smarttube.stable\",\"state\":\"playing\","
                        + "\"title\":\"Song\",\"artist\":\"Band\",\"timestamp\":5}",
                new PlaybackEvent("org.smarttube.stable", PlayState.PLAYING, details("Song", "Band"), NO_POSITION, 5).fields().compact());
    }

    @Test
    public void leavesOutTitleAndArtistWhenTheAppGivesNone() {
        assertEquals("{\"event\":\"playback_changed\",\"package\":\"a\",\"state\":\"paused\",\"timestamp\":5}",
                new PlaybackEvent("a", PlayState.PAUSED, details(null, " "), NO_POSITION, 5).fields().compact());
    }

    @Test
    public void serialisesEverythingTheAppGivesInAFixedOrder() {
        MediaDetails all = MediaDetails.builder().title("Song").artist("Band").album("Record").albumArtist("Group")
                .subtitle("Live").description("A long night").genre("Rock").year(1999).durationMillis(215_900)
                .artwork("content://local/1", "https://example.com/a.jpg").build();
        assertEquals("{\"event\":\"playback_changed\",\"package\":\"a\",\"state\":\"playing\","
                        + "\"title\":\"Song\",\"artist\":\"Band\",\"album\":\"Record\",\"album_artist\":\"Group\","
                        + "\"subtitle\":\"Live\",\"description\":\"A long night\",\"genre\":\"Rock\",\"year\":1999,"
                        + "\"duration\":215,\"artwork\":\"https://example.com/a.jpg\",\"position\":42,\"timestamp\":5}",
                new PlaybackEvent("a", PlayState.PLAYING, all, 42, 5).fields().compact());
    }

    @Test
    public void aPositionOfZeroIsReportedButAnUnknownOneIsNot() {
        assertEquals("{\"event\":\"playback_changed\",\"package\":\"a\",\"state\":\"playing\","
                        + "\"position\":0,\"timestamp\":5}",
                new PlaybackEvent("a", PlayState.PLAYING, MediaDetails.NONE, 0, 5).fields().compact());
    }

    @Test
    public void includesTheDeviceNameWhenKnown() {
        assertEquals("{\"event\":\"playback_changed\",\"package\":\"a\",\"state\":\"stopped\",\"device\":\"Den\","
                        + "\"timestamp\":5}",
                new PlaybackEvent("a", PlayState.STOPPED, MediaDetails.NONE, NO_POSITION, 5).withDevice("Den").fields().compact());
    }

    @Test
    public void summaryNamesTheAppAndItsState() {
        assertEquals("a playing", new PlaybackEvent("a", PlayState.PLAYING, MediaDetails.NONE, NO_POSITION, 5).summary());
    }

    @Test
    public void eachAppSupersedesOnlyItsOwnEvents() {
        assertEquals("playback:a", new PlaybackEvent("a", PlayState.PLAYING, MediaDetails.NONE, NO_POSITION, 5).supersessionKey());
        assertNotEquals(new PlaybackEvent("a", PlayState.PLAYING, MediaDetails.NONE, NO_POSITION, 5).supersessionKey(),
                new PlaybackEvent("b", PlayState.PLAYING, MediaDetails.NONE, NO_POSITION, 5).supersessionKey());
    }

    @Test
    public void prettyJsonHasTheSameFieldsIndentedForReading() {
        assertEquals("{\n  \"event\": \"playback_changed\",\n  \"package\": \"a\",\n  \"state\": \"playing\",\n"
                        + "  \"timestamp\": 5\n}",
                new PlaybackEvent("a", PlayState.PLAYING, MediaDetails.NONE, NO_POSITION, 5).fields().pretty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void bufferingIsNeverReported() {
        new PlaybackEvent("a", PlayState.BUFFERING, MediaDetails.NONE, NO_POSITION, 5);
    }
}
