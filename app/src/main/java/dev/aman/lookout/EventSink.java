package dev.aman.lookout;

/** Destination for foreground-app events. */
public interface EventSink {
    /** Delivers the event; returns false if it should be retried. */
    boolean send(AppEvent event);
}
