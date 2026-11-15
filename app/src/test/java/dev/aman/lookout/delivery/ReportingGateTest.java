package dev.aman.lookout.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.device.DeviceEvent;
import dev.aman.lookout.event.Event;
import dev.aman.lookout.event.LatestEvents;
import dev.aman.lookout.foreground.AppEvent;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ReportingGateTest {
    /** Collects what it is asked to send, and whether it was stopped. */
    private static final class FakeSender implements ReportingGate.Sender {
        final List<Event> sent = new ArrayList<>();
        boolean stopped;

        @Override
        public void dispatch(Event event) {
            sent.add(event);
        }

        @Override
        public void stop() {
            stopped = true;
        }
    }

    private static final URL WEBHOOK = url("https://ha.example/hook");
    private static final URL OTHER_WEBHOOK = url("https://other.example/hook");

    private final LatestEvents latest = new LatestEvents();
    private final List<FakeSender> senders = new ArrayList<>();
    private final ReportingGate gate = new ReportingGate(latest, webhook -> {
        FakeSender sender = new FakeSender();
        senders.add(sender);
        return sender;
    });

    private static URL url(String text) {
        try {
            return new URL(text);
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
    }

    private FakeSender lastSender() {
        return senders.get(senders.size() - 1);
    }

    @Test
    public void sendsWhileOn() {
        gate.update(WEBHOOK, true);
        AppEvent app = new AppEvent("a", 10);
        gate.report(app);
        assertEquals(List.of(app), lastSender().sent);
    }

    @Test
    public void sendsNothingWhilePausedAndStopsPendingRetries() {
        gate.update(WEBHOOK, true);
        FakeSender before = lastSender();
        gate.update(WEBHOOK, false);
        gate.report(new AppEvent("a", 10));
        assertTrue(before.stopped);
        assertTrue(before.sent.isEmpty());
        assertEquals(1, senders.size());
    }

    @Test
    public void resumingSendsTheNewestOfEachKindFromThePauseInOrder() {
        gate.update(WEBHOOK, false);
        gate.report(new AppEvent("a", 10));
        DeviceEvent screen = new DeviceEvent(DeviceEvent.Kind.SCREEN_OFF, 11);
        gate.report(screen);
        AppEvent c = new AppEvent("c", 12);
        gate.report(c);
        gate.update(WEBHOOK, true);
        assertEquals(List.of(screen, c), lastSender().sent);
    }

    @Test
    public void aDeliveryThatFailedBeforeThePauseIsRetriedOnResume() {
        gate.update(WEBHOOK, true);
        AppEvent failed = new AppEvent("a", 10);
        gate.report(failed);
        gate.update(WEBHOOK, false);
        gate.update(WEBHOOK, true);
        assertEquals(List.of(failed), lastSender().sent);
    }

    @Test
    public void deliveredEventsAreNotSentAgain() {
        gate.update(WEBHOOK, true);
        AppEvent app = new AppEvent("a", 10);
        gate.report(app);
        gate.settling(WEBHOOK, e -> DeliveryResult.DELIVERED).send(app);
        gate.update(WEBHOOK, false);
        gate.update(WEBHOOK, true);
        assertTrue(lastSender().sent.isEmpty());
    }

    @Test
    public void refusedEventsAreNotSentAgain() {
        gate.update(WEBHOOK, true);
        AppEvent app = new AppEvent("a", 10);
        gate.report(app);
        gate.settling(WEBHOOK, e -> DeliveryResult.PERMANENT_FAILURE).send(app);
        gate.update(WEBHOOK, false);
        gate.update(WEBHOOK, true);
        assertTrue(lastSender().sent.isEmpty());
    }

    @Test
    public void aNewWebhookStopsTheOldOneAndGetsWhatIsUndelivered() {
        gate.update(WEBHOOK, true);
        FakeSender old = lastSender();
        AppEvent app = new AppEvent("a", 10);
        gate.report(app);
        gate.update(OTHER_WEBHOOK, true);
        assertTrue(old.stopped);
        assertEquals(List.of(app), lastSender().sent);
    }

    @Test
    public void aNewWebhookGetsTheCurrentStateEvenWhenItWasDeliveredToTheOldOne() {
        gate.update(WEBHOOK, true);
        AppEvent app = new AppEvent("a", 10);
        gate.report(app);
        gate.settling(WEBHOOK, e -> DeliveryResult.DELIVERED).send(app);
        gate.update(OTHER_WEBHOOK, true);
        assertEquals(List.of(app), lastSender().sent);
    }

    // Settled means settled for one webhook: what the old one received says nothing about the new one
    @Test
    public void aNewWebhookThatMissedTheCurrentStateGetsItOnResume() {
        gate.update(WEBHOOK, true);
        AppEvent app = new AppEvent("a", 10);
        gate.report(app);
        gate.settling(WEBHOOK, e -> DeliveryResult.DELIVERED).send(app);
        gate.update(OTHER_WEBHOOK, true);
        gate.update(OTHER_WEBHOOK, false);
        gate.update(OTHER_WEBHOOK, true);
        assertEquals(List.of(app), lastSender().sent);
    }

    @Test
    public void aDeliveryToTheOldWebhookFinishingLateDoesNotSettleForTheNewOne() {
        gate.update(WEBHOOK, true);
        AppEvent app = new AppEvent("a", 10);
        gate.report(app);
        EventSink oldSink = gate.settling(WEBHOOK, e -> DeliveryResult.DELIVERED);
        gate.update(OTHER_WEBHOOK, true);
        oldSink.send(app);
        assertTrue(latest.isPending(app));
    }

    @Test
    public void aWebhookChangedWhilePausedGetsTheCurrentStateOnResume() {
        gate.update(WEBHOOK, true);
        AppEvent app = new AppEvent("a", 10);
        gate.report(app);
        gate.settling(WEBHOOK, e -> DeliveryResult.DELIVERED).send(app);
        gate.update(WEBHOOK, false);
        gate.update(OTHER_WEBHOOK, false);
        gate.update(OTHER_WEBHOOK, true);
        assertEquals(List.of(app), lastSender().sent);
    }

    @Test
    public void resumingOnTheSameWebhookSkipsWhatItAlreadyHas() {
        gate.update(WEBHOOK, true);
        AppEvent app = new AppEvent("a", 10);
        gate.report(app);
        gate.settling(WEBHOOK, e -> DeliveryResult.DELIVERED).send(app);
        gate.update(WEBHOOK, false);
        gate.update(WEBHOOK, true);
        assertTrue(lastSender().sent.isEmpty());
    }

    // Keyboard and unrelated settings changes reconfigure the service; a resend would duplicate an in-flight event
    @Test
    public void anUpdateThatChangesNothingSendsNothingAgain() {
        gate.update(WEBHOOK, true);
        gate.report(new AppEvent("a", 10));
        FakeSender current = lastSender();
        gate.update(url("https://ha.example/hook"), true);
        assertEquals(1, senders.size());
        assertEquals(1, current.sent.size());
        assertFalse(current.stopped);
    }

    @Test
    public void aFirstWebhookGetsTheStateRecordedSoFar() {
        AppEvent app = new AppEvent("a", 10);
        gate.report(app);
        gate.update(WEBHOOK, true);
        assertEquals(List.of(app), lastSender().sent);
    }

    @Test
    public void aFirstWebhookWhilePausedWaitsForResume() {
        gate.update(null, false);
        AppEvent app = new AppEvent("a", 10);
        gate.report(app);
        gate.update(WEBHOOK, false);
        assertTrue(senders.isEmpty());
        gate.update(WEBHOOK, true);
        assertEquals(List.of(app), lastSender().sent);
    }
}
