package dev.aman.lookout.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import dev.aman.lookout.R;
import dev.aman.lookout.setup.AdbCommand;
import dev.aman.lookout.setup.Checklist;
import dev.aman.lookout.setup.Permission;
import dev.aman.lookout.setup.SystemPermissions;

/**
 * Setup step 2: every permission with whether it is on, and a button to turn it on. A permission whose screen the
 * TV does not have gets the adb command instead. Only rows still to do have a button, so granted ones cost no
 * D-pad stops.
 */
public final class PermissionsPanel {
    private final AppCompatActivity activity;
    private final SystemPermissions permissions;
    private final AdbCommand commands;
    private final CommandSheet commandSheet;
    private final View panel;
    private final LinearLayout rows;
    private final View done;

    public PermissionsPanel(AppCompatActivity activity, SystemPermissions permissions, AdbCommand commands,
                            Runnable onDone, Runnable onBack) {
        this.activity = activity;
        this.permissions = permissions;
        this.commands = commands;
        commandSheet = new CommandSheet(activity);
        panel = activity.findViewById(R.id.permissions_panel);
        rows = activity.findViewById(R.id.permission_rows);
        done = activity.findViewById(R.id.permissions_done);
        done.setOnClickListener(v -> onDone.run());
        activity.findViewById(R.id.permissions_back).setOnClickListener(v -> onBack.run());
    }

    public void show() {
        panel.setVisibility(View.VISIBLE);
        refresh();
    }

    public void hide() {
        panel.setVisibility(View.GONE);
    }

    /** Re-reads every permission and puts focus on the next one to allow, or on Done when none is left. */
    public void refresh() {
        // The rows are rebuilt, so the button the sheet came from would be gone; closing the sheet refreshes instead
        if (panel.getVisibility() != View.VISIBLE || commandSheet.isShowing()) return;
        Checklist checklist = Checklist.read(permissions);
        rows.removeAllViews();
        View firstAction = null;
        for (Checklist.Row row : checklist.rows()) {
            View item = LayoutInflater.from(activity).inflate(R.layout.item_permission, rows, false);
            View action = bind(item, row);
            rows.addView(item);
            if (firstAction == null) firstAction = action;
        }
        (firstAction != null ? firstAction : done).requestFocus();
    }

    // Returns the row's button, or null when there is nothing left to do for it
    private View bind(View item, Checklist.Row row) {
        Permission permission = row.permission();
        boolean hasScreen = permissions.screenFor(permission).isPresent();
        ((TextView) item.findViewById(R.id.permission_name)).setText(nameOf(permission));
        TextView tag = item.findViewById(R.id.permission_tag);
        tag.setText(permission.isRequired() ? R.string.perm_needed : R.string.perm_optional);
        tag.setBackgroundTintList(activity.getColorStateList(
                permission.isRequired() ? R.color.accent_container : R.color.outline_variant));
        tag.setTextColor(activity.getColor(
                permission.isRequired() ? R.color.on_accent_container : R.color.on_surface_variant));

        String description = activity.getString(descriptionOf(permission));
        boolean needsCommand = !row.granted() && !hasScreen;
        ((TextView) item.findViewById(R.id.permission_desc)).setText(
                needsCommand ? activity.getString(R.string.perm_no_screen, description) : description);

        ImageView icon = item.findViewById(R.id.permission_icon);
        icon.setImageResource(row.granted() ? R.drawable.ic_status_ok : R.drawable.ic_warning);
        int color = row.granted() ? R.color.success : R.color.warning;
        icon.setImageTintList(activity.getColorStateList(color));
        TextView state = item.findViewById(R.id.permission_state);
        state.setText(row.granted() ? R.string.perm_on : R.string.perm_off);
        state.setTextColor(activity.getColor(color));

        MaterialButton action = item.findViewById(R.id.permission_action);
        if (row.granted()) {
            action.setVisibility(View.GONE);
            return null;
        }
        action.setText(hasScreen ? R.string.perm_allow : R.string.perm_show_command);
        action.setIconResource(hasScreen ? R.drawable.ic_download : R.drawable.ic_info);
        ButtonStyle.setPrimary(action, hasScreen);
        action.setOnClickListener(v -> allow(permission));
        return action;
    }

    // A screen that is missing or will not open, even one that resolved when the row was drawn, gets the command
    private void allow(Permission permission) {
        if (PermissionScreens.open(activity, permissions, permission)) return;
        commandSheet.show(activity.getString(nameOf(permission)), commands.forPermission(permission),
                this::refresh);
    }

    private static int nameOf(Permission permission) {
        switch (permission) {
            case ACCESSIBILITY:
                return R.string.perm_accessibility;
            case MEDIA_ACCESS:
                return R.string.perm_media_access;
            case INSTALL_UPDATES:
                return R.string.perm_install_updates;
            default:
                throw new IllegalArgumentException("No name for " + permission);
        }
    }

    private static int descriptionOf(Permission permission) {
        switch (permission) {
            case ACCESSIBILITY:
                return R.string.perm_accessibility_desc;
            case MEDIA_ACCESS:
                return R.string.perm_media_access_desc;
            case INSTALL_UPDATES:
                return R.string.perm_install_updates_desc;
            default:
                throw new IllegalArgumentException("No description for " + permission);
        }
    }
}
