package dev.aman.lookout.update;

import android.content.Context;
import android.content.pm.PackageManager;

/** Android adapter: the version name of the running copy of Lookout, as set from the release tag. */
public final class InstalledVersion {
    private InstalledVersion() {}

    public static String name(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("The app is not installed under its own package name", e);
        }
    }
}
