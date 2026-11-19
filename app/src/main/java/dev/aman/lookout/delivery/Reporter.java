package dev.aman.lookout.delivery;

import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.TvState;
import dev.aman.lookout.heartbeat.Heartbeat;
import dev.aman.lookout.heartbeat.HeartbeatEvent;

import java.net.URL;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Keeps the receiver in step with the TV. Every event updates the TV's state, and every message carries that whole
 * state: each change is sent once, as it happens, and a heartbeat sends the state again every
 * {@link #HEARTBEAT_SECONDS}. Nothing is retried or saved. A failed message is healed by the next one, so the
 * receiver is never more than a heartbeat behind once it can be reached, even if it lost its own state meanwhile.
 *
 * <p>This is the level-triggered design of device firmware such as Tasmota: an outbox that saves and retries each
 * event could only ever bring the receiver up to the same current state, with far more moving parts.
 *
 * <p>Everything runs on the scheduler's one thread, so messages carry their states in the order the events happened,
 * and a webhook change never interleaves with a delivery in flight. That thread's clock stops in deep sleep, so a TV
 * in deep standby goes quiet until it wakes.
 */
public final class Reporter {
    /** Runs tasks one at a time, in order, on one thread; injectable so tests control time. */
    public interface Scheduler {
        void execute(Runnable task);

        /** @return cancels the task if it has not started */
        Runnable schedule(Runnable task, long delayMillis);
    }

    /** How often the state is sent again; also the longest a receiver stays out of step once it can be reached. */
    public static final long HEARTBEAT_SECONDS = 30;

    private final Function<URL, EventSink> sinks;
    private final Scheduler scheduler;
    private final Supplier<String> deviceName;
    private final LongSupplier wallMillis;
    private final Heartbeat heartbeat;
    // Only touched on the scheduler's thread
    private TvState state = TvState.UNKNOWN;
    private String webhook;
    private EventSink sink;
    private boolean on;
    // Whether the receiver may have missed something: the last message to it was not delivered
    private boolean outOfStep;

    /**
     * @param sinks      builds the sink that delivers to a webhook
     * @param deviceName the TV's name to put in each message, or null to leave it out; asked per message, on the
     *                   scheduler's thread, so renaming the TV or changing the setting applies at once
     * @param wallMillis the wall clock, for when events happened
     */
    public Reporter(Function<URL, EventSink> sinks, Scheduler scheduler, Supplier<String> deviceName,
                    LongSupplier wallMillis) {
        this.sinks = sinks;
        this.scheduler = scheduler;
        this.deviceName = deviceName;
        this.wallMillis = wallMillis;
        heartbeat = new Heartbeat(scheduler::schedule, HEARTBEAT_SECONDS * 1_000, this::beat);
    }

    /** Records the event in the TV's state, and sends it if reporting is on; while off, the next heartbeat tells. */
    public void report(Event event) {
        // Timed here, when it happened; the message is made on the worker, since the first random UUID can be slow to
        // make and the TV's name is read from system settings
        long occurredAtMillis = wallMillis.getAsLong();
        scheduler.execute(() -> {
            state = event.applyTo(state);
            if (isSending()) send(event, occurredAtMillis);
        });
    }

    /**
     * Starting to send, to a new webhook or after a pause, sends the state at once, so the receiver need not wait
     * for the next change or heartbeat.
     *
     * @param webhook where events go, or null while there is none. Unrelated settings changes call this too, and
     *                change nothing.
     * @param on      the Enabled switch
     */
    public void update(URL webhook, boolean on) {
        scheduler.execute(() -> {
            boolean wasSending = isSending();
            boolean newWebhook = useWebhook(webhook);
            this.on = on;
            if (!isSending()) {
                heartbeat.stop();
            } else if (newWebhook || !wasSending) {
                heartbeat.restart();
            }
        });
    }

    /**
     * Sends the state at once when the network comes back and the receiver missed something, rather than at the next
     * heartbeat. Android also says a network is available when the watch starts and when the TV switches networks;
     * a receiver that got the last message is in step, so those send nothing.
     */
    public void networkAvailable() {
        scheduler.execute(() -> {
            if (isSending() && outOfStep) heartbeat.restart();
        });
    }

    /** @return whether it differs from the one in use */
    private boolean useWebhook(URL url) {
        // As text, because URL.equals resolves host names
        String text = url == null ? null : url.toString();
        if (Objects.equals(text, webhook)) return false;
        webhook = text;
        sink = url == null ? null : sinks.apply(url);
        return true;
    }

    private boolean isSending() {
        return on && sink != null;
    }

    private void beat() {
        send(new HeartbeatEvent(HEARTBEAT_SECONDS), wallMillis.getAsLong());
    }

    private void send(Event event, long occurredAtMillis) {
        DeliveryResult result = sink.send(Message.of(event, state, deviceName.get(), occurredAtMillis));
        outOfStep = result != DeliveryResult.DELIVERED;
    }
}
