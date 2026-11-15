package dev.aman.lookout;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
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
import dev.aman.lookout.delivery.ReportingGate;
import dev.aman.lookout.delivery.RetryingDispatcher;
import dev.aman.lookout.delivery.WebhookSink;
import dev.aman.lookout.device.DeviceEvent;
import dev.aman.lookout.device.DeviceStateReceiver;
import dev.aman.lookout.event.LatestEvents;
import dev.aman.lookout.foreground.ForegroundTracker;
import dev.aman.lookout.foreground.PackageFilter;
import dev.aman.lookout.playback.MediaAccessService;
import dev.aman.lookout.playback.MediaSessionWatcher;
import dev.aman.lookout.playback.PlaybackTracker;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Android adapter: wires window, device and playback events through the trackers to the reporting gate. */
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
    // Access is granted from outside the app, after the service may already be running
    private final ContentObserver listenersChanged = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            watchMediaSessions();
        }
    };
    private final LatestEvents latest = new LatestEvents();
    private final ReportingGate reporting = new ReportingGate(latest, this::senderFor);
    // Replaced whenever settings change, so the opt-in applies to device events too
    private volatile Supplier<String> deviceName = () -> null;
    private final PlaybackTracker playback = new PlaybackTracker(() -> deviceName.get());
    private PrefsConfigStore store;
    private DeviceStateReceiver deviceState;
    private MediaSessionWatcher mediaSessions;

    @Override
    protected void onServiceConnected() {
        store = new PrefsConfigStore(this);
        store.onChange(this::configure);
        getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_INPUT_METHODS), false, keyboardsChanged);
        configure();
        if (deviceState != null) deviceState.stop();
        deviceState = new DeviceStateReceiver(this, this::onDeviceEvent, () -> deviceName.get());
        deviceState.start();
        // The constant is hidden from the SDK, so the setting is named by its key
        getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor("enabled_notification_listeners"), false, listenersChanged);
        watchMediaSessions();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null) return;
        String activity = event.getClassName() == null ? null : event.getClassName().toString();
        tracker.onWindowChanged(event.getPackageName().toString(), activity, System.currentTimeMillis() / 1000)
                .ifPresent(reporting::report);
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        if (deviceState != null) deviceState.stop();
        if (mediaSessions != null) mediaSessions.stop();
        if (store != null) store.stopListening();
        getContentResolver().unregisterContentObserver(keyboardsChanged);
        getContentResolver().unregisterContentObserver(listenersChanged);
        io.shutdownNow();
        super.onDestroy();
    }

    // The screensaver's own window is not an app the user opened, so app changes wait until it ends
    private void onDeviceEvent(DeviceEvent event) {
        if (event.kind() == DeviceEvent.Kind.SCREENSAVER_STARTED) tracker.pause();
        if (event.kind() == DeviceEvent.Kind.SCREENSAVER_STOPPED) tracker.resume();
        reporting.report(event);
    }

    // Without the media access grant this does nothing, so playback stays off until the user opts in
    private void watchMediaSessions() {
        if (mediaSessions != null) mediaSessions.stop();
        mediaSessions = new MediaSessionWatcher(this, new ComponentName(this, MediaAccessService.class),
                sessions -> playback.onSessionsChanged(sessions, System.currentTimeMillis() / 1000)
                        .forEach(reporting::report));
        mediaSessions.start();
    }

    // Rebuilt whenever settings or keyboards change, so edits apply without restarting the service
    private void configure() {
        Config config = store.load();
        deviceName = deviceNameFor(config);
        tracker.reconfigure(new PackageFilter(ignoredPackages(config)), deviceName);
        reporting.update(config.webhookUrl().orElse(null), store.isReportingOn());
    }

    // Asked per event so renaming the TV applies at once; withheld unless the user opted in
    private Supplier<String> deviceNameFor(Config config) {
        return config.sendDeviceName() ? () -> DeviceName.read(getContentResolver()) : () -> null;
    }

    private ReportingGate.Sender senderFor(URL url) {
        return new RetryingDispatcher(reporting.settling(url, recordingSinkFor(url)),
                (task, delayMillis) -> io.schedule(task, delayMillis, TimeUnit.MILLISECONDS),
                BACKOFF_MS, latest::isPending);
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
