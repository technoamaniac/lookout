package dev.aman.lookout.delivery;

import dev.aman.lookout.foreground.AppEvent;

import java.util.function.LongSupplier;

/** Passes events to another sink and records every attempt's outcome. */
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
    public DeliveryResult send(AppEvent event) {
        DeliveryResult result = delegate.send(event);
        log.add(new DeliveryRecord(event, result, clockMillis.getAsLong()));
        return result;
    }
}
