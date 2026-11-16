package dev.aman.lookout.apps;

import java.util.Objects;

/** An app on the TV that has a launcher entry. */
public final class InstalledApp {
    private final String label;
    private final String packageName;
    private final boolean homeScreen;

    public InstalledApp(String label, String packageName, boolean homeScreen) {
        this.label = Objects.requireNonNull(label);
        this.packageName = Objects.requireNonNull(packageName);
        this.homeScreen = homeScreen;
    }

    public String label() {
        return label;
    }

    public String packageName() {
        return packageName;
    }

    /** Whether it can be the TV's home screen, so it is often the one app to ignore. */
    public boolean isHomeScreen() {
        return homeScreen;
    }
}
