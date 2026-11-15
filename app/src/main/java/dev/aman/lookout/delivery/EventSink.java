package dev.aman.lookout.delivery;

import dev.aman.lookout.event.Event;

/** Destination for events. */
public interface EventSink {
    DeliveryResult send(Event event);
}
