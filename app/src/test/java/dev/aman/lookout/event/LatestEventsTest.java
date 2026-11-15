package dev.aman.lookout.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.device.DeviceEvent;
import dev.aman.lookout.foreground.AppEvent;

import java.util.List;

import org.junit.Test;

public class LatestEventsTest {
    private final LatestEvents latest = new LatestEvents();

    @Test
    public void theNewestEventOfAKindIsCurrent() {
        AppEvent a = new AppEvent("com.example.a", 10);
        latest.record(a);
        assertTrue(latest.isCurrent(a));
    }

    @Test
    public void unsettledHoldsTheNewestOfEachKindInTheOrderItLastChanged() {
        DeviceEvent screen = new DeviceEvent(DeviceEvent.Kind.SCREEN_OFF, 11);
        AppEvent last = new AppEvent("com.example.b", 12);
        latest.record(new AppEvent("com.example.a", 10));
        latest.record(screen);
        latest.record(last);
        assertEquals(List.of(screen, last), latest.unsettled());
    }

    @Test
    public void unsettlingAllBringsSettledEventsBackInOrder() {
        DeviceEvent screen = new DeviceEvent(DeviceEvent.Kind.SCREEN_OFF, 11);
        AppEvent app = new AppEvent("com.example.a", 12);
        latest.record(screen);
        latest.record(app);
        latest.markSettled(screen);
        latest.unsettleAll();
        assertEquals(List.of(screen, app), latest.unsettled());
    }

    @Test
    public void onlyACurrentUnsettledEventIsPending() {
        AppEvent old = new AppEvent("com.example.a", 10);
        AppEvent settled = new AppEvent("com.example.b", 11);
        DeviceEvent screen = new DeviceEvent(DeviceEvent.Kind.SCREEN_ON, 11);
        latest.record(old);
        latest.record(settled);
        latest.record(screen);
        latest.markSettled(settled);
        assertFalse(latest.isPending(old));
        assertFalse(latest.isPending(settled));
        assertTrue(latest.isPending(screen));
    }

    @Test
    public void aSettledEventIsLeftOutButStaysCurrent() {
        AppEvent app = new AppEvent("com.example.a", 10);
        DeviceEvent screen = new DeviceEvent(DeviceEvent.Kind.SCREEN_ON, 10);
        latest.record(app);
        latest.record(screen);
        latest.markSettled(app);
        assertEquals(List.of(screen), latest.unsettled());
        assertTrue(latest.isCurrent(app));
    }

    @Test
    public void settlingAStaleEventLeavesItsReplacementUnsettled() {
        AppEvent old = new AppEvent("com.example.a", 10);
        AppEvent replacement = new AppEvent("com.example.b", 11);
        latest.record(old);
        latest.record(replacement);
        latest.markSettled(old);
        assertEquals(List.of(replacement), latest.unsettled());
    }

    @Test
    public void aNewerEventOfTheSameKindMakesAnOlderOneStale() {
        AppEvent a = new AppEvent("com.example.a", 10);
        latest.record(a);
        latest.record(new AppEvent("com.example.b", 11));
        assertFalse(latest.isCurrent(a));
    }

    @Test
    public void earlierEventIsStaleEvenWhenItsAppReturns() {
        AppEvent first = new AppEvent("com.example.a", 10);
        latest.record(first);
        latest.record(new AppEvent("com.example.b", 11));
        AppEvent second = new AppEvent("com.example.a", 12);
        latest.record(second);
        assertFalse(latest.isCurrent(first));
        assertTrue(latest.isCurrent(second));
    }

    @Test
    public void screenEventsReplaceEachOther() {
        DeviceEvent off = new DeviceEvent(DeviceEvent.Kind.SCREEN_OFF, 10);
        latest.record(off);
        latest.record(new DeviceEvent(DeviceEvent.Kind.SCREEN_ON, 11));
        assertFalse(latest.isCurrent(off));
    }

    @Test
    public void unrelatedKindsDoNotMakeEachOtherStale() {
        AppEvent app = new AppEvent("com.example.a", 10);
        DeviceEvent screenOff = new DeviceEvent(DeviceEvent.Kind.SCREEN_OFF, 11);
        DeviceEvent saverStarted = new DeviceEvent(DeviceEvent.Kind.SCREENSAVER_STARTED, 12);
        latest.record(app);
        latest.record(screenOff);
        latest.record(saverStarted);
        assertTrue(latest.isCurrent(app));
        assertTrue(latest.isCurrent(screenOff));
        assertTrue(latest.isCurrent(saverStarted));
    }
}
