package dev.aman.lookout.delivery;

import dev.aman.lookout.event.Event;

import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * Works through the {@link Outbox}: every event goes into it and is saved first, then delivered newest first, one at a
 * time. Failed deliveries are retried with a growing wait that never gives up while the event is current, and at once
 * when the network comes back. The Send to webhook switch only pauses delivery; events are still recorded, so turning
 * it back on sends what changed meanwhile.
 *
 * <p>Everything, deliveries included, runs on the scheduler's one thread, so a webhook change or a new event can never
 * interleave with a delivery in flight. Each delivery is its own task, so an event reported during a catch-up is
 * recorded before the next one is picked and goes out first.
 */
public final class OutboxSender {
    /** Runs tasks one at a time, in order, on one thread; injectable so tests control time. */
    public interface Scheduler {
        void execute(Runnable task);

        /** @return cancels the task if it has not started */
        Runnable schedule(Runnable task, long delayMillis);
    }

    /** Sent this long after the event happened, a message is flagged as delayed. */
    static final long LATE_AFTER_MILLIS = 60_000;

    private final Outbox outbox;
    private final OutboxStore store;
    private final Function<URL, EventSink> sinks;
    private final Scheduler scheduler;
    private final LongSupplier elapsedMillis;
    private final LongSupplier wallMillis;
    // Only touched on the scheduler's thread
    private String sinkWebhook;
    private EventSink sink;
    private boolean on;
    private boolean deliveryQueued;
    private Runnable cancelWake = () -> {};

    /**
     * @param sinks         builds the sink that delivers to a webhook
     * @param elapsedMillis a clock that never jumps, for retry waits
     * @param wallMillis    the wall clock, for when events happened
     */
    public OutboxSender(Outbox outbox, OutboxStore store, Function<URL, EventSink> sinks, Scheduler scheduler,
                        LongSupplier elapsedMillis, LongSupplier wallMillis) {
        this.outbox = outbox;
        this.store = store;
        this.sinks = sinks;
        this.scheduler = scheduler;
        this.elapsedMillis = elapsedMillis;
        this.wallMillis = wallMillis;
    }

    /** Loads the saved outbox; call once, before anything else, so it runs first. */
    public void start() {
        scheduler.execute(() -> outbox.restore(store.load()));
    }

    // Timed here, when it happened; the id is made on the worker, since the first random UUID can be slow to make
    public void report(Event event) {
        long occurredAtMillis = wallMillis.getAsLong();
        scheduler.execute(() -> {
            outbox.add(Message.of(event, occurredAtMillis));
            store.save(outbox.snapshot());
            queueDelivery();
        });
    }

    /**
     * @param webhook where events go, or null while there is none. Unrelated settings changes call this too, and
     *                change nothing.
     * @param on      the Send to webhook switch
     */
    public void update(URL webhook, boolean on) {
        scheduler.execute(() -> {
            // As text, because URL.equals resolves host names
            String text = webhook == null ? null : webhook.toString();
            if (outbox.useWebhook(text)) store.save(outbox.snapshot());
            // Tracked apart from the outbox's webhook: a restored outbox has one before the first update builds a sink
            if (!Objects.equals(text, sinkWebhook)) {
                sinkWebhook = text;
                sink = webhook == null ? null : sinks.apply(webhook);
            }
            // Resuming catches up at once rather than at the end of a wait that began before the pause
            if (on && !this.on) outbox.retryNow();
            this.on = on;
            queueDelivery();
        });
    }

    /**
     * Sends an event once, now, if reporting is on: never saved, retried or replayed, for a message that means
     * something only when it is on time, such as a heartbeat.
     */
    public void sendNow(Event event) {
        long occurredAtMillis = wallMillis.getAsLong();
        scheduler.execute(() -> {
            if (!on || sink == null) return;
            // It just reached the webhook, so anything waiting out a retry would too
            if (sink.send(Message.of(event, occurredAtMillis)) == DeliveryResult.DELIVERED) {
                outbox.retryNow();
                queueDelivery();
            }
        });
    }

    /** Retries at once whatever is waiting, for when the network comes back. */
    public void networkAvailable() {
        scheduler.execute(() -> {
            outbox.retryNow();
            queueDelivery();
        });
    }

    // One queued delivery at a time, so several triggers do not each start their own run through the outbox
    private void queueDelivery() {
        if (deliveryQueued) return;
        deliveryQueued = true;
        scheduler.execute(this::deliverNext);
    }

    private void deliverNext() {
        deliveryQueued = false;
        cancelWake.run();
        if (!on || sink == null) return;
        Optional<Message> next = outbox.nextDue(elapsedMillis.getAsLong());
        if (!next.isPresent()) {
            wakeForNextRetry();
            return;
        }
        Message message = next.get();
        DeliveryResult result = sink.send(isLate(message) ? message.markedDelayed() : message);
        if (result.isRetryable()) {
            outbox.failed(message, elapsedMillis.getAsLong());
        } else {
            outbox.settle(message);
            store.save(outbox.snapshot());
        }
        queueDelivery();
    }

    private boolean isLate(Message message) {
        return wallMillis.getAsLong() - message.occurredAtMillis() >= LATE_AFTER_MILLIS;
    }

    private void wakeForNextRetry() {
        OptionalLong dueAt = outbox.nextDueAt();
        if (!dueAt.isPresent()) return;
        long delay = Math.max(0, dueAt.getAsLong() - elapsedMillis.getAsLong());
        cancelWake = scheduler.schedule(this::queueDelivery, delay);
    }
}
