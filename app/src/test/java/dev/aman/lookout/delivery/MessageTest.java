package dev.aman.lookout.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.foreground.AppEvent;

import org.junit.Test;

public class MessageTest {
    private final AppEvent event = new AppEvent("com.example.a", 42);

    @Test
    public void addsAnIdToTheEventsPayload() {
        Message message = Message.of(event, 42_000);
        assertEquals("{\"event\":\"app_changed\",\"package\":\"com.example.a\",\"timestamp\":42,\"id\":\""
                + message.id() + "\"}", message.toJson());
        assertEquals(event.supersessionKey(), message.supersessionKey());
        assertEquals(event.summary(), message.summary());
        assertEquals(42_000, message.occurredAtMillis());
    }

    @Test
    public void eachMessageGetsItsOwnId() {
        assertNotEquals(Message.of(event, 0).id(), Message.of(event, 0).id());
    }

    @Test
    public void aDelayedCopyKeepsTheIdAndLeavesTheOriginalAlone() {
        Message message = Message.of(event, 0);
        Message delayed = message.markedDelayed();
        assertEquals(message.id(), delayed.id());
        assertTrue(delayed.toJson().endsWith(",\"delayed\":true}"));
        assertFalse(message.toJson().contains("delayed"));
    }
}
