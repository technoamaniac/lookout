package dev.aman.lookout.device;

import static org.junit.Assert.assertEquals;

import dev.aman.lookout.event.TvState;

import org.junit.Test;

public class DeviceEventTest {
    @Test
    public void serialisesEachKindWithItsEventName() {
        assertEquals("{\"event\":\"screen_on\"}", new DeviceEvent(DeviceEvent.Kind.SCREEN_ON).fields().compact());
        assertEquals("{\"event\":\"screen_off\"}", new DeviceEvent(DeviceEvent.Kind.SCREEN_OFF).fields().compact());
        assertEquals("{\"event\":\"screensaver_started\"}",
                new DeviceEvent(DeviceEvent.Kind.SCREENSAVER_STARTED).fields().compact());
        assertEquals("{\"event\":\"screensaver_stopped\"}",
                new DeviceEvent(DeviceEvent.Kind.SCREENSAVER_STOPPED).fields().compact());
    }

    @Test
    public void summaryIsTheEventName() {
        assertEquals("screensaver_started", new DeviceEvent(DeviceEvent.Kind.SCREENSAVER_STARTED).summary());
    }

    @Test
    public void eachKindSetsItsPartOfTheTvsState() {
        assertEquals("{\"screen\":\"on\",\"playback\":[]}", stateAfter(DeviceEvent.Kind.SCREEN_ON));
        assertEquals("{\"screen\":\"off\",\"screensaver\":false,\"playback\":[]}",
                stateAfter(DeviceEvent.Kind.SCREEN_OFF));
        assertEquals("{\"screensaver\":true,\"playback\":[]}", stateAfter(DeviceEvent.Kind.SCREENSAVER_STARTED));
        assertEquals("{\"screensaver\":false,\"playback\":[]}", stateAfter(DeviceEvent.Kind.SCREENSAVER_STOPPED));
    }

    @Test
    public void theScreenAndTheScreensaverAreIndependent() {
        TvState state = new DeviceEvent(DeviceEvent.Kind.SCREEN_ON).applyTo(TvState.UNKNOWN);
        state = new DeviceEvent(DeviceEvent.Kind.SCREENSAVER_STARTED).applyTo(state);
        assertEquals("{\"screen\":\"on\",\"screensaver\":true,\"playback\":[]}", state.fields().compact());
    }

    // Some TVs may not send the screensaver's stop when the screen turns off, which would leave "true" in every
    // heartbeat
    @Test
    public void turningTheScreenOffEndsTheScreensaver() {
        TvState state = new DeviceEvent(DeviceEvent.Kind.SCREENSAVER_STARTED).applyTo(TvState.UNKNOWN);
        state = new DeviceEvent(DeviceEvent.Kind.SCREEN_OFF).applyTo(state);
        assertEquals("{\"screen\":\"off\",\"screensaver\":false,\"playback\":[]}", state.fields().compact());
    }

    private static String stateAfter(DeviceEvent.Kind kind) {
        return new DeviceEvent(kind).applyTo(TvState.UNKNOWN).fields().compact();
    }
}
