package dev.aman.lookout.setup;

/** Remembers that the user has been through, or skipped, the setup screens. */
public interface SetupStore {
    boolean isSeen();

    void markSeen();
}
