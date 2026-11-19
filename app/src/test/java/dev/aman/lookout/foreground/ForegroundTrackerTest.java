package dev.aman.lookout.foreground;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class ForegroundTrackerTest {
    private final ForegroundTracker tracker = new ForegroundTracker(new PackageFilter(List.of("com.android.systemui")));

    @Test
    public void emitsEventOnFirstSightingOfAnApp() {
        AppEvent event = tracker.onWindowChanged("com.example.a").orElseThrow();
        assertEquals("com.example.a", event.packageName());
    }

    @Test
    public void aNewFilterKeepsTheAppInFrontSoItIsNotReportedAgain() {
        tracker.onWindowChanged("com.example.a");
        tracker.setFilter(new PackageFilter(List.of("com.example.launcher")));
        assertTrue(tracker.onWindowChanged("com.example.a").isEmpty());
    }

    @Test
    public void aNewFilterApplies() {
        tracker.setFilter(new PackageFilter(List.of("com.example.launcher")));
        assertTrue(tracker.onWindowChanged("com.example.launcher").isEmpty());
        assertTrue(tracker.onWindowChanged("com.example.a").isPresent());
    }

    @Test
    public void suppressesRepeatsOfTheCurrentApp() {
        tracker.onWindowChanged("com.example.a");
        assertTrue(tracker.onWindowChanged("com.example.a").isEmpty());
    }

    @Test
    public void emitsAgainWhenReturningToAPreviousApp() {
        tracker.onWindowChanged("com.example.a");
        tracker.onWindowChanged("com.example.b");
        assertTrue(tracker.onWindowChanged("com.example.a").isPresent());
    }

    @Test
    public void ignoredPackagesDoNotDisplaceTheCurrentApp() {
        tracker.onWindowChanged("com.example.a").orElseThrow();
        assertTrue(tracker.onWindowChanged("com.android.systemui").isEmpty());
        assertTrue(tracker.onWindowChanged("com.example.a").isEmpty());
    }

    @Test
    public void reportsNothingWhilePaused() {
        tracker.onWindowChanged("com.example.a").orElseThrow();
        tracker.pause();
        assertTrue(tracker.onWindowChanged("com.example.screensaver").isEmpty());
    }

    @Test
    public void theAppInFrontBeforeAPauseIsNotReportedAgainAfterIt() {
        tracker.onWindowChanged("com.example.a").orElseThrow();
        tracker.pause();
        tracker.onWindowChanged("com.example.screensaver");
        tracker.resume();
        assertTrue(tracker.onWindowChanged("com.example.a").isEmpty());
    }

    @Test
    public void reportsAgainAfterResuming() {
        tracker.pause();
        tracker.resume();
        assertTrue(tracker.onWindowChanged("com.example.a").isPresent());
    }

    @Test
    public void ignoresNullPackage() {
        assertTrue(tracker.onWindowChanged(null).isEmpty());
    }

    @Test
    public void emptyFilterAcceptsEverything() {
        ForegroundTracker open = new ForegroundTracker(new PackageFilter(Collections.emptyList()));
        assertTrue(open.onWindowChanged("com.android.systemui").isPresent());
    }
}
