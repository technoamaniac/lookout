package dev.aman.lookout;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;

import dev.aman.lookout.apps.BuiltInApps;
import dev.aman.lookout.config.Config;
import dev.aman.lookout.config.DeviceName;
import dev.aman.lookout.config.PrefsConfigStore;
import dev.aman.lookout.delivery.NetworkWatcher;
import dev.aman.lookout.delivery.Reporter;
import dev.aman.lookout.delivery.WebhookReporter;
import dev.aman.lookout.device.DeviceEvent;
import dev.aman.lookout.device.DeviceStateReceiver;
import dev.aman.lookout.foreground.ForegroundTracker;
import dev.aman.lookout.foreground.PackageFilter;
import dev.aman.lookout.playback.MediaAccessService;
import dev.aman.lookout.playback.MediaSessionWatcher;
import dev.aman.lookout.playback.PlaybackTracker;
import dev.aman.lookout.update.PrefsUpdateChannel;
import dev.aman.lookout.update.RelaunchAfterUpdate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/** Android adapter: wires window, device and playback events through the trackers to the reporter. */
public class LookoutService extends AccessibilityService {
    // Lives as long as the service: settings changes reconfigure it, so the app in front is not forgotten
    private final ForegroundTracker tracker = new ForegroundTracker(new PackageFilter(List.of()));
    private final PlaybackTracker playback = new PlaybackTracker();
    private final ScheduledExecutorService io = Executors.newSingleThreadScheduledExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
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
            mediaSessions.start();
        }
    };
    // Set from the settings, and read on the reporter's thread for each message
    private volatile boolean sendDeviceName;
    // Made once the service has a context, and kept until it is destroyed
    private PrefsConfigStore store;
    private Reporter reporter;
    private NetworkWatcher network;
    private DeviceStateReceiver deviceState;
    private MediaSessionWatcher mediaSessions;

    @Override
    public void onCreate() {
        super.onCreate();
        store = new PrefsConfigStore(this);
        reporter = WebhookReporter.create(this, ((LookoutApplication) getApplication()).deliveryLog(), io,
                () -> sendDeviceName ? DeviceName.read(getContentResolver()) : null);
        network = new NetworkWatcher(this, reporter::networkAvailable);
        deviceState = new DeviceStateReceiver(this, this::onDeviceEvent);
        // Without the media access grant this does nothing, so playback stays off until the user opts in
        mediaSessions = new MediaSessionWatcher(this, new ComponentName(this, MediaAccessService.class),
                sessions -> playback.onSessionsChanged(sessions).forEach(reporter::report));
    }

    // The system may connect again without destroying the service, so everything watched is let go first
    @Override
    protected void onServiceConnected() {
        stopWatching();
        store.onChange(this::configure);
        getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_INPUT_METHODS), false, keyboardsChanged);
        // The constant is hidden from the SDK, so the setting is named by its key
        getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor("enabled_notification_listeners"), false, listenersChanged);
        // Before configure, so what they see is in the TV's state when the first heartbeat goes out
        deviceState.start();
        mediaSessions.start();
        configure();
        network.start();
        reopenAfterUpdate();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null) return;
        String activity = event.getClassName() == null ? null : event.getClassName().toString();
        tracker.onWindowChanged(event.getPackageName().toString(), activity).ifPresent(reporter::report);
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        stopWatching();
        io.shutdownNow();
        super.onDestroy();
    }

    private void stopWatching() {
        deviceState.stop();
        mediaSessions.stop();
        network.stop();
        store.stopListening();
        getContentResolver().unregisterContentObserver(keyboardsChanged);
        getContentResolver().unregisterContentObserver(listenersChanged);
    }

    // The screensaver's own window is not an app the user opened, so app changes wait until it ends, or the screen
    // turns off, which ends it too if the TV never says so
    private void onDeviceEvent(DeviceEvent event) {
        if (event.kind() == DeviceEvent.Kind.SCREENSAVER_STARTED) tracker.pause();
        if (event.kind() == DeviceEvent.Kind.SCREENSAVER_STOPPED || event.kind() == DeviceEvent.Kind.SCREEN_OFF) {
            tracker.resume();
        }
        reporter.report(event);
    }

    // Rerun whenever settings or keyboards change, so edits apply without restarting the service
    private void configure() {
        Config config = store.load();
        sendDeviceName = config.sendDeviceName();
        tracker.setFilter(filterFor(config));
        reporter.update(config.webhookUrl().orElse(null), store.isReportingOn());
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

    private PackageFilter filterFor(Config config) {
        return PackageFilter.of(config.ignoredPackages(), new BuiltInApps(this).packageNames(),
                config.reportedPackages());
    }
}
