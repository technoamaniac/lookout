package dev.aman.lookout;

import android.content.Context;
import android.content.SharedPreferences;

/** Stores config in app-private preferences. */
public final class PrefsConfigStore implements ConfigStore {
    private static final String FILE = "lookout";
    private static final String KEY_URL = "webhook_url";
    private static final String KEY_IGNORED = "ignored_packages";

    private final SharedPreferences prefs;
    // SharedPreferences holds listeners weakly, so keep a strong reference
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    public PrefsConfigStore(Context context) {
        this.prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    @Override
    public Config load() {
        String ignored = prefs.getString(KEY_IGNORED, "");
        try {
            return Config.parse(prefs.getString(KEY_URL, ""), ignored);
        } catch (IllegalArgumentException e) {
            // A URL saved before validation tightened no longer parses; keep the ignored packages
            return Config.parse("", ignored);
        }
    }

    @Override
    public void save(Config config) {
        prefs.edit()
                .putString(KEY_URL, config.webhookUrlText())
                .putString(KEY_IGNORED, config.ignoredPackagesText())
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
