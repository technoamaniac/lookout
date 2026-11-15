package dev.aman.lookout.delivery;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.LatestEvents;

import java.util.function.Consumer;

/**
 * Decides which events are sent, given the user's Send reports switch. Every event is recorded, paused or not, so
 * the trackers never fall behind. Pausing makes the events recorded so far stale, which drops their pending
 * retries; resuming sends the newest event of each kind recorded during the pause, so the receiver catches up at
 * once instead of keeping the state from before it.
 */
public final class ReportingGate {
    private final LatestEvents latest;
    private Consumer<Event> sender;
    private boolean on = true;

    public ReportingGate(LatestEvents latest) {
        this.latest = latest;
    }

    /**
     * @param sender where events go, or null while there is no webhook. A first webhook gets the state recorded so
     *               far, since the trackers only report changes and would otherwise wait for the next one.
     */
    public void setSender(Consumer<Event> sender) {
        boolean first = this.sender == null && sender != null;
        this.sender = sender;
        if (first && on) replay();
    }

    public void setOn(boolean on) {
        if (on == this.on) return;
        this.on = on;
        if (!on) {
            latest.forgetAll();
        } else if (sender != null) {
            replay();
        }
    }

    private void replay() {
        latest.all().forEach(sender);
    }

    // Recorded first so an older undelivered event of the same kind goes stale
    public void report(Event event) {
        latest.record(event);
        if (on && sender != null) sender.accept(event);
    }
}
