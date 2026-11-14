package dev.aman.lookout;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.List;

/** Settings screen shown in the TV's app list. */
public class SettingsActivity extends AppCompatActivity {
    // Optional intent extras prefill the fields so ADB can seed a long URL; nothing is saved until Save
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_IGNORED = "ignored";

    private ConfigStore store;
    private Config saved;
    private EditText urlField;
    private EditText ignoredField;
    private TextView status;
    private View saveButton;
    private View testButton;
    private View accessibilityButton;
    private boolean sending;

    private final DeliveryLog deliveryLog = DeliveryLog.shared();
    private final DeliveriesAdapter deliveries = new DeliveriesAdapter();
    // The log notifies from the service's worker thread
    private final Runnable onDeliveryLogged = () -> runOnUiThread(this::showDeliveries);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        store = new PrefsConfigStore(this);
        urlField = findViewById(R.id.url);
        ignoredField = findViewById(R.id.ignored);
        status = findViewById(R.id.status);

        saveButton = findViewById(R.id.save);
        testButton = findViewById(R.id.test);
        accessibilityButton = findViewById(R.id.open_accessibility);
        accessibilityButton.setOnClickListener(v -> openAccessibilitySettings());
        ((RecyclerView) findViewById(R.id.deliveries)).setAdapter(deliveries);
        setUpTabs();

        saved = store.load();
        showText(urlField, orDefault(getIntent().getStringExtra(EXTRA_URL), saved.webhookUrlText()));
        showText(ignoredField, orDefault(getIntent().getStringExtra(EXTRA_IGNORED), saved.ignoredPackagesText()));
        onTextChanged(urlField, this::onFieldsEdited);
        onTextChanged(ignoredField, this::onFieldsEdited);
        saveButton.setOnClickListener(v -> save());
        testButton.setOnClickListener(v -> sendTest());
        updateSaveButton();
    }

    @Override
    protected void onStart() {
        super.onStart();
        deliveryLog.addListener(onDeliveryLogged);
        showDeliveries();
    }

    @Override
    protected void onStop() {
        deliveryLog.removeListener(onDeliveryLogged);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView service = findViewById(R.id.service_status);
        boolean enabled = serviceEnabled();
        service.setText(enabled ? R.string.service_enabled : R.string.service_disabled);
        service.setTextColor(getColor(enabled ? R.color.ok : R.color.warn));
        accessibilityButton.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }

    private void setUpTabs() {
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                boolean settings = tab.getPosition() == 0;
                findViewById(R.id.settings_panel).setVisibility(settings ? View.VISIBLE : View.GONE);
                findViewById(R.id.deliveries_panel).setVisibility(settings ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showDeliveries() {
        List<DeliveryRecord> recent = deliveryLog.recent();
        deliveries.show(recent);
        findViewById(R.id.deliveries_empty).setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (ActivityNotFoundException e) {
            // Some TV builds ship without this screen; the install script is the fallback
            showError(getString(R.string.service_disabled));
        }
    }

    private void save() {
        Config config;
        try {
            config = readFields();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return;
        }
        store.save(config);
        saved = config;
        // Show the normalised values (trimmed, de-duplicated) rather than what was typed
        showText(urlField, config.webhookUrlText());
        showText(ignoredField, config.ignoredPackagesText());
        updateSaveButton();
        showStatus(getString(R.string.saved), R.color.ok);
    }

    private void sendTest() {
        if (sending) return;
        Config config;
        try {
            config = readFields();
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return;
        }
        // Not Optional#isEmpty: that needs API 33 and minSdk is 28
        if (!config.webhookUrl().isPresent()) {
            showError(getString(R.string.no_url));
            return;
        }
        sending = true;
        testButton.setEnabled(false);
        showStatus(getString(R.string.sending), R.color.accent);
        // Recorded too, so a test shows up in the Deliveries tab
        EventSink sink = new RecordingSink(new WebhookSink(config.webhookUrl().get()), deliveryLog,
                System::currentTimeMillis);
        new Thread(() -> {
            DeliveryResult result = sink.send(new AppEvent(getPackageName() + ".test", System.currentTimeMillis() / 1000));
            runOnUiThread(() -> {
                sending = false;
                testButton.setEnabled(true);
                if (result == DeliveryResult.DELIVERED) {
                    showStatus(getString(R.string.test_ok), R.color.ok);
                } else {
                    showError(getString(messageFor(result)));
                }
            });
        }).start();
    }

    private static int messageFor(DeliveryResult result) {
        return result == DeliveryResult.PERMANENT_FAILURE ? R.string.test_rejected : R.string.test_failed;
    }

    // Typing invalidates any earlier result, and Save is only useful while something differs
    private void onFieldsEdited() {
        status.setText("");
        updateSaveButton();
    }

    private void updateSaveButton() {
        saveButton.setEnabled(hasUnsavedChanges());
    }

    private boolean hasUnsavedChanges() {
        try {
            Config current = readFields();
            return !current.webhookUrlText().equals(saved.webhookUrlText())
                    || !current.ignoredPackagesText().equals(saved.ignoredPackagesText());
        } catch (IllegalArgumentException e) {
            return true; // Let the user press Save to see what is wrong
        }
    }

    private void showError(String message) {
        showStatus(message, R.color.warn);
    }

    private void showStatus(String message, int colorRes) {
        status.setText(message);
        status.setTextColor(getColor(colorRes));
    }

    // Park the cursor at the start so a long value shows its beginning, not its tail
    private static void showText(EditText field, String text) {
        field.setText(text);
        field.setSelection(0);
    }

    private static void onTextChanged(EditText field, Runnable callback) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                callback.run();
            }
        });
    }

    private Config readFields() {
        return Config.parse(urlField.getText().toString(), ignoredField.getText().toString());
    }

    private boolean serviceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        // Compare whole entries: a substring match would accept e.g. ".LookoutServiceX", and
        // entries may use the short "pkg/.Class" form
        ComponentName self = new ComponentName(this, LookoutService.class);
        for (String entry : enabled.split(":")) {
            if (self.equals(ComponentName.unflattenFromString(entry))) return true;
        }
        return false;
    }

    private static String orDefault(String value, String fallback) {
        return value != null ? value : fallback;
    }
}
