package dev.aman.lookout.delivery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import dev.aman.lookout.device.DeviceEvent;
import dev.aman.lookout.foreground.AppEvent;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

public class OutboxTest {
    private final Outbox outbox = new Outbox(new Backoff(new long[] {1_000, 5_000}, () -> 0.5));

    private static Message app(String pkg) {
        return Message.of(new AppEvent(pkg), null, 1_000);
    }

    private static Message screenOn() {
        return Message.of(new DeviceEvent(DeviceEvent.Kind.SCREEN_ON), null, 1_000);
    }

    @Test
    public void sendsNewestFirst() {
        Message screen = screenOn();
        Message app = app("a");
        outbox.add(screen);
        outbox.add(app);
        assertEquals(Optional.of(app), outbox.nextDue(0));
        outbox.settle(app);
        assertEquals(Optional.of(screen), outbox.nextDue(0));
    }

    @Test
    public void aNewerEventReplacesAnUnsentOneOfItsKind() {
        Message first = app("a");
        Message second = app("b");
        outbox.add(first);
        outbox.add(second);
        outbox.settle(second);
        assertFalse(outbox.nextDue(0).isPresent());
    }

    @Test
    public void aReplacedEventsResultIsIgnored() {
        Message first = app("a");
        Message second = app("b");
        outbox.add(first);
        outbox.add(second);
        outbox.settle(first);
        outbox.failed(first, 0);
        assertEquals(Optional.of(second), outbox.nextDue(0));
    }

    @Test
    public void aFailureWaitsLongerEachTime() {
        Message app = app("a");
        outbox.add(app);
        outbox.failed(app, 0);
        assertFalse(outbox.nextDue(999).isPresent());
        assertEquals(1_000, outbox.nextDueAt().getAsLong());
        outbox.failed(app, 1_000);
        assertEquals(6_000, outbox.nextDueAt().getAsLong());
        assertEquals(Optional.of(app), outbox.nextDue(6_000));
    }

    @Test
    public void aWaitingMessageDoesNotHoldUpOthers() {
        Message screen = screenOn();
        Message app = app("a");
        outbox.add(screen);
        outbox.add(app);
        outbox.failed(app, 0);
        assertEquals(Optional.of(screen), outbox.nextDue(0));
    }

    @Test
    public void retryNowEndsEveryWait() {
        Message app = app("a");
        outbox.add(app);
        outbox.failed(app, 0);
        outbox.retryNow();
        assertEquals(Optional.of(app), outbox.nextDue(0));
    }

    @Test
    public void nothingWaitingHasNoNextDueTime() {
        assertFalse(outbox.nextDueAt().isPresent());
        Message app = app("a");
        outbox.add(app);
        outbox.settle(app);
        assertFalse(outbox.nextDueAt().isPresent());
    }

    @Test
    public void aNewWebhookIsSentEverythingAgain() {
        Message app = app("a");
        outbox.useWebhook("https://one.example");
        outbox.add(app);
        outbox.settle(app);
        assertFalse(outbox.useWebhook("https://one.example"));
        assertFalse(outbox.nextDue(0).isPresent());

        assertTrue(outbox.useWebhook("https://two.example"));
        assertEquals(Optional.of(app), outbox.nextDue(0));
    }

    @Test
    public void aNewWebhookStartsWithoutWaiting() {
        Message app = app("a");
        outbox.add(app);
        outbox.failed(app, 0);
        outbox.useWebhook("https://two.example");
        assertEquals(Optional.of(app), outbox.nextDue(0));
    }

    @Test
    public void restoresASnapshotWithEverythingUnsettledDueAtOnce() {
        Message screen = screenOn();
        Message app = app("a");
        outbox.useWebhook("https://one.example");
        outbox.add(screen);
        outbox.add(app);
        outbox.settle(screen);
        outbox.failed(app, 0);

        Outbox restored = new Outbox(new Backoff(new long[] {1_000}, () -> 0.5));
        restored.restore(outbox.snapshot());

        assertEquals("https://one.example", restored.snapshot().webhook());
        List<Outbox.Item> items = restored.snapshot().items();
        assertEquals(2, items.size());
        assertTrue(items.get(0).settled());
        assertEquals(Optional.of(app), restored.nextDue(0));
        restored.settle(app);
        assertFalse(restored.nextDue(0).isPresent());
    }

    @Test
    public void settlesARestoredMessageMatchedById() {
        Message app = app("a");
        Message copy = new Message(app.id(), app.supersessionKey(), app.summary(), app.category(), app.fields(),
                app.occurredAtMillis());
        outbox.add(copy);
        outbox.settle(app);
        assertFalse(outbox.nextDue(0).isPresent());
    }
}
