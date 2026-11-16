package dev.aman.lookout.setup;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Keeps whether setup was seen in its own preferences file: the service reconfigures itself whenever the
 * webhook settings change, and this is none of its business.
 */
public final class PrefsSetupStore implements SetupStore {
    private static final String FILE = "setup";
    private static final String KEY_SEEN = "seen";

    private final SharedPreferences prefs;

    public PrefsSetupStore(Context context) {
        this.prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    @Override
    public boolean isSeen() {
        return prefs.getBoolean(KEY_SEEN, false);
    }

    @Override
    public void markSeen() {
        prefs.edit().putBoolean(KEY_SEEN, true).apply();
    }
}
