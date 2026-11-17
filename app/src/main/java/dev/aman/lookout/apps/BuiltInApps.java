package dev.aman.lookout.apps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.aman.lookout.R;

/**
 * Android adapter: the packages ignored unless the user says otherwise. Keyboards and system chrome raise window
 * events over the real foreground app, and Lookout's own screen is not something to report.
 */
public final class BuiltInApps implements AppCatalog {
    private static final String SYSTEM_UI = "com.android.systemui";

    private final Context context;

    public BuiltInApps(Context context) {
        this.context = context;
    }

    @Override
    public List<InstalledApp> apps() {
        Map<String, InstalledApp> byPackage = new LinkedHashMap<>();
        add(byPackage, SYSTEM_UI, context.getString(R.string.builtin_system_ui));
        add(byPackage, context.getPackageName(), context.getString(R.string.app_name));
        PackageManager packages = context.getPackageManager();
        // A keyboard app can offer several input methods; the package is what is ignored
        for (InputMethodInfo keyboard : enabledKeyboards()) {
            add(byPackage, keyboard.getPackageName(), keyboard.loadLabel(packages).toString());
        }
        return new ArrayList<>(byPackage.values());
    }

    /**
     * Just the packages, for deciding what to ignore: the service asks on every config change, so this skips the
     * label lookups that only the list needs.
     */
    public List<String> packageNames() {
        Set<String> names = new LinkedHashSet<>();
        names.add(SYSTEM_UI);
        names.add(context.getPackageName());
        for (InputMethodInfo keyboard : enabledKeyboards()) names.add(keyboard.getPackageName());
        return new ArrayList<>(names);
    }

    private List<InputMethodInfo> enabledKeyboards() {
        InputMethodManager keyboards = context.getSystemService(InputMethodManager.class);
        return keyboards == null ? Collections.emptyList() : keyboards.getEnabledInputMethodList();
    }

    private static void add(Map<String, InstalledApp> byPackage, String packageName, String label) {
        byPackage.putIfAbsent(packageName, new InstalledApp(label, packageName, false));
    }
}
