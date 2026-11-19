package dev.aman.lookout.ui;

import dev.aman.lookout.R;
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
            default:
                throw new IllegalArgumentException("No label for " + category);
        }
    }

    static int labelFor(DeliveryResult result) {
        switch (result) {
            case DELIVERED:
                return R.string.delivery_delivered;
            case TEMPORARY_FAILURE:
                return R.string.delivery_failed;
            case PERMANENT_FAILURE:
                return R.string.delivery_rejected;
            default:
                throw new IllegalArgumentException("No label for " + result);
        }
    }

    static int responseFor(DeliveryResult result) {
        switch (result) {
            case DELIVERED:
                return R.string.response_delivered;
            case TEMPORARY_FAILURE:
                return R.string.response_failed;
            case PERMANENT_FAILURE:
                return R.string.response_rejected;
            default:
                throw new IllegalArgumentException("No response for " + result);
        }
    }

    static int colorFor(DeliveryResult result) {
        switch (result) {
            case DELIVERED:
                return R.color.success;
            case TEMPORARY_FAILURE:
                return R.color.warning;
            case PERMANENT_FAILURE:
                return R.color.error;
            default:
                throw new IllegalArgumentException("No color for " + result);
        }
    }

    // Nothing is retried, so a temporary failure shows a warning rather than a pending state
    static int iconFor(DeliveryResult result) {
        switch (result) {
            case DELIVERED:
                return R.drawable.ic_status_ok;
            case TEMPORARY_FAILURE:
                return R.drawable.ic_warning;
            case PERMANENT_FAILURE:
                return R.drawable.ic_status_rejected;
            default:
                throw new IllegalArgumentException("No icon for " + result);
        }
    }

    static StatusLine.Kind kindFor(DeliveryResult result) {
        switch (result) {
            case DELIVERED:
                return StatusLine.Kind.SUCCESS;
            case TEMPORARY_FAILURE:
                return StatusLine.Kind.WARNING;
            case PERMANENT_FAILURE:
                return StatusLine.Kind.ERROR;
            default:
                throw new IllegalArgumentException("No kind for " + result);
        }
    }
}
