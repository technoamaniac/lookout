package dev.aman.lookout.update;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Keeps the choice in its own preferences file: the service reconfigures itself whenever the
 * webhook settings change, and this choice is none of its business.
 */
public final class PrefsUpdateChannel implements UpdateChannel {
    private static final String FILE = "updates";
    private static final String KEY_PRE_RELEASES = "include_pre_releases";

    private final SharedPreferences prefs;

    public PrefsUpdateChannel(Context context) {
        this.prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // Off until the user opts in: release candidates are less tested
    @Override
    public boolean includesPreReleases() {
        return prefs.getBoolean(KEY_PRE_RELEASES, false);
    }

    @Override
    public void setIncludesPreReleases(boolean include) {
        prefs.edit().putBoolean(KEY_PRE_RELEASES, include).apply();
    }
}
