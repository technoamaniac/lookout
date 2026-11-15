package dev.aman.lookout.delivery;

/** Destination for messages. */
public interface EventSink {
    DeliveryResult send(Message message);
}
