package dev.aman.lookout;

import android.content.ComponentName;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import dev.aman.lookout.config.PrefsConfigStore;
import dev.aman.lookout.delivery.DeliveryLog;
import dev.aman.lookout.delivery.WebhookTester;
import dev.aman.lookout.setup.PrefsSetupStore;
import dev.aman.lookout.setup.SystemPermissions;
import dev.aman.lookout.ui.DeliveriesPanel;
import dev.aman.lookout.ui.PanelHeader;
import dev.aman.lookout.ui.PanelTabs;
import dev.aman.lookout.ui.RailNavigator;
import dev.aman.lookout.ui.ServiceStatusPanel;
import dev.aman.lookout.ui.SettingsPanel;
import dev.aman.lookout.ui.Tab;
import dev.aman.lookout.ui.UpdateBadge;
import dev.aman.lookout.ui.UpdatesPanel;
import dev.aman.lookout.update.PrefsUpdateChannel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** The app's one screen, shown in the TV's app list: a tab per panel. */
public class SettingsActivity extends AppCompatActivity {
    // Optional intent extras prefill the fields so ADB can seed a long URL; nothing is saved until Save
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_IGNORED = "ignored";
    /** Opens on the Updates tab, which shows the version just installed. */
    public static final String EXTRA_SHOW_UPDATES = "show_updates";

    private final ExecutorService background = Executors.newSingleThreadExecutor();
    // Its own thread, so a test before saving never waits behind an update check or download
    private final ExecutorService webhookTests = Executors.newSingleThreadExecutor();
    private ServiceStatusPanel serviceStatus;
    private DeliveriesPanel deliveries;
    private RailNavigator navigator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        PanelTabs tabs = new PanelTabs(this);
        navigator = new RailNavigator(this, tabs);
        // Added before the panels, so a sheet that is open closes first on Back
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (navigator.leaveOnBack()) finish();
            }
        });
        SetupLauncher setup = new SetupLauncher(this, new PrefsSetupStore(this));
        View permissionsButton = findViewById(R.id.open_permissions);
        setup.openFrom(permissionsButton);
        PanelHeader header = new PanelHeader(this);
        DeliveryLog log = ((LookoutApplication) getApplication()).deliveryLog();

        PrefsConfigStore store = new PrefsConfigStore(this);
        SystemPermissions permissions = new SystemPermissions(this, new ComponentName(this, LookoutService.class));
        serviceStatus = new ServiceStatusPanel(this, permissions, store);
        new SettingsPanel(this, store, new WebhookTester(log, System::currentTimeMillis), webhookTests,
                new SettingsPanel.Prefill(getIntent().getStringExtra(EXTRA_URL), getIntent().getStringExtra(EXTRA_IGNORED)));
        deliveries = new DeliveriesPanel(this, log);
        // The badge hides on the Updates tab, so focus must move off it before it goes
        UpdateBadge badge = new UpdateBadge(this, () -> tabs.focus(Tab.UPDATES));
        tabs.onSelected(tab -> {
            header.show(tab);
            permissionsButton.setVisibility(tab == Tab.SETTINGS ? View.VISIBLE : View.GONE);
            badge.setSuppressed(tab == Tab.UPDATES);
            deliveries.setSelected(tab == Tab.DELIVERIES);
        });
        new UpdatesPanel(this, background, new PrefsUpdateChannel(this), permissions, badge::show).check(true);
        // Only when first opened: a recreated screen keeps the tab the user moved to
        if (savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_SHOW_UPDATES, false)) {
            tabs.focus(Tab.UPDATES);
        }
        // A URL sent from a computer means the user is already setting Lookout up that way
        if (savedInstanceState == null) {
            setup.openIfFirstRun(store.load().webhookUrl().isPresent() || getIntent().hasExtra(EXTRA_URL));
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return navigator.onKey(event) || super.dispatchKeyEvent(event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        deliveries.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        serviceStatus.refresh();
    }

    @Override
    protected void onStop() {
        deliveries.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        background.shutdownNow();
        webhookTests.shutdownNow();
        super.onDestroy();
    }
}
