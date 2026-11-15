package dev.aman.lookout.config;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Stores config in app-private preferences. The reporting switch shares the file, so the service's change listener
 * picks it up like any other setting.
 */
public final class PrefsConfigStore implements ConfigStore, ReportingSwitch {
    private static final String FILE = "lookout";
    private static final String KEY_URL = "webhook_url";
    private static final String KEY_IGNORED = "ignored_packages";
    private static final String KEY_SEND_DEVICE_NAME = "send_device_name";
    private static final String KEY_REPORTING = "reporting";
    private static final String KEY_HEARTBEAT_MINUTES = "heartbeat_minutes";

    private final SharedPreferences prefs;
    // SharedPreferences holds listeners weakly, so keep a strong reference
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    public PrefsConfigStore(Context context) {
        this.prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    @Override
    public Config load() {
        String ignored = prefs.getString(KEY_IGNORED, "");
        // Off until the user opts in: the name can be personal
        boolean sendDeviceName = prefs.getBoolean(KEY_SEND_DEVICE_NAME, false);
        // Off until the user picks an interval
        int heartbeatMinutes = Math.max(0, prefs.getInt(KEY_HEARTBEAT_MINUTES, 0));
        try {
            return Config.parse(prefs.getString(KEY_URL, ""), ignored, sendDeviceName)
                    .withHeartbeatMinutes(heartbeatMinutes);
        } catch (IllegalArgumentException e) {
            // A URL saved before validation tightened no longer parses; keep the other settings
            return Config.parse("", ignored, sendDeviceName).withHeartbeatMinutes(heartbeatMinutes);
        }
    }

    @Override
    public void save(Config config) {
        prefs.edit()
                .putString(KEY_URL, config.webhookUrlText())
                .putString(KEY_IGNORED, config.ignoredPackagesText())
                .putBoolean(KEY_SEND_DEVICE_NAME, config.sendDeviceName())
                .putInt(KEY_HEARTBEAT_MINUTES, config.heartbeatMinutes())
                .apply();
    }

    // On unless the user switched it off, so installs from before the switch keep reporting
    @Override
    public boolean isReportingOn() {
        return prefs.getBoolean(KEY_REPORTING, true);
    }

    @Override
    public void setReportingOn(boolean on) {
        prefs.edit().putBoolean(KEY_REPORTING, on).apply();
    }

    public void onChange(Runnable callback) {
        listener = (p, key) -> callback.run();
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void stopListening() {
        if (listener != null) prefs.unregisterOnSharedPreferenceChangeListener(listener);
        listener = null;
    }
}
