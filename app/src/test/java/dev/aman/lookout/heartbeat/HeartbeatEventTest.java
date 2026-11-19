package dev.aman.lookout.heartbeat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import dev.aman.lookout.event.EventCategory;
import dev.aman.lookout.event.TvState;

import org.junit.Test;

public class HeartbeatEventTest {
    @Test
    public void carriesOnlyTheInterval() {
        assertEquals("{\"event\":\"heartbeat\",\"interval\":300}", new HeartbeatEvent(300).fields().compact());
    }

    @Test
    public void leavesTheTvsStateAsItIs() {
        TvState state = TvState.UNKNOWN.withApp("com.example.a");
        assertSame(state, new HeartbeatEvent(30).applyTo(state));
    }

    @Test
    public void isAHeartbeat() {
        assertEquals(EventCategory.HEARTBEAT, new HeartbeatEvent(60).category());
    }
}
