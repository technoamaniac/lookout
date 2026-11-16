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
        AppEvent event = tracker.onWindowChanged("com.example.a", null).orElseThrow();
        assertEquals("com.example.a", event.packageName());
    }

    @Test
    public void namesTheActivityOnTheEvent() {
        AppEvent event = tracker.onWindowChanged("com.example.a", "com.example.a.Main").orElseThrow();
        assertTrue(event.fields().compact().contains("\"activity\":\"com.example.a.Main\""));
    }

    @Test
    public void aNewFilterKeepsTheAppInFrontSoItIsNotReportedAgain() {
        tracker.onWindowChanged("com.example.a", null);
        tracker.setFilter(new PackageFilter(List.of("com.example.launcher")));
        assertTrue(tracker.onWindowChanged("com.example.a", null).isEmpty());
    }

    @Test
    public void aNewFilterApplies() {
        tracker.setFilter(new PackageFilter(List.of("com.example.launcher")));
        assertTrue(tracker.onWindowChanged("com.example.launcher", null).isEmpty());
        assertTrue(tracker.onWindowChanged("com.example.a", null).isPresent());
    }

    @Test
    public void suppressesRepeatsOfTheCurrentApp() {
        tracker.onWindowChanged("com.example.a", null);
        assertTrue(tracker.onWindowChanged("com.example.a", "com.example.a.Other").isEmpty());
    }

    @Test
    public void emitsAgainWhenReturningToAPreviousApp() {
        tracker.onWindowChanged("com.example.a", null);
        tracker.onWindowChanged("com.example.b", null);
        assertTrue(tracker.onWindowChanged("com.example.a", null).isPresent());
    }

    @Test
    public void ignoredPackagesDoNotDisplaceTheCurrentApp() {
        tracker.onWindowChanged("com.example.a", null).orElseThrow();
        assertTrue(tracker.onWindowChanged("com.android.systemui", null).isEmpty());
        assertTrue(tracker.onWindowChanged("com.example.a", null).isEmpty());
    }

    @Test
    public void reportsNothingWhilePaused() {
        tracker.onWindowChanged("com.example.a", null).orElseThrow();
        tracker.pause();
        assertTrue(tracker.onWindowChanged("com.example.screensaver", null).isEmpty());
    }

    @Test
    public void theAppInFrontBeforeAPauseIsNotReportedAgainAfterIt() {
        tracker.onWindowChanged("com.example.a", null).orElseThrow();
        tracker.pause();
        tracker.onWindowChanged("com.example.screensaver", null);
        tracker.resume();
        assertTrue(tracker.onWindowChanged("com.example.a", null).isEmpty());
    }

    @Test
    public void reportsAgainAfterResuming() {
        tracker.pause();
        tracker.resume();
        assertTrue(tracker.onWindowChanged("com.example.a", null).isPresent());
    }

    @Test
    public void ignoresNullPackage() {
        assertTrue(tracker.onWindowChanged(null, null).isEmpty());
    }

    @Test
    public void emptyFilterAcceptsEverything() {
        ForegroundTracker open = new ForegroundTracker(new PackageFilter(Collections.emptyList()));
        assertTrue(open.onWindowChanged("com.android.systemui", null).isPresent());
    }
}
