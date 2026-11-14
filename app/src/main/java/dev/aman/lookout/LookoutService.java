package dev.aman.lookout;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Android adapter: wires window events into the tracker and dispatcher. */
public class LookoutService extends AccessibilityService {
    private static final String SYSTEM_UI = "com.android.systemui";
    private static final long[] BACKOFF_MS = {0, 2_000, 10_000};

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private PrefsConfigStore store;
    private ForegroundTracker tracker;
    private RetryingDispatcher dispatcher;

    @Override
    protected void onServiceConnected() {
        store = new PrefsConfigStore(this);
        store.onChange(this::configure);
        configure();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (tracker == null || dispatcher == null || event.getPackageName() == null) return;
        tracker.onWindowChanged(event.getPackageName().toString(), System.currentTimeMillis() / 1000)
                .ifPresent(dispatcher::dispatch);
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        if (store != null) store.stopListening();
        io.shutdownNow();
        super.onDestroy();
    }

    // Rebuilt whenever settings change, so edits apply without restarting the service
    private void configure() {
        Config config = store.load();
        tracker = new ForegroundTracker(new PackageFilter(ignoredPackages(config)));
        dispatcher = config.webhookUrl()
                .map(url -> new RetryingDispatcher(new WebhookSink(url), io, BACKOFF_MS,
                        Thread::sleep, tracker::isCurrent))
                .orElse(null);
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
