package dev.aman.lookout.config;

import android.content.ContentResolver;
import android.provider.Settings;

/** The name the user gave the TV in system settings. */
public final class DeviceName {
    private DeviceName() {}

    /** Null on devices that never set one. */
    public static String read(ContentResolver resolver) {
        return Settings.Global.getString(resolver, Settings.Global.DEVICE_NAME);
    }
}
