package dev.aman.lookout;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import dev.aman.lookout.config.Config;
import dev.aman.lookout.config.DeviceName;
import dev.aman.lookout.config.PrefsConfigStore;
import dev.aman.lookout.delivery.NetworkWatcher;
import dev.aman.lookout.delivery.OutboxSender;
import dev.aman.lookout.delivery.WebhookOutbox;
import dev.aman.lookout.device.DeviceEvent;
import dev.aman.lookout.device.DeviceStateReceiver;
import dev.aman.lookout.event.Event;
import dev.aman.lookout.foreground.ForegroundTracker;
import dev.aman.lookout.foreground.PackageFilter;
import dev.aman.lookout.heartbeat.Heartbeat;
import dev.aman.lookout.heartbeat.HeartbeatEvent;
import dev.aman.lookout.playback.MediaAccessService;
import dev.aman.lookout.playback.MediaSessionWatcher;
import dev.aman.lookout.playback.PlaybackTracker;
import dev.aman.lookout.update.InstalledVersion;
import dev.aman.lookout.update.PrefsUpdateChannel;
import dev.aman.lookout.update.RelaunchAfterUpdate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/** Android adapter: wires window, device and playback events through the trackers to the outbox. */
public class LookoutService extends AccessibilityService {
    private static final String SYSTEM_UI = "com.android.systemui";

    // Lives as long as the service: settings changes reconfigure it, so the app in front is not forgotten
    private final ForegroundTracker tracker = new ForegroundTracker(new PackageFilter(List.of()), () -> null);
    private final ScheduledExecutorService io = Executors.newSingleThreadScheduledExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    // On the main thread, where the trackers it reads are updated; the main looper's clock stops in deep sleep
    private final Heartbeat heartbeat = new Heartbeat((task, delayMillis) -> {
        main.postDelayed(task, delayMillis);
        return () -> main.removeCallbacks(task);
    }, this::heartbeatFor, this::sendHeartbeat);
    // Enabling a keyboard changes which packages are ignored, without any settings change of ours
    private final ContentObserver keyboardsChanged = new ContentObserver(main) {
        @Override
        public void onChange(boolean selfChange) {
            configure();
        }
    };
    // Access is granted from outside the app, after the service may already be running
    private final ContentObserver listenersChanged = new ContentObserver(main) {
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
    private String version;

    @Override
    protected void onServiceConnected() {
        store = new PrefsConfigStore(this);
        version = InstalledVersion.name(this);
        // Kept if the system connects again, so nothing loads the outbox twice
        if (outbox == null) {
            outbox = WebhookOutbox.create(this, ((LookoutApplication) getApplication()).deliveryLog(), io);
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
        reopenAfterUpdate();
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
        heartbeat.stop();
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
        boolean reporting = store.isReportingOn();
        outbox.update(config.webhookUrl().orElse(null), reporting);
        // Nothing to beat for while reports are paused or there is no webhook; resuming beats at once
        boolean sending = reporting && config.webhookUrl().isPresent();
        heartbeat.setInterval(sending ? config.heartbeatMinutes() * 60L : 0);
    }

    // Asked per event so renaming the TV applies at once; withheld unless the user opted in
    private Supplier<String> deviceNameFor(Config config) {
        return config.sendDeviceName() ? () -> DeviceName.read(getContentResolver()) : () -> null;
    }

    // Android starts the service again once an update installs; the screen the user updated from does not come back
    // by itself. A bound accessibility service may start an activity from the background. The old version's task may
    // still be listed, so it is cleared rather than left under the new screen for Back to return to.
    private void reopenAfterUpdate() {
        if (!new RelaunchAfterUpdate(new PrefsUpdateChannel(this)).consume(System.currentTimeMillis())) return;
        startActivity(new Intent(this, SettingsActivity.class)
                .putExtra(SettingsActivity.EXTRA_SHOW_UPDATES, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
    }

    // A method rather than a lambda in the field: the outbox is made only once the service connects
    private void sendHeartbeat(Event heartbeat) {
        outbox.sendNow(heartbeat);
    }

    private Event heartbeatFor(long intervalSeconds) {
        PowerManager power = getSystemService(PowerManager.class);
        boolean screenOn = power == null || power.isInteractive();
        return new HeartbeatEvent(intervalSeconds, tracker.currentPackage(), screenOn, playback.sessions(), version,
                System.currentTimeMillis() / 1000).withDevice(deviceName.get());
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
