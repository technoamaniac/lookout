package dev.aman.lookout.config;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores config in app-private preferences. */
public final class PrefsConfigStore implements ConfigStore {
    private static final String FILE = "lookout";
    private static final String KEY_URL = "webhook_url";
    private static final String KEY_IGNORED = "ignored_packages";
    private static final String KEY_SEND_DEVICE_NAME = "send_device_name";

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
        try {
            return Config.parse(prefs.getString(KEY_URL, ""), ignored, sendDeviceName);
        } catch (IllegalArgumentException e) {
            // A URL saved before validation tightened no longer parses; keep the other settings
            return Config.parse("", ignored, sendDeviceName);
        }
    }

    @Override
    public void save(Config config) {
        prefs.edit()
                .putString(KEY_URL, config.webhookUrlText())
                .putString(KEY_IGNORED, config.ignoredPackagesText())
                .putBoolean(KEY_SEND_DEVICE_NAME, config.sendDeviceName())
                .apply();
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
