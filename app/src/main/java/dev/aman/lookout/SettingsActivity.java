package dev.aman.lookout;

import android.content.ComponentName;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import dev.aman.lookout.config.PrefsConfigStore;
import dev.aman.lookout.delivery.DeliveryLog;
import dev.aman.lookout.ui.DeliveriesPanel;
import dev.aman.lookout.ui.PanelHeader;
import dev.aman.lookout.ui.PanelTabs;
import dev.aman.lookout.ui.ServiceStatusPanel;
import dev.aman.lookout.ui.SettingsPanel;
import dev.aman.lookout.ui.UpdateBadge;
import dev.aman.lookout.ui.UpdatesPanel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** The app's one screen, shown in the TV's app list: a tab per panel. */
public class SettingsActivity extends AppCompatActivity {
    // Optional intent extras prefill the fields so ADB can seed a long URL; nothing is saved until Save
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_IGNORED = "ignored";

    private static final int UPDATES_TAB = 2;

    private final ExecutorService background = Executors.newSingleThreadExecutor();
    private ServiceStatusPanel serviceStatus;
    private DeliveriesPanel deliveries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        // Tab order matches the rail in the layout
        PanelTabs tabs = new PanelTabs(this,
                new int[] {R.id.tab_settings, R.id.tab_deliveries, R.id.tab_updates},
                new int[] {R.id.settings_panel, R.id.deliveries_panel, R.id.updates_panel});
        PanelHeader header = new PanelHeader(this);
        DeliveryLog log = DeliveryLog.shared();

        serviceStatus = new ServiceStatusPanel(this, new ComponentName(this, LookoutService.class));
        new SettingsPanel(this, new PrefsConfigStore(this), log, background,
                new SettingsPanel.Prefill(getIntent().getStringExtra(EXTRA_URL), getIntent().getStringExtra(EXTRA_IGNORED)));
        deliveries = new DeliveriesPanel(this, log);
        // The badge hides on the Updates tab, so focus must move off it before it goes
        UpdateBadge badge = new UpdateBadge(this, () -> tabs.focus(UPDATES_TAB));
        tabs.onSelected(tab -> {
            header.show(tab);
            badge.setSuppressed(tab == UPDATES_TAB);
        });
        new UpdatesPanel(this, background, badge::show).check(true);
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
        super.onDestroy();
    }
}
