package dev.aman.lookout.ui;

import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import dev.aman.lookout.R;

/** The sheet that gives the adb command for a permission the TV has no settings screen for. */
final class CommandSheet {
    private final AppCompatActivity activity;
    private final Sheet sheet;
    private final TextView body;
    private final TextView command;
    private final View close;

    CommandSheet(AppCompatActivity activity) {
        this.activity = activity;
        sheet = new Sheet(activity, R.id.command_overlay);
        body = activity.findViewById(R.id.command_body);
        command = activity.findViewById(R.id.command_text);
        close = activity.findViewById(R.id.command_close);
        close.setOnClickListener(v -> sheet.hide());
    }

    /** @param onClosed runs after the sheet closes, to hand focus back to the row it was opened from */
    void show(String permissionName, String adbCommand, Runnable onClosed) {
        body.setText(activity.getString(R.string.command_body, permissionName));
        command.setText(adbCommand);
        sheet.show(close, onClosed);
    }

    boolean isShowing() {
        return sheet.isShowing();
    }
}
