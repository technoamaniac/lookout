package dev.aman.lookout.delivery;

import android.content.Context;
import android.os.SystemClock;

import java.io.File;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/** Android adapter: builds the outbox that delivers to the webhook, saved in the app's private files. */
public final class WebhookOutbox {
    // Grows to every 15 minutes and stays there: a current event is never given up on
    private static final long[] RETRY_WAITS_MS = {2_000, 10_000, 60_000, 300_000, 900_000};

    private WebhookOutbox() {}

    /**
     * @param log        records every attempt for the Deliveries tab
     * @param io         the single thread everything runs on; it must not run anything else that blocks
     * @param deviceName the TV's name to put in each message, or null to leave it out
     */
    public static OutboxSender create(Context context, DeliveryLog log, ScheduledExecutorService io,
                                      Supplier<String> deviceName) {
        Random random = new Random();
        return new OutboxSender(new Outbox(new Backoff(RETRY_WAITS_MS, random::nextDouble)),
                new OutboxFile(new File(context.getFilesDir(), "outbox.json")),
                url -> new RecordingSink(new WebhookSink(url), log, System::currentTimeMillis),
                new ExecutorScheduler(io), deviceName, SystemClock::elapsedRealtime, System::currentTimeMillis);
    }
}
