package dev.aman.lookout.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import java.util.Optional;

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
    private final Activity activity;
    private final SystemPermissions permissions;
    private final AdbCommand commands;
    private final CommandSheet commandSheet;
    private final View panel;
    private final LinearLayout rows;
    private final View done;

    public PermissionsPanel(Activity activity, SystemPermissions permissions, AdbCommand commands, Runnable onDone,
                            Runnable onBack) {
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

    /** Closes the command sheet if it is open; true when it was. */
    public boolean closeSheet() {
        return commandSheet.hide();
    }

    /** Re-reads every permission and puts focus on the next one to allow, or on Done when none is left. */
    public void refresh() {
        // The rows are rebuilt, so the button the sheet came from would be gone; closing the sheet refreshes instead
        if (panel.getVisibility() != View.VISIBLE || commandSheet.isShowing()) return;
        Checklist checklist = Checklist.read(permissions);
        rows.removeAllViews();
        View focus = done;
        boolean focusChosen = false;
        for (Checklist.Row row : checklist.rows()) {
            View item = LayoutInflater.from(activity).inflate(R.layout.item_permission, rows, false);
            View action = bind(item, row);
            rows.addView(item);
            if (!focusChosen && action != null) {
                focus = action;
                focusChosen = true;
            }
        }
        focus.requestFocus();
    }

    // Returns the row's button, or null when there is nothing left to do for it
    private View bind(View item, Checklist.Row row) {
        Permission permission = row.permission();
        Optional<Intent> screen = permissions.screenFor(permission);
        ((TextView) item.findViewById(R.id.permission_name)).setText(nameOf(permission));
        TextView tag = item.findViewById(R.id.permission_tag);
        tag.setText(permission.isRequired() ? R.string.perm_needed : R.string.perm_optional);
        tag.setBackgroundTintList(activity.getColorStateList(
                permission.isRequired() ? R.color.accent_container : R.color.outline_variant));
        tag.setTextColor(activity.getColor(
                permission.isRequired() ? R.color.on_accent_container : R.color.on_surface_variant));

        String description = activity.getString(descriptionOf(permission));
        boolean needsCommand = !row.granted() && !screen.isPresent();
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
        action.setText(screen.isPresent() ? R.string.perm_allow : R.string.perm_show_command);
        action.setIconResource(screen.isPresent() ? R.drawable.ic_download : R.drawable.ic_info);
        useStyle(action, screen.isPresent());
        action.setOnClickListener(v -> allow(permission, screen));
        return action;
    }

    private void allow(Permission permission, Optional<Intent> screen) {
        if (screen.isPresent()) {
            try {
                activity.startActivity(screen.get());
                return;
            } catch (ActivityNotFoundException e) {
                // The screen resolved a moment ago but is gone, so fall through to the command
            }
        }
        commandSheet.show(activity.getString(nameOf(permission)), commands.forPermission(permission),
                this::refresh);
    }

    private static void useStyle(MaterialButton button, boolean primary) {
        Context context = button.getContext();
        button.setBackgroundTintList(context.getColorStateList(primary ? R.color.primary_bg : R.color.secondary_bg));
        button.setStrokeColor(context.getColorStateList(primary ? R.color.primary_stroke : R.color.secondary_stroke_state));
        button.setTextColor(context.getColorStateList(primary ? R.color.primary_text : R.color.secondary_text));
        button.setIconTint(context.getColorStateList(primary ? R.color.primary_text : R.color.secondary_text));
    }

    private static int nameOf(Permission permission) {
        switch (permission) {
            case ACCESSIBILITY:
                return R.string.perm_accessibility;
            case MEDIA_ACCESS:
                return R.string.perm_media_access;
            default:
                return R.string.perm_install_updates;
        }
    }

    private static int descriptionOf(Permission permission) {
        switch (permission) {
            case ACCESSIBILITY:
                return R.string.perm_accessibility_desc;
            case MEDIA_ACCESS:
                return R.string.perm_media_access_desc;
            default:
                return R.string.perm_install_updates_desc;
        }
    }
}
