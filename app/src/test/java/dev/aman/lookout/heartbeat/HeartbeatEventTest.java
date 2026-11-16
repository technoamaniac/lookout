package dev.aman.lookout.heartbeat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.event.EventCategory;

import org.junit.Test;

public class HeartbeatEventTest {
    @Test
    public void carriesOnlyTheInterval() {
        assertEquals("{\"event\":\"heartbeat\",\"interval\":300}", new HeartbeatEvent(300).fields().compact());
    }

    @Test
    public void isNeverResentAfterAFailure() {
        assertFalse(EventCategory.HEARTBEAT.resentWhenFailed());
        assertTrue(EventCategory.CHANGE.resentWhenFailed());
    }

    @Test
    public void isAHeartbeat() {
        assertEquals(EventCategory.HEARTBEAT, new HeartbeatEvent(60).category());
    }
}
