package dev.aman.lookout.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import dev.aman.lookout.device.DeviceEvent;
import dev.aman.lookout.event.TvState;
import dev.aman.lookout.foreground.AppEvent;
import dev.aman.lookout.heartbeat.HeartbeatEvent;

import org.junit.Test;

public class MessageTest {
    private final AppEvent event = new AppEvent("com.example.a");
    private final TvState tv = event.applyTo(TvState.UNKNOWN);

    @Test
    public void addsTheTvsStateAndAnIdToTheEventsPayload() {
        Message message = Message.of(event, tv, null, 42_000);
        assertEquals("{\"event\":\"app_changed\",\"tv\":{\"app\":\"com.example.a\","
                + "\"playback\":[]},\"timestamp\":42,\"id\":\"" + message.id() + "\"}",
                message.toJson());
        assertEquals(event.summary(), message.summary());
        assertEquals(42_000, message.occurredAtMillis());
    }

    @Test
    public void addsTheTvsNameBeforeTheTimeWhenGiven() {
        Message message = Message.withoutState(new AppEvent("com.example.a", "com.example.a.Main"), "Den", 42_999);
        assertEquals("{\"event\":\"app_changed\",\"activity\":\"com.example.a.Main\","
                + "\"device\":\"Den\",\"timestamp\":42,\"id\":\"" + message.id() + "\"}", message.toJson());
    }

    @Test
    public void leavesOutABlankTvName() {
        assertFalse(Message.of(event, tv, " ", 0).toJson().contains("device"));
    }

    @Test
    public void putsTheSharedFieldsAfterEveryKindOfEvent() {
        Message beat = Message.of(new HeartbeatEvent(30), tv, "Den", 7_000);
        assertEquals("{\"event\":\"heartbeat\",\"interval\":30,\"tv\":{\"app\":\"com.example.a\","
                + "\"playback\":[]},\"device\":\"Den\",\"timestamp\":7,\"id\":\"" + beat.id() + "\"}", beat.toJson());
        Message screen = Message.withoutState(new DeviceEvent(DeviceEvent.Kind.SCREEN_OFF), null, 5_000);
        assertEquals("{\"event\":\"screen_off\",\"timestamp\":5,\"id\":\"" + screen.id() + "\"}", screen.toJson());
    }

    @Test
    public void aTestMessageSaysNothingAboutTheTv() {
        assertFalse(Message.withoutState(event, null, 0).toJson().contains("\"tv\""));
    }

    @Test
    public void eachMessageGetsItsOwnId() {
        assertNotEquals(Message.of(event, tv, null, 0).id(), Message.of(event, tv, null, 0).id());
    }
}
