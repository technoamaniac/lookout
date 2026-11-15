package dev.aman.lookout.delivery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Every event Lookout reported, reduced to the newest of each kind, and whether each has settled (delivered, or
 * refused for good) at the current webhook. A newer event replaces an older one of its kind even before that one is
 * sent, so after an outage the receiver gets the current state rather than a backlog. Unsettled messages go out newest
 * first, so the current state arrives before anything older.
 *
 * <p>Keeping settled messages lets a new webhook be sent the whole current state: the trackers only report changes,
 * so it would otherwise know nothing until the next one.
 *
 * <p>Not thread-safe: {@link OutboxSender} confines it to its one thread.
 */
public final class Outbox {
    /** What is saved across restarts: the webhook and each kind's newest message. Retry timing starts afresh. */
    public static final class Snapshot {
        public static final Snapshot EMPTY = new Snapshot(null, List.of());

        private final String webhook;
        private final List<Item> items;

        /** @param items oldest first */
        public Snapshot(String webhook, List<Item> items) {
            this.webhook = webhook;
            this.items = Collections.unmodifiableList(new ArrayList<>(items));
        }

        /** The webhook the settled flags refer to, as text; null when there was none. */
        public String webhook() {
            return webhook;
        }

        public List<Item> items() {
            return items;
        }
    }

    /** One kind's newest message and whether it has settled. */
    public static final class Item {
        private final Message message;
        private final boolean settled;

        public Item(Message message, boolean settled) {
            this.message = Objects.requireNonNull(message);
            this.settled = settled;
        }

        public Message message() {
            return message;
        }

        public boolean settled() {
            return settled;
        }
    }

    private static final class Entry {
        final Message message;
        boolean settled;
        int failures;
        // On the sender's monotonic clock; 0, the start, means due at once
        long dueAtMillis;

        Entry(Message message, boolean settled) {
            this.message = message;
            this.settled = settled;
        }
    }

    private final Backoff backoff;
    // By supersession key, oldest first. Removed and re-added on each event, so the order is that of the newest events.
    private final Map<String, Entry> latest = new LinkedHashMap<>();
    private String webhook;

    public Outbox(Backoff backoff) {
        this.backoff = backoff;
    }

    /** Replaces everything with a saved snapshot; every unsettled message is due at once. */
    public void restore(Snapshot snapshot) {
        latest.clear();
        webhook = snapshot.webhook();
        for (Item item : snapshot.items()) {
            latest.put(item.message().supersessionKey(), new Entry(item.message(), item.settled()));
        }
    }

    public Snapshot snapshot() {
        List<Item> items = new ArrayList<>();
        for (Entry entry : latest.values()) items.add(new Item(entry.message, entry.settled));
        return new Snapshot(webhook, items);
    }

    /** Adds a message to send at once, replacing any older one of its kind, sent or not. */
    public void add(Message message) {
        latest.remove(message.supersessionKey());
        latest.put(message.supersessionKey(), new Entry(message, false));
    }

    /**
     * Points the outbox at a webhook, given as text because {@code URL.equals} resolves host names. A different one
     * has been sent nothing, so every message becomes unsettled and due at once.
     *
     * @return whether the webhook changed
     */
    public boolean useWebhook(String webhook) {
        if (Objects.equals(webhook, this.webhook)) return false;
        this.webhook = webhook;
        for (Entry entry : latest.values()) {
            entry.settled = false;
            entry.failures = 0;
            entry.dueAtMillis = 0;
        }
        return true;
    }

    /** The newest unsettled message whose retry wait is over at {@code nowMillis}. */
    public Optional<Message> nextDue(long nowMillis) {
        List<Entry> newestFirst = new ArrayList<>(latest.values());
        Collections.reverse(newestFirst);
        for (Entry entry : newestFirst) {
            if (!entry.settled && entry.dueAtMillis <= nowMillis) return Optional.of(entry.message);
        }
        return Optional.empty();
    }

    /** When the next retry wait ends, if any unsettled message is waiting. */
    public OptionalLong nextDueAt() {
        OptionalLong next = OptionalLong.empty();
        for (Entry entry : latest.values()) {
            if (!entry.settled && (!next.isPresent() || entry.dueAtMillis < next.getAsLong())) {
                next = OptionalLong.of(entry.dueAtMillis);
            }
        }
        return next;
    }

    /** Marks a message as needing no more attempts; ignored once a newer one replaced it. */
    public void settle(Message message) {
        Entry entry = current(message);
        if (entry != null) entry.settled = true;
    }

    /** Schedules the next attempt at a message after a failed one; ignored once a newer one replaced it. */
    public void failed(Message message, long nowMillis) {
        Entry entry = current(message);
        if (entry == null) return;
        entry.failures++;
        entry.dueAtMillis = nowMillis + backoff.delayAfter(entry.failures);
    }

    /** Ends every retry wait, for when the network comes back. The backoff keeps growing if they fail again. */
    public void retryNow() {
        for (Entry entry : latest.values()) entry.dueAtMillis = 0;
    }

    // Matched by id, since a restored message is a new object
    private Entry current(Message message) {
        Entry entry = latest.get(message.supersessionKey());
        return entry != null && entry.message.id().equals(message.id()) ? entry : null;
    }
}
