package dev.aman.lookout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class RecordingSinkTest {
    private final DeliveryLog log = new DeliveryLog(10);
    private final AppEvent event = new AppEvent("com.example.a", 5);

    @Test
    public void recordsEachAttemptWithItsResultAndTime() {
        RecordingSink sink = new RecordingSink(e -> DeliveryResult.TEMPORARY_FAILURE, log, () -> 42_000);

        sink.send(event);

        DeliveryRecord record = log.recent().get(0);
        assertSame(event, record.event());
        assertEquals(DeliveryResult.TEMPORARY_FAILURE, record.result());
        assertEquals(42_000, record.attemptedAtMillis());
    }

    @Test
    public void returnsTheWrappedSinksResult() {
        RecordingSink sink = new RecordingSink(e -> DeliveryResult.PERMANENT_FAILURE, log, () -> 0);
        assertEquals(DeliveryResult.PERMANENT_FAILURE, sink.send(event));
    }
}
