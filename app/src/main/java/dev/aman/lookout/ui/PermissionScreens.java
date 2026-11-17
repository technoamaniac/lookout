package dev.aman.lookout.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;

import dev.aman.lookout.setup.Permission;
import dev.aman.lookout.setup.SystemPermissions;

import java.util.Optional;

/** Opens the system screen that grants a permission, for the panels that offer a button to it. */
final class PermissionScreens {
    private PermissionScreens() {}

    /** @return false when this TV has no such screen, so the caller can offer another way */
    static boolean open(Activity activity, SystemPermissions permissions, Permission permission) {
        Optional<Intent> screen = permissions.screenFor(permission);
        if (!screen.isPresent()) return false;
        try {
            activity.startActivity(screen.get());
            return true;
        } catch (ActivityNotFoundException e) {
            // Resolved but would not start, as on some TV builds: the same as having no screen
            return false;
        }
    }
}
