package dev.aman.lookout.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Turns the system's screen and screensaver broadcasts into {@link DeviceEvent}s. These are
 * protected broadcasts that only reach receivers registered at runtime, so it cannot be declared in
 * the manifest; the accessibility service keeps the process alive instead.
 */
public final class DeviceStateReceiver extends BroadcastReceiver {
    private static final Map<String, DeviceEvent.Kind> KINDS = Map.of(
            Intent.ACTION_SCREEN_ON, DeviceEvent.Kind.SCREEN_ON,
            Intent.ACTION_SCREEN_OFF, DeviceEvent.Kind.SCREEN_OFF,
            Intent.ACTION_DREAMING_STARTED, DeviceEvent.Kind.SCREENSAVER_STARTED,
            Intent.ACTION_DREAMING_STOPPED, DeviceEvent.Kind.SCREENSAVER_STOPPED);

    private final Context context;
    private final Consumer<DeviceEvent> listener;
    private boolean registered;

    public DeviceStateReceiver(Context context, Consumer<DeviceEvent> listener) {
        this.context = context;
        this.listener = listener;
    }

    /** Starts listening, and reports the current screen state so a receiver does not start blind. */
    public void start() {
        stop();
        IntentFilter filter = new IntentFilter();
        for (String action : KINDS.keySet()) filter.addAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(this, filter);
        }
        registered = true;
        PowerManager power = context.getSystemService(PowerManager.class);
        listener.accept(new DeviceEvent(power != null && !power.isInteractive()
                ? DeviceEvent.Kind.SCREEN_OFF : DeviceEvent.Kind.SCREEN_ON));
    }

    /** Stops listening; does nothing when not started. */
    public void stop() {
        if (!registered) return;
        context.unregisterReceiver(this);
        registered = false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        DeviceEvent.Kind kind = KINDS.get(intent.getAction());
        if (kind != null) listener.accept(new DeviceEvent(kind));
    }
}
