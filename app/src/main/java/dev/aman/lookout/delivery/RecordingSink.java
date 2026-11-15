package dev.aman.lookout.delivery;

import java.util.function.LongSupplier;

/** Passes messages to another sink and records each attempt's outcome, except routine messages that arrived. */
public final class RecordingSink implements EventSink {
    private final EventSink delegate;
    private final DeliveryLog log;
    private final LongSupplier clockMillis;

    public RecordingSink(EventSink delegate, DeliveryLog log, LongSupplier clockMillis) {
        this.delegate = delegate;
        this.log = log;
        this.clockMillis = clockMillis;
    }

    @Override
    public DeliveryResult send(Message message) {
        DeliveryResult result = delegate.send(message);
        if (result != DeliveryResult.DELIVERED || message.listedWhenDelivered()) {
            log.add(new DeliveryRecord(message, result, clockMillis.getAsLong()));
        }
        return result;
    }
}
