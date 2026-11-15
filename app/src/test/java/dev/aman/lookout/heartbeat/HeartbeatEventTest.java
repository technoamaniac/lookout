package dev.aman.lookout.heartbeat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import dev.aman.lookout.playback.PlayState;
import dev.aman.lookout.playback.PlaybackSnapshot;

import java.util.List;

import org.junit.Test;

public class HeartbeatEventTest {
    @Test
    public void carriesTheIntervalAndCurrentState() {
        HeartbeatEvent event = new HeartbeatEvent(300, "org.smarttube.stable", true,
                List.of(new PlaybackSnapshot("org.smarttube.stable", PlayState.PLAYING, "Video", null)), "0.4.0", 42)
                .withDevice("Den");
        assertEquals("{\"event\":\"heartbeat\",\"interval\":300,\"package\":\"org.smarttube.stable\",\"screen\":\"on\","
                + "\"playback\":[{\"package\":\"org.smarttube.stable\",\"state\":\"playing\",\"title\":\"Video\"}],"
                + "\"version\":\"0.4.0\",\"device\":\"Den\",\"timestamp\":42}", event.fields().compact());
    }

    @Test
    public void leavesOutAnUnknownAppAndListsNoPlaybackWithoutSessions() {
        HeartbeatEvent event = new HeartbeatEvent(60, null, false, List.of(), "0.4.0", 7);
        assertEquals("{\"event\":\"heartbeat\",\"interval\":60,\"screen\":\"off\",\"playback\":[],"
                + "\"version\":\"0.4.0\",\"timestamp\":7}", event.fields().compact());
    }

    @Test
    public void isNotListedOnceDelivered() {
        assertFalse(new HeartbeatEvent(60, null, true, List.of(), "0.4.0", 7).listedWhenDelivered());
    }
}
