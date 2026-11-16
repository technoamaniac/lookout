package dev.aman.lookout.setup;

import java.util.Objects;

/** The command to run from a computer for a permission the TV has no screen for. */
public final class AdbCommand {
    private final String packageName;
    private final String mediaServiceComponent;

    /** @param mediaServiceComponent the notification listener as {@code package/Class}, which Android needs spelled out */
    public AdbCommand(String packageName, String mediaServiceComponent) {
        this.packageName = Objects.requireNonNull(packageName);
        this.mediaServiceComponent = Objects.requireNonNull(mediaServiceComponent);
    }

    public String forPermission(Permission permission) {
        switch (permission) {
            case ACCESSIBILITY:
                // The script keeps any accessibility services already on, which a bare `settings put` would replace
                return "./scripts/install.sh <path to the Lookout .apk>";
            case MEDIA_ACCESS:
                return "adb shell cmd notification allow_listener " + mediaServiceComponent;
            case INSTALL_UPDATES:
                return "adb shell appops set " + packageName + " REQUEST_INSTALL_PACKAGES allow";
            default:
                throw new IllegalArgumentException("No command for " + permission);
        }
    }
}
