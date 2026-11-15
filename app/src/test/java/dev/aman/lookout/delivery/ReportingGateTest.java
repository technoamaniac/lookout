package dev.aman.lookout.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.device.DeviceEvent;
import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.LatestEvents;
import dev.aman.lookout.foreground.AppEvent;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ReportingGateTest {
    private final LatestEvents latest = new LatestEvents();
    private final ReportingGate gate = new ReportingGate(latest);
    private final List<Event> sent = new ArrayList<>();

    @Test
    public void sendsAndRecordsWhileOn() {
        gate.setSender(sent::add);
        AppEvent app = new AppEvent("a", 10);
        gate.report(app);
        assertEquals(List.of(app), sent);
        assertTrue(latest.isCurrent(app));
    }

    @Test
    public void recordsWithoutAWebhookSoNothingIsLost() {
        AppEvent app = new AppEvent("a", 10);
        gate.report(app);
        assertTrue(latest.isCurrent(app));
    }

    @Test
    public void aFirstWebhookGetsTheStateRecordedWithoutOne() {
        AppEvent app = new AppEvent("a", 10);
        gate.report(app);
        gate.setSender(sent::add);
        assertEquals(List.of(app), sent);
    }

    @Test
    public void replacingTheWebhookSendsNothingAgain() {
        gate.setSender(sent::add);
        gate.report(new AppEvent("a", 10));
        sent.clear();
        gate.setSender(sent::add);
        assertTrue(sent.isEmpty());
    }

    @Test
    public void aFirstWebhookWhilePausedWaitsForResume() {
        gate.setOn(false);
        gate.report(new AppEvent("a", 10));
        gate.setSender(sent::add);
        assertTrue(sent.isEmpty());
    }

    @Test
    public void pausingDropsPendingRetriesAndSendsNothing() {
        gate.setSender(sent::add);
        AppEvent before = new AppEvent("a", 10);
        gate.report(before);
        gate.setOn(false);
        assertFalse(latest.isCurrent(before));
        gate.report(new AppEvent("b", 11));
        assertEquals(List.of(before), sent);
    }

    @Test
    public void resumingSendsTheNewestOfEachKindFromThePause() {
        gate.setSender(sent::add);
        gate.report(new AppEvent("a", 10));
        gate.setOn(false);
        sent.clear();
        gate.report(new AppEvent("b", 11));
        AppEvent c = new AppEvent("c", 12);
        gate.report(c);
        DeviceEvent screen = new DeviceEvent(DeviceEvent.Kind.SCREEN_OFF, 13);
        gate.report(screen);
        gate.setOn(true);
        assertEquals(2, sent.size());
        assertTrue(sent.contains(c));
        assertTrue(sent.contains(screen));
    }

    @Test
    public void resumingAfterAQuietPauseSendsNothing() {
        gate.setSender(sent::add);
        gate.report(new AppEvent("a", 10));
        gate.setOn(false);
        sent.clear();
        gate.setOn(true);
        assertTrue(sent.isEmpty());
    }

    @Test
    public void settingTheSameStateAgainChangesNothing() {
        gate.setSender(sent::add);
        gate.report(new AppEvent("a", 10));
        sent.clear();
        gate.setOn(true);
        assertTrue(sent.isEmpty());
    }
}
