package dev.aman.lookout.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.device.DeviceEvent;
import dev.aman.lookout.foreground.AppEvent;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ReporterTest {
    private static final URL WEBHOOK = url("https://ha.example/hook");
    private static final URL OTHER_WEBHOOK = url("https://other.example/hook");
    private static final long BEAT_MILLIS = Reporter.HEARTBEAT_SECONDS * 1_000;

    /** Records what each webhook was sent, and answers with {@link #result}. */
    private static final class FakeWebhooks {
        final List<String> sent = new ArrayList<>();
        final List<String> payloads = new ArrayList<>();
        DeliveryResult result = DeliveryResult.DELIVERED;

        EventSink sinkFor(URL webhook) {
            return message -> {
                sent.add(webhook.getHost() + " " + message.summary());
                payloads.add(message.toJson());
                return result;
            };
        }
    }

    private final ManualScheduler scheduler = new ManualScheduler();
    private final FakeWebhooks webhooks = new FakeWebhooks();
    private long wallMillis = 1_000_000;
    private String deviceName;
    private final Reporter reporter = new Reporter(webhooks::sinkFor, scheduler, () -> deviceName, () -> wallMillis);

    private static URL url(String text) {
        try {
            return new URL(text);
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
    }

    private void report(String pkg) {
        reporter.report(new AppEvent(pkg));
        scheduler.runQueued();
    }

    private void update(URL webhook, boolean on) {
        reporter.update(webhook, on);
        scheduler.runQueued();
    }

    private String lastPayload() {
        return webhooks.payloads.get(webhooks.payloads.size() - 1);
    }

    @Test
    public void startingToSendSendsTheStateAtOnce() {
        report("a");
        update(WEBHOOK, true);
        assertEquals(List.of("ha.example heartbeat"), webhooks.sent);
        assertTrue(lastPayload().contains("\"tv\":{\"app\":\"a\","));
    }

    @Test
    public void sendsEachChangeOnceWithTheStateItLeavesBehind() {
        update(WEBHOOK, true);
        reporter.report(new DeviceEvent(DeviceEvent.Kind.SCREEN_ON));
        report("a");
        assertEquals(List.of("ha.example heartbeat", "ha.example screen_on", "ha.example a"), webhooks.sent);
        assertTrue(lastPayload().contains("\"tv\":{\"app\":\"a\",\"screen\":\"on\","));
    }

    @Test
    public void sendsTheStateAgainEveryHeartbeat() {
        update(WEBHOOK, true);
        report("a");
        scheduler.advance(BEAT_MILLIS);
        assertEquals(List.of("ha.example heartbeat", "ha.example a", "ha.example heartbeat"), webhooks.sent);
        assertTrue(lastPayload().contains("\"interval\":30,\"tv\":{\"app\":\"a\","));
        scheduler.advance(BEAT_MILLIS);
        assertEquals(4, webhooks.sent.size());
    }

    // The heartbeat, not a retry, brings a receiver that missed a change back in step
    @Test
    public void aFailedChangeIsNotRetriedButTheNextHeartbeatCarriesIt() {
        update(WEBHOOK, true);
        webhooks.result = DeliveryResult.TEMPORARY_FAILURE;
        report("a");
        webhooks.result = DeliveryResult.DELIVERED;
        scheduler.advance(BEAT_MILLIS - 1);
        assertEquals(List.of("ha.example heartbeat", "ha.example a"), webhooks.sent);

        scheduler.advance(1);
        assertEquals("ha.example heartbeat", webhooks.sent.get(2));
        assertTrue(lastPayload().contains("\"app\":\"a\""));
    }

    @Test
    public void stampsEachMessageWithTheTvsNameAndTheTimeItWasReported() {
        update(WEBHOOK, true);
        deviceName = "Den";
        reporter.report(new AppEvent("a"));
        wallMillis = 5_000_000;
        scheduler.runQueued();
        assertTrue(lastPayload().contains("\"device\":\"Den\",\"timestamp\":1000,"));
    }

    @Test
    public void readsTheTvsNameForEachMessageSoARenameOrOptOutAppliesAtOnce() {
        update(WEBHOOK, true);
        deviceName = "Den";
        report("a");
        assertTrue(lastPayload().contains("\"device\":\"Den\""));
        deviceName = null;
        report("b");
        assertFalse(lastPayload().contains("\"device\""));
    }

    @Test
    public void whilePausedChangesAreKeptInTheStateAndSentOnResuming() {
        update(WEBHOOK, true);
        update(WEBHOOK, false);
        report("a");
        scheduler.advance(BEAT_MILLIS * 3);
        assertEquals(List.of("ha.example heartbeat"), webhooks.sent);

        update(WEBHOOK, true);
        assertEquals(List.of("ha.example heartbeat", "ha.example heartbeat"), webhooks.sent);
        assertTrue(lastPayload().contains("\"app\":\"a\""));
    }

    @Test
    public void unrelatedSettingsChangesSendNothingAndKeepTheBeat() {
        update(WEBHOOK, true);
        scheduler.advance(BEAT_MILLIS / 2);
        update(WEBHOOK, true);
        assertEquals(1, webhooks.sent.size());

        scheduler.advance(BEAT_MILLIS / 2);
        assertEquals(2, webhooks.sent.size());
    }

    @Test
    public void aNewWebhookIsSentTheStateAtOnceAndTheOldOneNothingMore() {
        update(WEBHOOK, true);
        report("a");
        update(OTHER_WEBHOOK, true);
        scheduler.advance(BEAT_MILLIS);
        assertEquals(List.of("ha.example heartbeat", "ha.example a", "other.example heartbeat",
                "other.example heartbeat"), webhooks.sent);
        assertTrue(lastPayload().contains("\"app\":\"a\""));
    }

    @Test
    public void sendsNothingWithoutAWebhook() {
        update(null, true);
        report("a");
        scheduler.advance(BEAT_MILLIS);
        assertTrue(webhooks.sent.isEmpty());
        assertEquals(0, scheduler.pendingTimed());
    }

    @Test
    public void removingTheWebhookStopsTheHeartbeat() {
        update(WEBHOOK, true);
        update(null, true);
        scheduler.advance(BEAT_MILLIS);
        assertEquals(1, webhooks.sent.size());
        assertEquals(0, scheduler.pendingTimed());
    }

    @Test
    public void sendsTheStateAtOnceWhenTheNetworkComesBackAfterAFailureAndCountsTheNextBeatFromThere() {
        webhooks.result = DeliveryResult.TEMPORARY_FAILURE;
        update(WEBHOOK, true);
        webhooks.result = DeliveryResult.DELIVERED;
        scheduler.advance(BEAT_MILLIS / 2);
        reporter.networkAvailable();
        scheduler.runQueued();
        assertEquals(2, webhooks.sent.size());

        scheduler.advance(BEAT_MILLIS - 1);
        assertEquals(2, webhooks.sent.size());
        scheduler.advance(1);
        assertEquals(3, webhooks.sent.size());
        assertEquals(1, scheduler.pendingTimed());
    }

    // Android reports the network as available when the watch starts, right after the first heartbeat went out
    @Test
    public void theNetworkComingBackSendsNothingWhenTheReceiverGotTheLastMessage() {
        update(WEBHOOK, true);
        reporter.networkAvailable();
        scheduler.runQueued();
        assertEquals(1, webhooks.sent.size());

        scheduler.advance(BEAT_MILLIS);
        assertEquals(2, webhooks.sent.size());
    }

    @Test
    public void theNetworkComingBackWhilePausedSendsNothing() {
        update(WEBHOOK, false);
        reporter.networkAvailable();
        scheduler.runQueued();
        assertTrue(webhooks.sent.isEmpty());
        assertEquals(0, scheduler.pendingTimed());
    }
}
