package dev.aman.lookout.delivery;

/** Keeps the outbox across restarts. */
public interface OutboxStore {
    /** The saved outbox, or {@link Outbox.Snapshot#EMPTY} when there is none or it cannot be read. */
    Outbox.Snapshot load();

    /** Never throws: a failed save leaves the previous one in place, and delivery carries on from memory. */
    void save(Outbox.Snapshot snapshot);
}
