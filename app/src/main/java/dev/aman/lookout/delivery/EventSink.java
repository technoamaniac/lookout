package dev.aman.lookout.delivery;

import dev.aman.lookout.foreground.AppEvent;

/** Destination for foreground-app events. */
public interface EventSink {
    DeliveryResult send(AppEvent event);
}
