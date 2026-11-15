package dev.aman.lookout.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.device.DeviceEvent;
import dev.aman.lookout.foreground.AppEvent;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.junit.Test;

public class OutboxSenderTest {
    private static final URL WEBHOOK = url("https://ha.example/hook");
    private static final URL OTHER_WEBHOOK = url("https://other.example/hook");
    private static final long FIRST_WAIT = 2_000;

    /** Records what each webhook was sent, and answers with queued results, then the default. */
    private static final class FakeWebhooks {
        final List<String> sent = new ArrayList<>();
        final Deque<DeliveryResult> results = new ArrayDeque<>();
        DeliveryResult otherwise = DeliveryResult.DELIVERED;

        EventSink sinkFor(URL webhook) {
            return message -> {
                sent.add(webhook.getHost() + " " + message.summary() + (message.toJson().contains("\"delayed\":true")
                        ? " delayed" : ""));
                return results.isEmpty() ? otherwise : results.poll();
            };
        }
    }

    /** Keeps the last save, like the file would. */
    private static final class MemoryStore implements OutboxStore {
        Outbox.Snapshot saved = Outbox.Snapshot.EMPTY;
        int saves;

        @Override
        public Outbox.Snapshot load() {
            return saved;
        }

        @Override
        public void save(Outbox.Snapshot snapshot) {
            saved = snapshot;
            saves++;
        }
    }

    private final ManualScheduler scheduler = new ManualScheduler();
    private final FakeWebhooks webhooks = new FakeWebhooks();
    private final MemoryStore store = new MemoryStore();
    private long wallMillis = 1_000_000;
    private OutboxSender sender = newSender();

    private static URL url(String text) {
        try {
            return new URL(text);
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
    }

    private OutboxSender newSender() {
        OutboxSender created = new OutboxSender(new Outbox(new Backoff(new long[] {FIRST_WAIT, 10_000}, () -> 0.5)),
                store, webhooks::sinkFor, scheduler, scheduler::now, () -> wallMillis);
        created.start();
        return created;
    }

    private void report(String pkg) {
        sender.report(new AppEvent(pkg, 1));
    }

    private void reportScreenOn() {
        sender.report(new DeviceEvent(DeviceEvent.Kind.SCREEN_ON, 1));
    }

    @Test
    public void sendsEachEventWhileOn() {
        sender.update(WEBHOOK, true);
        report("a");
        scheduler.runQueued();
        report("b");
        scheduler.runQueued();
        assertEquals(List.of("ha.example a", "ha.example b"), webhooks.sent);
    }

    @Test
    public void catchesUpNewestFirstWithOnlyTheLatestOfEachKind() {
        sender.update(WEBHOOK, false);
        reportScreenOn();
        report("a");
        report("b");
        scheduler.runQueued();
        assertTrue(webhooks.sent.isEmpty());

        sender.update(WEBHOOK, true);
        scheduler.runQueued();
        assertEquals(List.of("ha.example b", "ha.example screen_on"), webhooks.sent);
    }

    @Test
    public void anEventReportedDuringACatchUpGoesFirst() {
        sender.update(WEBHOOK, false);
        reportScreenOn();
        report("a");
        scheduler.runQueued();
        sender.update(WEBHOOK, true);
        report("b");
        scheduler.runQueued();
        assertEquals(List.of("ha.example b", "ha.example screen_on"), webhooks.sent);
    }

    @Test
    public void retriesAfterTheWaitUntilDelivered() {
        sender.update(WEBHOOK, true);
        webhooks.results.add(DeliveryResult.TEMPORARY_FAILURE);
        webhooks.results.add(DeliveryResult.TEMPORARY_FAILURE);
        report("a");
        scheduler.runQueued();
        assertEquals(1, webhooks.sent.size());

        scheduler.advance(FIRST_WAIT - 1);
        assertEquals(1, webhooks.sent.size());
        scheduler.advance(1);
        assertEquals(2, webhooks.sent.size());
        scheduler.advance(10_000);
        assertEquals(3, webhooks.sent.size());
        scheduler.advance(60_000);
        assertEquals(3, webhooks.sent.size());
        assertEquals(0, scheduler.pendingTimed());
    }

    @Test
    public void neverGivesUpOnACurrentEvent() {
        sender.update(WEBHOOK, true);
        webhooks.otherwise = DeliveryResult.TEMPORARY_FAILURE;
        report("a");
        scheduler.advance(24 * 60 * 60_000L);
        assertTrue(webhooks.sent.size() > 100);
        assertEquals(1, scheduler.pendingTimed());
    }

    @Test
    public void aRefusedEventIsNotRetried() {
        sender.update(WEBHOOK, true);
        webhooks.otherwise = DeliveryResult.PERMANENT_FAILURE;
        report("a");
        scheduler.advance(60_000);
        assertEquals(1, webhooks.sent.size());
    }

    @Test
    public void aNewerEventReplacesOneWaitingToBeRetried() {
        sender.update(WEBHOOK, true);
        webhooks.results.add(DeliveryResult.TEMPORARY_FAILURE);
        report("a");
        scheduler.runQueued();
        report("b");
        scheduler.advance(60_000);
        assertEquals(List.of("ha.example a", "ha.example b"), webhooks.sent);
    }

    @Test
    public void retriesAtOnceWhenTheNetworkComesBack() {
        sender.update(WEBHOOK, true);
        webhooks.results.add(DeliveryResult.TEMPORARY_FAILURE);
        report("a");
        scheduler.runQueued();
        sender.networkAvailable();
        scheduler.runQueued();
        assertEquals(2, webhooks.sent.size());
        assertEquals(0, scheduler.pendingTimed());
    }

    @Test
    public void pausingStopsRetries() {
        sender.update(WEBHOOK, true);
        webhooks.results.add(DeliveryResult.TEMPORARY_FAILURE);
        report("a");
        scheduler.runQueued();
        sender.update(WEBHOOK, false);
        scheduler.advance(60_000);
        assertEquals(1, webhooks.sent.size());

        sender.update(WEBHOOK, true);
        scheduler.runQueued();
        assertEquals(2, webhooks.sent.size());
    }

    @Test
    public void resumingRetriesAtOnceWithoutWaitingOutTheBackoff() {
        sender.update(WEBHOOK, true);
        webhooks.results.add(DeliveryResult.TEMPORARY_FAILURE);
        report("a");
        scheduler.runQueued();
        sender.update(WEBHOOK, false);
        scheduler.runQueued();

        sender.update(WEBHOOK, true);
        scheduler.runQueued();
        assertEquals(2, webhooks.sent.size());
    }

    @Test
    public void unrelatedSettingsChangesSendNothingAgain() {
        sender.update(WEBHOOK, true);
        report("a");
        scheduler.runQueued();
        sender.update(WEBHOOK, true);
        scheduler.runQueued();
        assertEquals(1, webhooks.sent.size());
    }

    @Test
    public void aNewWebhookIsSentTheCurrentStateAndTheOldOneNothingMore() {
        sender.update(WEBHOOK, true);
        reportScreenOn();
        report("a");
        scheduler.runQueued();
        webhooks.sent.clear();

        sender.update(OTHER_WEBHOOK, true);
        scheduler.runQueued();
        assertEquals(List.of("other.example a", "other.example screen_on"), webhooks.sent);
    }

    @Test
    public void sendsNothingWithoutAWebhook() {
        sender.update(null, true);
        report("a");
        scheduler.runQueued();
        assertTrue(webhooks.sent.isEmpty());

        sender.update(WEBHOOK, true);
        scheduler.runQueued();
        assertEquals(List.of("ha.example a"), webhooks.sent);
    }

    @Test
    public void flagsAnEventSentAMinuteOrMoreAfterItHappened() {
        sender.update(WEBHOOK, false);
        report("a");
        scheduler.runQueued();
        wallMillis += OutboxSender.LATE_AFTER_MILLIS - 1;
        sender.update(WEBHOOK, true);
        scheduler.runQueued();
        assertEquals(List.of("ha.example a"), webhooks.sent);

        sender.update(WEBHOOK, false);
        sender.update(OTHER_WEBHOOK, true);
        wallMillis += 1;
        scheduler.runQueued();
        assertEquals("other.example a delayed", webhooks.sent.get(1));
    }

    @Test
    public void picksUpWhereItLeftOffAfterARestart() {
        sender.update(WEBHOOK, true);
        webhooks.otherwise = DeliveryResult.TEMPORARY_FAILURE;
        reportScreenOn();
        report("a");
        scheduler.runQueued();
        webhooks.sent.clear();

        webhooks.otherwise = DeliveryResult.DELIVERED;
        sender = newSender();
        // Regression: the restored outbox already names this webhook, which must still get a sink
        sender.update(WEBHOOK, true);
        scheduler.runQueued();
        assertEquals(List.of("ha.example a", "ha.example screen_on"), webhooks.sent);
    }

    @Test
    public void doesNotResendWhatWasDeliveredBeforeARestart() {
        sender.update(WEBHOOK, true);
        report("a");
        scheduler.runQueued();
        webhooks.sent.clear();

        sender = newSender();
        sender.update(WEBHOOK, true);
        scheduler.runQueued();
        assertTrue(webhooks.sent.isEmpty());
    }

    @Test
    public void savesBeforeSending() {
        sender.update(WEBHOOK, true);
        webhooks.otherwise = DeliveryResult.TEMPORARY_FAILURE;
        report("a");
        scheduler.runQueued();
        assertEquals(1, store.saved.items().size());
        assertFalse(store.saved.items().get(0).settled());
    }

    @Test
    public void sendsAHeartbeatOnceWithoutSavingOrRetryingIt() {
        sender.update(WEBHOOK, true);
        scheduler.runQueued();
        int saves = store.saves;
        webhooks.otherwise = DeliveryResult.TEMPORARY_FAILURE;
        sender.sendNow(new AppEvent("beat", 1));
        scheduler.advance(60 * 60_000L);
        assertEquals(List.of("ha.example beat"), webhooks.sent);
        assertEquals(saves, store.saves);
    }

    @Test
    public void sendsNoHeartbeatWhilePaused() {
        sender.update(WEBHOOK, false);
        sender.sendNow(new AppEvent("beat", 1));
        scheduler.runQueued();
        assertTrue(webhooks.sent.isEmpty());
    }

    @Test
    public void aDeliveredHeartbeatRetriesWhatIsWaitingAtOnce() {
        sender.update(WEBHOOK, true);
        webhooks.results.add(DeliveryResult.TEMPORARY_FAILURE);
        report("a");
        scheduler.runQueued();

        sender.sendNow(new AppEvent("beat", 1));
        scheduler.runQueued();

        assertEquals(List.of("ha.example a", "ha.example beat", "ha.example a"), webhooks.sent);
        assertEquals(0, scheduler.pendingTimed());
    }
}
