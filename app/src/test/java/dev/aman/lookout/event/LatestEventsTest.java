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
    public void allHoldsOnlyTheNewestOfEachKind() {
        AppEvent b = new AppEvent("com.example.b", 11);
        DeviceEvent screen = new DeviceEvent(DeviceEvent.Kind.SCREEN_ON, 10);
        latest.record(new AppEvent("com.example.a", 10));
        latest.record(b);
        latest.record(screen);
        assertEquals(2, latest.all().size());
        assertTrue(latest.all().containsAll(List.of(b, screen)));
    }

    @Test
    public void allListsEventsInTheOrderTheirKindLastChanged() {
        AppEvent first = new AppEvent("com.example.a", 10);
        DeviceEvent screen = new DeviceEvent(DeviceEvent.Kind.SCREEN_OFF, 11);
        AppEvent last = new AppEvent("com.example.b", 12);
        latest.record(first);
        latest.record(screen);
        latest.record(last);
        assertEquals(List.of(screen, last), latest.all());
    }

    @Test
    public void forgettingMakesEveryRecordedEventStale() {
        AppEvent app = new AppEvent("com.example.a", 10);
        DeviceEvent screen = new DeviceEvent(DeviceEvent.Kind.SCREEN_ON, 10);
        latest.record(app);
        latest.record(screen);
        latest.forgetAll();
        assertFalse(latest.isCurrent(app));
        assertFalse(latest.isCurrent(screen));
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
