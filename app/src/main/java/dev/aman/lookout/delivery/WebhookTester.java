package dev.aman.lookout.delivery;

import dev.aman.lookout.event.Event;

import java.net.URL;
import java.util.function.Function;
import java.util.function.LongSupplier;

/** Sends one event to a webhook straight away, without retries, recorded so it shows up in the Deliveries tab. */
public final class WebhookTester {
    private final Function<URL, EventSink> sinkFor;
    private final DeliveryLog log;
    private final LongSupplier clockMillis;

    public WebhookTester(DeliveryLog log, LongSupplier clockMillis) {
        this(WebhookSink::new, log, clockMillis);
    }

    WebhookTester(Function<URL, EventSink> sinkFor, DeliveryLog log, LongSupplier clockMillis) {
        this.sinkFor = sinkFor;
        this.log = log;
        this.clockMillis = clockMillis;
    }

    /**
     * Blocks on the network, so call it off the main thread.
     *
     * @param device the TV's name to send, or null to leave it out
     */
    public DeliveryResult test(URL url, Event event, String device) {
        Message message = Message.of(event, device, clockMillis.getAsLong());
        return new RecordingSink(sinkFor.apply(url), log, clockMillis).send(message);
    }
}
