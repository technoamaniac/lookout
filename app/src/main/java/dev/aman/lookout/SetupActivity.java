package dev.aman.lookout;

import android.content.ComponentName;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import dev.aman.lookout.playback.MediaAccessService;
import dev.aman.lookout.setup.AdbCommand;
import dev.aman.lookout.setup.PrefsSetupStore;
import dev.aman.lookout.setup.SetupStore;
import dev.aman.lookout.setup.SystemPermissions;
import dev.aman.lookout.ui.PermissionsPanel;
import dev.aman.lookout.ui.WelcomePanel;

/**
 * First-run setup: a welcome screen, then the permissions. Opens by itself for a new install, and the Permissions
 * button on the Settings tab opens it again on the permissions alone.
 */
public class SetupActivity extends AppCompatActivity {
    /** Skips the welcome screen, for coming back to check or change a permission. */
    public static final String EXTRA_PERMISSIONS_ONLY = "permissions_only";
    private static final String STATE_ON_PERMISSIONS = "on_permissions";

    private SetupStore store;
    private WelcomePanel welcome;
    private PermissionsPanel permissions;
    private TextView step;
    private boolean permissionsOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        store = new PrefsSetupStore(this);
        permissionsOnly = getIntent().getBooleanExtra(EXTRA_PERMISSIONS_ONLY, false);
        step = findViewById(R.id.setup_step);

        // Added before the panels, so a sheet that is open closes first on Back
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (welcome.isShowing() || permissionsOnly) finishSetup();
                else showWelcome();
            }
        });

        ComponentName media = new ComponentName(this, MediaAccessService.class);
        SystemPermissions system = new SystemPermissions(this, new ComponentName(this, LookoutService.class));
        permissions = new PermissionsPanel(this, system, new AdbCommand(getPackageName(), media.flattenToString()),
                this::finishSetup, permissionsOnly ? this::finishSetup : this::showWelcome);
        welcome = new WelcomePanel(this, this::showPermissions, this::finishSetup);

        if (permissionsOnly || savedInstanceState != null && savedInstanceState.getBoolean(STATE_ON_PERMISSIONS)) {
            showPermissions();
        } else {
            showWelcome();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_ON_PERMISSIONS, !welcome.isShowing());
    }

    // The user may have granted something in system settings
    @Override
    protected void onResume() {
        super.onResume();
        permissions.refresh();
    }

    private void showWelcome() {
        permissions.hide();
        step.setText(getString(R.string.setup_step, 1));
        step.setVisibility(View.VISIBLE);
        welcome.show();
    }

    private void showPermissions() {
        welcome.hide();
        step.setText(getString(R.string.setup_step, 2));
        step.setVisibility(permissionsOnly ? View.GONE : View.VISIBLE);
        permissions.show();
    }

    // Done, Skip and Back all count as having seen it: setup never reopens by itself
    private void finishSetup() {
        store.markSeen();
        finish();
    }
}
