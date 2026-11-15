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
        }
        throw new IllegalArgumentException("No label for " + category);
    }

    static int labelFor(DeliveryResult result) {
        switch (result) {
            case DELIVERED:
                return R.string.delivery_delivered;
            case PERMANENT_FAILURE:
                return R.string.delivery_rejected;
            default:
                return R.string.delivery_failed;
        }
    }

    static int responseFor(DeliveryResult result) {
        switch (result) {
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

    static int iconFor(DeliveryResult result) {
        switch (result) {
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
