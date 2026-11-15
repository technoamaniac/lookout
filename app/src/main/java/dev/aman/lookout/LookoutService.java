package dev.aman.lookout;

import android.accessibilityservice.AccessibilityService;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import dev.aman.lookout.config.Config;
import dev.aman.lookout.config.DeviceName;
import dev.aman.lookout.config.PrefsConfigStore;
import dev.aman.lookout.delivery.DeliveryLog;
import dev.aman.lookout.delivery.EventSink;
import dev.aman.lookout.delivery.RecordingSink;
import dev.aman.lookout.delivery.RetryingDispatcher;
import dev.aman.lookout.delivery.WebhookSink;
import dev.aman.lookout.foreground.ForegroundTracker;
import dev.aman.lookout.foreground.PackageFilter;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Android adapter: wires window events into the tracker and dispatcher. */
public class LookoutService extends AccessibilityService {
    private static final String SYSTEM_UI = "com.android.systemui";
    private static final long[] BACKOFF_MS = {0, 2_000, 10_000};

    // Lives as long as the service: settings changes reconfigure it, so the app in front is not forgotten
    private final ForegroundTracker tracker = new ForegroundTracker(new PackageFilter(List.of()), () -> null);
    private final ScheduledExecutorService io = Executors.newSingleThreadScheduledExecutor();
    // Enabling a keyboard changes which packages are ignored, without any settings change of ours
    private final ContentObserver keyboardsChanged = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            configure();
        }
    };
    private PrefsConfigStore store;
    private RetryingDispatcher dispatcher;

    @Override
    protected void onServiceConnected() {
        store = new PrefsConfigStore(this);
        store.onChange(this::configure);
        getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_INPUT_METHODS), false, keyboardsChanged);
        configure();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (dispatcher == null || event.getPackageName() == null) return;
        String activity = event.getClassName() == null ? null : event.getClassName().toString();
        tracker.onWindowChanged(event.getPackageName().toString(), activity, System.currentTimeMillis() / 1000)
                .ifPresent(dispatcher::dispatch);
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        if (store != null) store.stopListening();
        getContentResolver().unregisterContentObserver(keyboardsChanged);
        io.shutdownNow();
        super.onDestroy();
    }

    // Rebuilt whenever settings or keyboards change, so edits apply without restarting the service
    private void configure() {
        Config config = store.load();
        tracker.reconfigure(new PackageFilter(ignoredPackages(config)), deviceNameFor(config));
        dispatcher = config.webhookUrl()
                .map(url -> new RetryingDispatcher(recordingSinkFor(url),
                        (task, delayMillis) -> io.schedule(task, delayMillis, TimeUnit.MILLISECONDS),
                        BACKOFF_MS, tracker::isCurrent))
                .orElse(null);
    }

    // Asked per event so renaming the TV applies at once; withheld unless the user opted in
    private Supplier<String> deviceNameFor(Config config) {
        return config.sendDeviceName() ? () -> DeviceName.read(getContentResolver()) : () -> null;
    }

    private static EventSink recordingSinkFor(URL url) {
        return new RecordingSink(new WebhookSink(url), DeliveryLog.shared(), System::currentTimeMillis);
    }

    // Keyboards and system chrome raise window events over the real foreground app
    private List<String> ignoredPackages(Config config) {
        List<String> ignored = new ArrayList<>(config.ignoredPackages());
        ignored.add(SYSTEM_UI);
        ignored.add(getPackageName());
        InputMethodManager imm = getSystemService(InputMethodManager.class);
        if (imm != null) {
            for (InputMethodInfo ime : imm.getEnabledInputMethodList()) ignored.add(ime.getPackageName());
        }
        return ignored;
    }
}
