package dev.aman.lookout;

/** Destination for foreground-app events. */
public interface EventSink {
    DeliveryResult send(AppEvent event);
}
