package dev.aman.lookout.foreground;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class ForegroundTrackerTest {
    private final ForegroundTracker tracker =
            new ForegroundTracker(new PackageFilter(List.of("com.android.systemui")), () -> "Living room");

    @Test
    public void emitsEventOnFirstSightingOfAnApp() {
        AppEvent event = tracker.onWindowChanged("com.example.a", null, 10).orElseThrow();
        assertEquals("com.example.a", event.packageName());
        assertEquals(10, event.epochSeconds());
    }

    @Test
    public void stampsTheDeviceAndActivityOnTheEvent() {
        AppEvent event = tracker.onWindowChanged("com.example.a", "com.example.a.Main", 10).orElseThrow();
        assertTrue(event.fields().compact().contains("\"activity\":\"com.example.a.Main\""));
        assertTrue(event.fields().compact().contains("\"device\":\"Living room\""));
    }

    @Test
    public void readsTheDeviceNameAtEventTimeSoARenameApplies() {
        AtomicReference<String> name = new AtomicReference<>("Den");
        ForegroundTracker renamed = new ForegroundTracker(new PackageFilter(Collections.emptyList()), name::get);
        renamed.onWindowChanged("com.example.a", null, 10);
        name.set("Bedroom");
        AppEvent event = renamed.onWindowChanged("com.example.b", null, 11).orElseThrow();
        assertTrue(event.fields().compact().contains("\"device\":\"Bedroom\""));
    }

    @Test
    public void reconfiguringKeepsTheAppInFrontSoItIsNotReportedAgain() {
        tracker.onWindowChanged("com.example.a", null, 10);
        tracker.reconfigure(new PackageFilter(List.of("com.example.launcher")), () -> "Den");
        assertTrue(tracker.onWindowChanged("com.example.a", null, 11).isEmpty());
    }

    @Test
    public void reconfiguringAppliesTheNewFilterAndDeviceName() {
        tracker.reconfigure(new PackageFilter(List.of("com.example.launcher")), () -> "Den");
        assertTrue(tracker.onWindowChanged("com.example.launcher", null, 10).isEmpty());
        AppEvent event = tracker.onWindowChanged("com.example.a", null, 11).orElseThrow();
        assertTrue(event.fields().compact().contains("\"device\":\"Den\""));
    }

    @Test
    public void suppressesRepeatsOfTheCurrentApp() {
        tracker.onWindowChanged("com.example.a", null, 10);
        assertTrue(tracker.onWindowChanged("com.example.a", null, 11).isEmpty());
    }

    @Test
    public void emitsAgainWhenReturningToAPreviousApp() {
        tracker.onWindowChanged("com.example.a", null, 10);
        tracker.onWindowChanged("com.example.b", null, 11);
        assertTrue(tracker.onWindowChanged("com.example.a", null, 12).isPresent());
    }

    @Test
    public void ignoredPackagesDoNotDisplaceTheCurrentApp() {
        tracker.onWindowChanged("com.example.a", null, 10).orElseThrow();
        assertTrue(tracker.onWindowChanged("com.android.systemui", null, 11).isEmpty());
        assertTrue(tracker.onWindowChanged("com.example.a", null, 12).isEmpty());
    }

    @Test
    public void reportsNothingWhilePaused() {
        tracker.onWindowChanged("com.example.a", null, 10).orElseThrow();
        tracker.pause();
        assertTrue(tracker.onWindowChanged("com.example.screensaver", null, 11).isEmpty());
    }

    @Test
    public void theAppInFrontBeforeAPauseIsNotReportedAgainAfterIt() {
        tracker.onWindowChanged("com.example.a", null, 10).orElseThrow();
        tracker.pause();
        tracker.onWindowChanged("com.example.screensaver", null, 11);
        tracker.resume();
        assertTrue(tracker.onWindowChanged("com.example.a", null, 12).isEmpty());
    }

    @Test
    public void reportsAgainAfterResuming() {
        tracker.pause();
        tracker.resume();
        assertTrue(tracker.onWindowChanged("com.example.a", null, 10).isPresent());
    }

    @Test
    public void ignoresNullPackage() {
        assertTrue(tracker.onWindowChanged(null, null, 10).isEmpty());
    }

    @Test
    public void emptyFilterAcceptsEverything() {
        ForegroundTracker open = new ForegroundTracker(new PackageFilter(Collections.emptyList()), () -> null);
        assertTrue(open.onWindowChanged("com.android.systemui", null, 1).isPresent());
    }

    @Test
    public void knowsTheAppLastReportedInFront() {
        ForegroundTracker tracker = new ForegroundTracker(new PackageFilter(List.of("ignored")), () -> null);
        assertNull(tracker.currentPackage());
        tracker.onWindowChanged("com.example.a", null, 1);
        tracker.onWindowChanged("ignored", null, 2);
        assertEquals("com.example.a", tracker.currentPackage());
    }
}
