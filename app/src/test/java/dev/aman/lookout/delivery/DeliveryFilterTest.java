package dev.aman.lookout.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.event.EventCategory;
import dev.aman.lookout.foreground.AppEvent;
import dev.aman.lookout.heartbeat.HeartbeatEvent;

import java.util.List;

import org.junit.Test;

public class DeliveryFilterTest {
    private final DeliveryRecord change = record(Message.of(new AppEvent("com.example.a", 1), 1_000));
    private final DeliveryRecord beat = record(Message.of(new HeartbeatEvent(60, 2), 2_000));

    private static DeliveryRecord record(Message message) {
        return new DeliveryRecord(message, DeliveryResult.DELIVERED, 0);
    }

    @Test
    public void listsOnlyChangesByDefault() {
        assertTrue(DeliveryFilter.DEFAULT.shows(EventCategory.CHANGE));
        assertFalse(DeliveryFilter.DEFAULT.shows(EventCategory.HEARTBEAT));
        assertEquals(List.of(change), DeliveryFilter.DEFAULT.apply(List.of(beat, change)));
    }

    @Test
    public void listsAKindOnceAskedAndKeepsTheOrder() {
        DeliveryFilter withBeats = DeliveryFilter.DEFAULT.with(EventCategory.HEARTBEAT, true);
        assertEquals(List.of(beat, change), withBeats.apply(List.of(beat, change)));
    }

    @Test
    public void countsTheCategoriesLeftOut() {
        assertEquals(1, DeliveryFilter.DEFAULT.hiddenCount());
        assertEquals(0, DeliveryFilter.DEFAULT.with(EventCategory.HEARTBEAT, true).hiddenCount());
        assertEquals(2, DeliveryFilter.DEFAULT.with(EventCategory.CHANGE, false).hiddenCount());
    }

    @Test
    public void changingAFilterLeavesTheOriginalAlone() {
        DeliveryFilter withBeats = DeliveryFilter.DEFAULT.with(EventCategory.HEARTBEAT, true);
        assertFalse(DeliveryFilter.DEFAULT.shows(EventCategory.HEARTBEAT));
        assertFalse(withBeats.with(EventCategory.HEARTBEAT, false).shows(EventCategory.HEARTBEAT));
    }
}
