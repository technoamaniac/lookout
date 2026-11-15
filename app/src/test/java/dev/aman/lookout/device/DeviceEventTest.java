package dev.aman.lookout.device;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DeviceEventTest {
    @Test
    public void serialisesEachKindWithItsEventName() {
        assertEquals("{\"event\":\"screen_on\",\"timestamp\":5}", new DeviceEvent(DeviceEvent.Kind.SCREEN_ON, 5).fields().compact());
        assertEquals("{\"event\":\"screen_off\",\"timestamp\":5}", new DeviceEvent(DeviceEvent.Kind.SCREEN_OFF, 5).fields().compact());
        assertEquals("{\"event\":\"screensaver_started\",\"timestamp\":5}",
                new DeviceEvent(DeviceEvent.Kind.SCREENSAVER_STARTED, 5).fields().compact());
        assertEquals("{\"event\":\"screensaver_stopped\",\"timestamp\":5}",
                new DeviceEvent(DeviceEvent.Kind.SCREENSAVER_STOPPED, 5).fields().compact());
    }

    @Test
    public void includesTheDeviceNameWhenKnown() {
        assertEquals("{\"event\":\"screen_off\",\"device\":\"Den\",\"timestamp\":5}",
                new DeviceEvent(DeviceEvent.Kind.SCREEN_OFF, 5).withDevice("Den").fields().compact());
    }

    @Test
    public void summaryIsTheEventName() {
        assertEquals("screensaver_started", new DeviceEvent(DeviceEvent.Kind.SCREENSAVER_STARTED, 5).summary());
    }

    @Test
    public void prettyJsonHasTheSameFieldsIndentedForReading() {
        assertEquals("{\n  \"event\": \"screen_on\",\n  \"timestamp\": 5\n}",
                new DeviceEvent(DeviceEvent.Kind.SCREEN_ON, 5).fields().pretty());
    }
}
