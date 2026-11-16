package dev.aman.lookout.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.device.DeviceEvent;
import dev.aman.lookout.foreground.AppEvent;
import dev.aman.lookout.heartbeat.HeartbeatEvent;

import org.junit.Test;

public class MessageTest {
    private final AppEvent event = new AppEvent("com.example.a");

    @Test
    public void addsAnIdToTheEventsPayload() {
        Message message = Message.of(event, null, 42_000);
        assertEquals("{\"event\":\"app_changed\",\"package\":\"com.example.a\",\"timestamp\":42,\"id\":\""
                + message.id() + "\"}", message.toJson());
        assertEquals(event.supersessionKey(), message.supersessionKey());
        assertEquals(event.summary(), message.summary());
        assertEquals(42_000, message.occurredAtMillis());
    }

    @Test
    public void addsTheTvsNameBeforeTheTimeWhenGiven() {
        Message message = Message.of(new AppEvent("com.example.a", "com.example.a.Main"), "Den", 42_999);
        assertEquals("{\"event\":\"app_changed\",\"package\":\"com.example.a\",\"activity\":\"com.example.a.Main\","
                + "\"device\":\"Den\",\"timestamp\":42,\"id\":\"" + message.id() + "\"}", message.toJson());
    }

    @Test
    public void leavesOutABlankTvName() {
        assertFalse(Message.of(event, " ", 0).toJson().contains("device"));
    }

    @Test
    public void putsTheSharedFieldsAfterEveryKindOfEvent() {
        Message beat = Message.of(new HeartbeatEvent(300), "Den", 7_000);
        assertEquals("{\"event\":\"heartbeat\",\"interval\":300,\"device\":\"Den\",\"timestamp\":7,\"id\":\""
                + beat.id() + "\"}", beat.toJson());
        Message screen = Message.of(new DeviceEvent(DeviceEvent.Kind.SCREEN_OFF), null, 5_000);
        assertEquals("{\"event\":\"screen_off\",\"timestamp\":5,\"id\":\"" + screen.id() + "\"}", screen.toJson());
    }

    @Test
    public void eachMessageGetsItsOwnId() {
        assertNotEquals(Message.of(event, null, 0).id(), Message.of(event, null, 0).id());
    }

    @Test
    public void aDelayedCopyKeepsTheIdAndLeavesTheOriginalAlone() {
        Message message = Message.of(event, null, 0);
        Message delayed = message.markedDelayed();
        assertEquals(message.id(), delayed.id());
        assertTrue(delayed.toJson().endsWith(",\"delayed\":true}"));
        assertFalse(message.toJson().contains("delayed"));
    }
}
