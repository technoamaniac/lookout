package dev.aman.lookout.event;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TvStateTest {
    // Android cannot say whether the screensaver is running, so a wrong "false" would be repeated every heartbeat
    @Test
    public void leavesOutWhatIsNotKnownYet() {
        assertEquals("{\"playback\":[]}", TvState.UNKNOWN.fields().compact());
    }

    @Test
    public void onceSeenTheScreensaverIsSaidEitherWay() {
        assertEquals("{\"screensaver\":false,\"playback\":[]}",
                TvState.UNKNOWN.withScreensaver(false).fields().compact());
        assertEquals("{\"screensaver\":true,\"playback\":[]}",
                TvState.UNKNOWN.withScreensaver(true).fields().compact());
    }

    @Test
    public void listsEverythingInAFixedOrder() {
        TvState state = TvState.UNKNOWN.withPlayback("p", new JsonFields().string("package", "p"))
                .withScreensaver(true).withScreenOn(true).withApp("a");
        assertEquals("{\"app\":\"a\",\"screen\":\"on\",\"screensaver\":true,\"playback\":[{\"package\":\"p\"}]}",
                state.fields().compact());
    }

    @Test
    public void isNotChangedByAnUpdateOrByTheFieldsItWasGiven() {
        JsonFields session = new JsonFields().string("package", "p");
        TvState state = TvState.UNKNOWN.withPlayback("p", session);
        session.string("state", "playing");
        state.withApp("a");
        state.fields().string("extra", "x");
        assertEquals("{\"playback\":[{\"package\":\"p\"}]}", state.fields().compact());
    }

    @Test
    public void removingAnAppThatIsNotPlayingChangesNothing() {
        assertEquals(TvState.UNKNOWN.fields().compact(), TvState.UNKNOWN.withoutPlayback("p").fields().compact());
    }
}
