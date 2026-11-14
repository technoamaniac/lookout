package dev.aman.lookout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class ForegroundTrackerTest {
    private final ForegroundTracker tracker =
            new ForegroundTracker(new PackageFilter(List.of("com.android.systemui")));

    @Test
    public void emitsEventOnFirstSightingOfAnApp() {
        AppEvent event = tracker.onWindowChanged("com.example.a", 10).orElseThrow();
        assertEquals("com.example.a", event.packageName());
        assertEquals(10, event.epochSeconds());
    }

    @Test
    public void suppressesRepeatsOfTheCurrentApp() {
        tracker.onWindowChanged("com.example.a", 10);
        assertTrue(tracker.onWindowChanged("com.example.a", 11).isEmpty());
    }

    @Test
    public void emitsAgainWhenReturningToAPreviousApp() {
        tracker.onWindowChanged("com.example.a", 10);
        tracker.onWindowChanged("com.example.b", 11);
        assertTrue(tracker.onWindowChanged("com.example.a", 12).isPresent());
    }

    @Test
    public void ignoredPackagesDoNotDisplaceTheCurrentApp() {
        AppEvent a = tracker.onWindowChanged("com.example.a", 10).orElseThrow();
        assertTrue(tracker.onWindowChanged("com.android.systemui", 11).isEmpty());
        assertTrue(tracker.isCurrent(a));
    }

    @Test
    public void ignoresNullPackage() {
        assertTrue(tracker.onWindowChanged(null, 10).isEmpty());
    }

    @Test
    public void olderEventIsNoLongerCurrentAfterASwitch() {
        AppEvent a = tracker.onWindowChanged("com.example.a", 10).orElseThrow();
        tracker.onWindowChanged("com.example.b", 11);
        assertFalse(tracker.isCurrent(a));
    }

    @Test
    public void emptyFilterAcceptsEverything() {
        ForegroundTracker open = new ForegroundTracker(new PackageFilter(Collections.emptyList()));
        assertTrue(open.onWindowChanged("com.android.systemui", 1).isPresent());
    }
}
