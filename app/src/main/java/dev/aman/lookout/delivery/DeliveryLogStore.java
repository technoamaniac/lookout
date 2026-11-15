package dev.aman.lookout.delivery;

import java.util.List;

/** Keeps the delivery log across restarts. */
public interface DeliveryLogStore {
    /** Keeps nothing, for a log that lives in memory only. */
    DeliveryLogStore NONE = new DeliveryLogStore() {
        @Override
        public List<DeliveryRecord> load() {
            return List.of();
        }

        @Override
        public void save(List<DeliveryRecord> newestFirst) {}
    };

    /** The saved records, newest first; empty when there are none or they cannot be read. */
    List<DeliveryRecord> load();

    /** Never throws: a failed save leaves the previous one in place. */
    void save(List<DeliveryRecord> newestFirst);
}
