package dev.aman.lookout.ui;

import dev.aman.lookout.R;
import dev.aman.lookout.delivery.DeliveryRecord;
import dev.aman.lookout.delivery.DeliveryResult;
import dev.aman.lookout.event.EventCategory;

/** How each delivery outcome looks and reads, shared by the list and the detail sheet. */
final class DeliveryAppearance {
    private DeliveryAppearance() {}

    static int labelFor(EventCategory category) {
        switch (category) {
            case HEARTBEAT:
                return R.string.category_heartbeat;
            case CHANGE:
                return R.string.category_change;
        }
        throw new IllegalArgumentException("No label for " + category);
    }

    // A failure that will not be tried again reads differently from one that will
    private static boolean isNotResent(DeliveryRecord record) {
        return record.result().isRetryable() && !record.message().category().resentWhenFailed();
    }

    static int labelFor(DeliveryRecord record) {
        if (isNotResent(record)) return R.string.delivery_not_resent;
        switch (record.result()) {
            case DELIVERED:
                return R.string.delivery_delivered;
            case PERMANENT_FAILURE:
                return R.string.delivery_rejected;
            default:
                return R.string.delivery_failed;
        }
    }

    static int responseFor(DeliveryRecord record) {
        if (isNotResent(record)) return R.string.response_not_resent;
        switch (record.result()) {
            case DELIVERED:
                return R.string.response_delivered;
            case PERMANENT_FAILURE:
                return R.string.response_rejected;
            default:
                return R.string.response_failed;
        }
    }

    static int colorFor(DeliveryResult result) {
        switch (result) {
            case DELIVERED:
                return R.color.success;
            case PERMANENT_FAILURE:
                return R.color.error;
            default:
                return R.color.warning;
        }
    }

    static int iconFor(DeliveryRecord record) {
        if (isNotResent(record)) return R.drawable.ic_warning;
        switch (record.result()) {
            case DELIVERED:
                return R.drawable.ic_status_ok;
            case PERMANENT_FAILURE:
                return R.drawable.ic_status_rejected;
            default:
                return R.drawable.ic_status_retry;
        }
    }

    static StatusLine.Kind kindFor(DeliveryResult result) {
        switch (result) {
            case DELIVERED:
                return StatusLine.Kind.SUCCESS;
            case PERMANENT_FAILURE:
                return StatusLine.Kind.ERROR;
            default:
                return StatusLine.Kind.WARNING;
        }
    }
}
