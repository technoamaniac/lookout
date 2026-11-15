package dev.aman.lookout.delivery;

import dev.aman.lookout.event.EventCategory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Which categories of event the Deliveries tab lists. Immutable. */
public final class DeliveryFilter {
    /** Changes only: the routine categories are listed once asked for. */
    public static final DeliveryFilter DEFAULT = new DeliveryFilter(EnumSet.of(EventCategory.CHANGE));

    private final Set<EventCategory> shown;

    private DeliveryFilter(Set<EventCategory> shown) {
        this.shown = shown;
    }

    public boolean shows(EventCategory category) {
        return shown.contains(category);
    }

    /** A copy that lists {@code category} or not. */
    public DeliveryFilter with(EventCategory category, boolean shown) {
        Set<EventCategory> next = EnumSet.noneOf(EventCategory.class);
        next.addAll(this.shown);
        if (shown) next.add(category); else next.remove(category);
        return new DeliveryFilter(next);
    }

    public List<DeliveryRecord> apply(List<DeliveryRecord> records) {
        List<DeliveryRecord> kept = new ArrayList<>();
        for (DeliveryRecord record : records) {
            if (shows(record.message().category())) kept.add(record);
        }
        return kept;
    }
}
