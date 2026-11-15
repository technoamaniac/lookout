package dev.aman.lookout.delivery;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.LatestEvents;

import java.net.URL;
import java.util.Objects;
import java.util.function.Function;

/**
 * Decides which events are sent, given the webhook and the user's Send reports switch. Every event is recorded,
 * paused or not, so the trackers never fall behind. Each change of webhook or switch stops the previous sender, so
 * retries never run while paused or against an old webhook; then, if reporting is on, the newest unsettled event of
 * each kind goes to a new sender. That one rule covers resuming and a failed delivery that ran out of attempts.
 * Settled means settled for the current webhook, so a new webhook starts with everything unsettled and gets the
 * whole current state: the trackers only report changes, so it would otherwise know nothing until the next one.
 */
public final class ReportingGate {
    /** Where events go: a dispatcher that can be stopped, dropping the attempts it has scheduled. */
    public interface Sender {
        void dispatch(Event event);

        void stop();
    }

    private final LatestEvents latest;
    private final Function<URL, Sender> senders;
    // Compared as text, because URL.equals resolves host names. Read by deliveries on the sender's thread.
    private String webhook;
    private Sender sender;
    private boolean on = true;

    /** @param senders builds the sender for a webhook; it should deliver through {@link #settling} */
    public ReportingGate(LatestEvents latest, Function<URL, Sender> senders) {
        this.latest = latest;
        this.senders = senders;
    }

    /**
     * @param webhook where events go, or null while there is none. Unrelated settings changes call this too, and
     *                change nothing: resending then would duplicate an event whose delivery is still in flight.
     */
    public synchronized void update(URL webhook, boolean on) {
        String text = webhook == null ? null : webhook.toString();
        boolean newWebhook = !Objects.equals(text, this.webhook);
        if (!newWebhook && on == this.on) return;
        if (sender != null) sender.stop();
        if (newWebhook) latest.unsettleAll();
        this.webhook = text;
        this.on = on;
        sender = on && webhook != null ? senders.apply(webhook) : null;
        if (sender != null) latest.unsettled().forEach(sender::dispatch);
    }

    // Recorded first so an older undelivered event of the same kind goes stale
    public synchronized void report(Event event) {
        latest.record(event);
        if (sender != null) sender.dispatch(event);
    }

    /** Wraps the sink that delivers to {@code webhook}, so delivered and refused events are not sent there again. */
    public EventSink settling(URL webhook, EventSink sink) {
        String text = webhook.toString();
        return event -> {
            DeliveryResult result = sink.send(event);
            if (!result.isRetryable()) settle(text, event);
            return result;
        };
    }

    // A delivery to a webhook that was replaced while it was in flight says nothing about the new one
    private synchronized void settle(String webhook, Event event) {
        if (webhook.equals(this.webhook)) latest.markSettled(event);
    }
}
