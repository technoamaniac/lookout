package dev.aman.lookout.device;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
    public void screenAndScreensaverEventsDoNotReplaceEachOther() {
        assertEquals(new DeviceEvent(DeviceEvent.Kind.SCREEN_ON).supersessionKey(),
                new DeviceEvent(DeviceEvent.Kind.SCREEN_OFF).supersessionKey());
        assertEquals(new DeviceEvent(DeviceEvent.Kind.SCREENSAVER_STARTED).supersessionKey(),
                new DeviceEvent(DeviceEvent.Kind.SCREENSAVER_STOPPED).supersessionKey());
        assertNotEquals(new DeviceEvent(DeviceEvent.Kind.SCREEN_ON).supersessionKey(),
                new DeviceEvent(DeviceEvent.Kind.SCREENSAVER_STARTED).supersessionKey());
    }
}
