package dev.aman.lookout.playback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class PlaybackEventTest {
    @Test
    public void serialisesWhatIsPlaying() {
        assertEquals("{\"event\":\"playback_changed\",\"package\":\"org.smarttube.stable\",\"state\":\"playing\","
                        + "\"title\":\"Song\",\"artist\":\"Band\",\"timestamp\":5}",
                new PlaybackEvent("org.smarttube.stable", PlayState.PLAYING, "Song", "Band", 5).toJson());
    }

    @Test
    public void leavesOutTitleAndArtistWhenTheAppGivesNone() {
        assertEquals("{\"event\":\"playback_changed\",\"package\":\"a\",\"state\":\"paused\",\"timestamp\":5}",
                new PlaybackEvent("a", PlayState.PAUSED, null, " ", 5).toJson());
    }

    @Test
    public void includesTheDeviceNameWhenKnown() {
        assertEquals("{\"event\":\"playback_changed\",\"package\":\"a\",\"state\":\"stopped\",\"device\":\"Den\","
                        + "\"timestamp\":5}",
                new PlaybackEvent("a", PlayState.STOPPED, null, null, 5).withDevice("Den").toJson());
    }

    @Test
    public void summaryNamesTheAppAndItsState() {
        assertEquals("a playing", new PlaybackEvent("a", PlayState.PLAYING, null, null, 5).summary());
    }

    @Test
    public void eachAppSupersedesOnlyItsOwnEvents() {
        assertEquals("playback:a", new PlaybackEvent("a", PlayState.PLAYING, null, null, 5).supersessionKey());
        assertNotEquals(new PlaybackEvent("a", PlayState.PLAYING, null, null, 5).supersessionKey(),
                new PlaybackEvent("b", PlayState.PLAYING, null, null, 5).supersessionKey());
    }

    @Test
    public void prettyJsonHasTheSameFieldsIndentedForReading() {
        assertEquals("{\n  \"event\": \"playback_changed\",\n  \"package\": \"a\",\n  \"state\": \"playing\",\n"
                        + "  \"timestamp\": 5\n}",
                new PlaybackEvent("a", PlayState.PLAYING, null, null, 5).toPrettyJson());
    }

    @Test(expected = IllegalArgumentException.class)
    public void bufferingIsNeverReported() {
        new PlaybackEvent("a", PlayState.BUFFERING, null, null, 5);
    }
}
