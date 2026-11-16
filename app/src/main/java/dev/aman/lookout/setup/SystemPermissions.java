package dev.aman.lookout.setup;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;

import java.util.Optional;

/** Android adapter: reads each permission from the system and finds the settings screen that grants it. */
public final class SystemPermissions implements PermissionProbe {
    private final Context context;
    private final ComponentName accessibilityService;

    public SystemPermissions(Context context, ComponentName accessibilityService) {
        this.context = context;
        this.accessibilityService = accessibilityService;
    }

    @Override
    public boolean isGranted(Permission permission) {
        switch (permission) {
            case ACCESSIBILITY:
                return isAccessibilityOn();
            case MEDIA_ACCESS:
                return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.getPackageName());
            case INSTALL_UPDATES:
                return context.getPackageManager().canRequestPackageInstalls();
            default:
                throw new IllegalArgumentException("Unknown permission " + permission);
        }
    }

    /** The system screen where the user grants it, or empty when this TV ships without one. */
    public Optional<Intent> screenFor(Permission permission) {
        Intent screen;
        switch (permission) {
            case ACCESSIBILITY:
                screen = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                break;
            case MEDIA_ACCESS:
                screen = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                break;
            case INSTALL_UPDATES:
                screen = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + context.getPackageName()));
                break;
            default:
                throw new IllegalArgumentException("Unknown permission " + permission);
        }
        // Needs the matching <queries> entry in the manifest, or Android 11+ hides the screen from this app
        return screen.resolveActivity(context.getPackageManager()) == null ? Optional.empty() : Optional.of(screen);
    }

    private boolean isAccessibilityOn() {
        String enabled = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        // Compare whole entries: a substring match would accept e.g. ".LookoutServiceX", and
        // entries may use the short "pkg/.Class" form
        for (String entry : enabled.split(":")) {
            if (accessibilityService.equals(ComponentName.unflattenFromString(entry))) return true;
        }
        return false;
    }
}
