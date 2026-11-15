package dev.aman.lookout;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import dev.aman.lookout.config.Config;
import dev.aman.lookout.config.DeviceName;
import dev.aman.lookout.config.PrefsConfigStore;
import dev.aman.lookout.delivery.Backoff;
import dev.aman.lookout.delivery.EventSink;
import dev.aman.lookout.delivery.NetworkWatcher;
import dev.aman.lookout.delivery.Outbox;
import dev.aman.lookout.delivery.OutboxFile;
import dev.aman.lookout.delivery.OutboxSender;
import dev.aman.lookout.delivery.RecordingSink;
import dev.aman.lookout.delivery.WebhookSink;
import dev.aman.lookout.device.DeviceEvent;
import dev.aman.lookout.device.DeviceStateReceiver;
import dev.aman.lookout.foreground.ForegroundTracker;
import dev.aman.lookout.foreground.PackageFilter;
import dev.aman.lookout.playback.MediaAccessService;
import dev.aman.lookout.playback.MediaSessionWatcher;
import dev.aman.lookout.playback.PlaybackTracker;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Android adapter: wires window, device and playback events through the trackers to the outbox. */
public class LookoutService extends AccessibilityService {
    private static final String SYSTEM_UI = "com.android.systemui";
    // Grows to every 15 minutes and stays there: a current event is never given up on
    private static final long[] RETRY_WAITS_MS = {2_000, 10_000, 60_000, 300_000, 900_000};

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
    // Replaced whenever settings change, so the opt-in applies to device events too
    private volatile Supplier<String> deviceName = () -> null;
    private final PlaybackTracker playback = new PlaybackTracker(() -> deviceName.get());
    private PrefsConfigStore store;
    private OutboxSender outbox;
    private NetworkWatcher network;
    private DeviceStateReceiver deviceState;
    private MediaSessionWatcher mediaSessions;

    @Override
    protected void onServiceConnected() {
        store = new PrefsConfigStore(this);
        // Kept if the system connects again, so nothing loads the outbox twice
        if (outbox == null) {
            outbox = newOutbox();
            outbox.start();
        }
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
        if (network != null) network.stop();
        network = new NetworkWatcher(this, outbox::networkAvailable);
        network.start();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null) return;
        String activity = event.getClassName() == null ? null : event.getClassName().toString();
        tracker.onWindowChanged(event.getPackageName().toString(), activity, System.currentTimeMillis() / 1000)
                .ifPresent(outbox::report);
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        if (deviceState != null) deviceState.stop();
        if (mediaSessions != null) mediaSessions.stop();
        if (network != null) network.stop();
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
        outbox.report(event);
    }

    // Without the media access grant this does nothing, so playback stays off until the user opts in
    private void watchMediaSessions() {
        if (mediaSessions != null) mediaSessions.stop();
        mediaSessions = new MediaSessionWatcher(this, new ComponentName(this, MediaAccessService.class),
                sessions -> playback.onSessionsChanged(sessions, System.currentTimeMillis() / 1000)
                        .forEach(outbox::report));
        mediaSessions.start();
    }

    // Rebuilt whenever settings or keyboards change, so edits apply without restarting the service
    private void configure() {
        Config config = store.load();
        deviceName = deviceNameFor(config);
        tracker.reconfigure(new PackageFilter(ignoredPackages(config)), deviceName);
        outbox.update(config.webhookUrl().orElse(null), store.isReportingOn());
    }

    // Asked per event so renaming the TV applies at once; withheld unless the user opted in
    private Supplier<String> deviceNameFor(Config config) {
        return config.sendDeviceName() ? () -> DeviceName.read(getContentResolver()) : () -> null;
    }

    // Saved in the app's private files, so undelivered events survive a restart, an update or a reboot
    private OutboxSender newOutbox() {
        Random random = new Random();
        return new OutboxSender(new Outbox(new Backoff(RETRY_WAITS_MS, random::nextDouble)),
                new OutboxFile(new File(getFilesDir(), "outbox.json")), this::recordingSinkFor,
                new OutboxSender.Scheduler() {
                    @Override
                    public void execute(Runnable task) {
                        io.execute(task);
                    }

                    @Override
                    public Runnable schedule(Runnable task, long delayMillis) {
                        ScheduledFuture<?> future = io.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
                        return () -> future.cancel(false);
                    }
                },
                SystemClock::elapsedRealtime, System::currentTimeMillis);
    }

    private EventSink recordingSinkFor(URL url) {
        return new RecordingSink(new WebhookSink(url), ((LookoutApplication) getApplication()).deliveryLog(),
                System::currentTimeMillis);
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
