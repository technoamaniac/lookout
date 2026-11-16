package dev.aman.lookout.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import dev.aman.lookout.foreground.AppEvent;
import dev.aman.lookout.heartbeat.HeartbeatEvent;

import org.junit.Test;

public class RecordingSinkTest {
    private final DeliveryLog log = new DeliveryLog(10);
    private final Message message = Message.of(new AppEvent("com.example.a"), null, 5_000);

    @Test
    public void recordsEachAttemptWithItsResultAndTime() {
        RecordingSink sink = new RecordingSink(e -> DeliveryResult.TEMPORARY_FAILURE, log, () -> 42_000);

        sink.send(message);

        DeliveryRecord record = log.recent().get(0);
        assertSame(message, record.message());
        assertEquals(DeliveryResult.TEMPORARY_FAILURE, record.result());
        assertEquals(42_000, record.attemptedAtMillis());
    }

    @Test
    public void returnsTheWrappedSinksResult() {
        RecordingSink sink = new RecordingSink(e -> DeliveryResult.PERMANENT_FAILURE, log, () -> 0);
        assertEquals(DeliveryResult.PERMANENT_FAILURE, sink.send(message));
    }

    @Test
    public void recordsDeliveredHeartbeatsToo() {
        Message beat = Message.of(new HeartbeatEvent(60), null, 1_000);
        new RecordingSink(m -> DeliveryResult.DELIVERED, log, () -> 0).send(beat);
        assertSame(beat, log.recent().get(0).message());
    }
}
