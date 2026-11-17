package dev.aman.lookout.ui;

import android.app.Activity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import dev.aman.lookout.R;
import dev.aman.lookout.config.ReportingSwitch;
import dev.aman.lookout.setup.Permission;
import dev.aman.lookout.setup.SystemPermissions;

/**
 * Shows whether the accessibility service is switched on, with a shortcut to switch it on, and the switch that
 * pauses reporting. The switch applies at once: turning reports off should not wait for Save or a test.
 */
public final class ServiceStatusPanel {
    private final Activity activity;
    private final SystemPermissions permissions;
    private final View runningCard;
    private final View notEnabledCard;
    private final View openSettingsButton;
    private final View lastTab;
    private final ReportingSwitch reporting;
    private final CompoundButton reportingToggle;
    private final ImageView dot;
    private final TextView detail;

    public ServiceStatusPanel(Activity activity, SystemPermissions permissions, ReportingSwitch reporting) {
        this.activity = activity;
        this.permissions = permissions;
        this.reporting = reporting;
        runningCard = activity.findViewById(R.id.service_on_card);
        notEnabledCard = activity.findViewById(R.id.service_off_card);
        openSettingsButton = activity.findViewById(R.id.open_accessibility);
        openSettingsButton.setOnClickListener(v -> openAccessibilitySettings());
        lastTab = activity.findViewById(R.id.tab_updates);
        reportingToggle = activity.findViewById(R.id.reporting_toggle);
        dot = activity.findViewById(R.id.service_dot);
        detail = activity.findViewById(R.id.service_detail);
        reportingToggle.setOnCheckedChangeListener((button, on) -> {
            reporting.setReportingOn(on);
            showReporting(on);
        });
    }

    /** Re-reads the system setting; call whenever the screen comes back into view. */
    public void refresh() {
        boolean enabled = permissions.isGranted(Permission.ACCESSIBILITY);
        runningCard.setVisibility(enabled ? View.VISIBLE : View.GONE);
        notEnabledCard.setVisibility(enabled ? View.GONE : View.VISIBLE);
        // Down from the last tab goes to whichever card shows, not into the page beside it
        lastTab.setNextFocusDownId(enabled ? R.id.reporting_toggle : R.id.open_accessibility);
        // Fires the listener only when the stored value differs, which then just writes it back
        reportingToggle.setChecked(reporting.isReportingOn());
        showReporting(reportingToggle.isChecked());
    }

    private void showReporting(boolean on) {
        dot.setImageResource(on ? R.drawable.dot_success : R.drawable.dot_paused);
        detail.setText(on ? R.string.service_enabled_detail : R.string.service_paused_detail);
    }

    private void openAccessibilitySettings() {
        // Some TV builds ship without this screen; the status text already points to the install script
        if (!PermissionScreens.open(activity, permissions, Permission.ACCESSIBILITY)) {
            openSettingsButton.setVisibility(View.GONE);
        }
    }
}
