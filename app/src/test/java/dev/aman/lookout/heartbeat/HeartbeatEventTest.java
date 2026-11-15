package dev.aman.lookout.heartbeat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class HeartbeatEventTest {
    @Test
    public void carriesOnlyTheIntervalAndTheTime() {
        assertEquals("{\"event\":\"heartbeat\",\"interval\":300,\"timestamp\":42}",
                new HeartbeatEvent(300, 42).fields().compact());
    }

    @Test
    public void namesTheTvWhenAskedTo() {
        assertEquals("{\"event\":\"heartbeat\",\"device\":\"Den\",\"interval\":60,\"timestamp\":7}",
                new HeartbeatEvent(60, 7).withDevice("Den").fields().compact());
    }

    @Test
    public void isNotListedOnceDelivered() {
        assertFalse(new HeartbeatEvent(60, 7).listedWhenDelivered());
    }
}
