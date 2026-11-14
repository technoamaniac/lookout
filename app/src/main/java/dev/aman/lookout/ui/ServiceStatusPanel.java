package dev.aman.lookout.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import dev.aman.lookout.R;

/** Shows whether the accessibility service is switched on, with a shortcut to switch it on. */
public final class ServiceStatusPanel {
    private final Activity activity;
    private final ComponentName service;
    private final TextView statusText;
    private final View openSettingsButton;

    public ServiceStatusPanel(Activity activity, ComponentName service) {
        this.activity = activity;
        this.service = service;
        statusText = activity.findViewById(R.id.service_status);
        openSettingsButton = activity.findViewById(R.id.open_accessibility);
        openSettingsButton.setOnClickListener(v -> openAccessibilitySettings());
    }

    /** Re-reads the system setting; call whenever the screen comes back into view. */
    public void refresh() {
        boolean enabled = isEnabled();
        statusText.setText(enabled ? R.string.service_enabled : R.string.service_disabled);
        statusText.setTextColor(activity.getColor(enabled ? R.color.ok : R.color.warn));
        openSettingsButton.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }

    private boolean isEnabled() {
        String enabled = Settings.Secure.getString(activity.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        // Compare whole entries: a substring match would accept e.g. ".LookoutServiceX", and
        // entries may use the short "pkg/.Class" form
        for (String entry : enabled.split(":")) {
            if (service.equals(ComponentName.unflattenFromString(entry))) return true;
        }
        return false;
    }

    private void openAccessibilitySettings() {
        try {
            activity.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (ActivityNotFoundException e) {
            // Some TV builds ship without this screen; the status text already points to the install script
            openSettingsButton.setVisibility(View.GONE);
        }
    }
}
