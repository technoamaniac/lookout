package dev.aman.lookout.apps;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Android adapter: lists the apps that have a launcher entry. Lookout can only see those, because the manifest asks
 * for package visibility by launcher intent rather than for every installed package.
 */
public final class InstalledApps implements AppCatalog {
    private final PackageManager packages;
    private final String ownPackage;

    /** @param ownPackage left out of the list: Lookout never reports itself */
    public InstalledApps(PackageManager packages, String ownPackage) {
        this.packages = packages;
        this.ownPackage = ownPackage;
    }

    @Override
    public List<InstalledApp> apps() {
        Set<String> homeScreens = new HashSet<>();
        for (ResolveInfo info : homeActivities()) homeScreens.add(info.activityInfo.packageName);
        Map<String, InstalledApp> byPackage = new LinkedHashMap<>();
        for (String category : new String[] {Intent.CATEGORY_LAUNCHER, Intent.CATEGORY_LEANBACK_LAUNCHER,
                Intent.CATEGORY_HOME}) {
            List<ResolveInfo> found = category.equals(Intent.CATEGORY_HOME)
                    ? homeActivities() : packages.queryIntentActivities(mainIntent(category), 0);
            for (ResolveInfo info : found) {
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(ownPackage) || byPackage.containsKey(pkg)) continue;
                String label = info.activityInfo.applicationInfo.loadLabel(packages).toString();
                byPackage.put(pkg, new InstalledApp(label, pkg, homeScreens.contains(pkg)));
            }
        }
        return new ArrayList<>(byPackage.values());
    }

    // Settings apps declare a home activity at a priority below zero only to cover the moments during boot when no
    // launcher is ready; that is not a home screen the user could choose
    private List<ResolveInfo> homeActivities() {
        List<ResolveInfo> homes = new ArrayList<>();
        for (ResolveInfo info : packages.queryIntentActivities(mainIntent(Intent.CATEGORY_HOME), 0)) {
            if (info.priority >= 0) homes.add(info);
        }
        return homes;
    }

    private static Intent mainIntent(String category) {
        return new Intent(Intent.ACTION_MAIN).addCategory(category);
    }
}
