package dev.aman.lookout.delivery;

import android.content.Context;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/** Android adapter: builds the reporter that delivers to the webhook. */
public final class WebhookReporter {
    // Versions 0.4.0 to 0.5.1 kept undelivered messages here; the state in every message has replaced it
    private static final String OLD_OUTBOX = "outbox.json";

    private WebhookReporter() {}

    /**
     * @param log        records every attempt for the Deliveries tab
     * @param io         the single thread everything runs on; it must not run anything else that blocks
     * @param deviceName the TV's name to put in each message, or null to leave it out
     */
    public static Reporter create(Context context, DeliveryLog log, ScheduledExecutorService io,
                                  Supplier<String> deviceName) {
        io.execute(() -> new File(context.getFilesDir(), OLD_OUTBOX).delete());
        return new Reporter(url -> new RecordingSink(new WebhookSink(url), log, System::currentTimeMillis),
                new ExecutorScheduler(io), deviceName, System::currentTimeMillis);
    }
}
