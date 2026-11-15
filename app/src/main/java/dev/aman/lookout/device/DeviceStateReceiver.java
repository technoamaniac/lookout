package dev.aman.lookout.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Turns the system's screen and screensaver broadcasts into {@link DeviceEvent}s. These are
 * protected broadcasts that only reach receivers registered at runtime, so it cannot be declared in
 * the manifest; the accessibility service keeps the process alive instead.
 */
public final class DeviceStateReceiver extends BroadcastReceiver {
    private static final Map<String, DeviceEvent.Kind> KINDS = new HashMap<>();

    static {
        KINDS.put(Intent.ACTION_SCREEN_ON, DeviceEvent.Kind.SCREEN_ON);
        KINDS.put(Intent.ACTION_SCREEN_OFF, DeviceEvent.Kind.SCREEN_OFF);
        KINDS.put(Intent.ACTION_DREAMING_STARTED, DeviceEvent.Kind.SCREENSAVER_STARTED);
        KINDS.put(Intent.ACTION_DREAMING_STOPPED, DeviceEvent.Kind.SCREENSAVER_STOPPED);
    }

    private final Context context;
    private final Consumer<DeviceEvent> listener;
    private final Supplier<String> device;

    /** @param device supplies the name stamped on each event, or null for none */
    public DeviceStateReceiver(Context context, Consumer<DeviceEvent> listener, Supplier<String> device) {
        this.context = context;
        this.listener = listener;
        this.device = device;
    }

    /** Starts listening, and reports the current screen state so a receiver does not start blind. */
    public void start() {
        IntentFilter filter = new IntentFilter();
        for (String action : KINDS.keySet()) filter.addAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(this, filter);
        }
        PowerManager power = context.getSystemService(PowerManager.class);
        emit(power != null && !power.isInteractive() ? DeviceEvent.Kind.SCREEN_OFF : DeviceEvent.Kind.SCREEN_ON);
    }

    public void stop() {
        context.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        DeviceEvent.Kind kind = KINDS.get(intent.getAction());
        if (kind != null) emit(kind);
    }

    private void emit(DeviceEvent.Kind kind) {
        listener.accept(new DeviceEvent(kind, System.currentTimeMillis() / 1000).withDevice(device.get()));
    }
}
