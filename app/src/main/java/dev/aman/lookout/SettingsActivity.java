package dev.aman.lookout;

import android.content.ComponentName;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/** Settings screen shown in the TV's app list. */
public class SettingsActivity extends AppCompatActivity {
    // Optional intent extras prefill the fields so ADB can seed a long URL; nothing is saved until Save
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_IGNORED = "ignored";

    private ConfigStore store;
    private EditText urlField;
    private EditText ignoredField;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        store = new PrefsConfigStore(this);
        urlField = findViewById(R.id.url);
        ignoredField = findViewById(R.id.ignored);
        status = findViewById(R.id.status);

        Config config = store.load();
        urlField.setText(orDefault(getIntent().getStringExtra(EXTRA_URL), config.webhookUrlText()));
        ignoredField.setText(orDefault(getIntent().getStringExtra(EXTRA_IGNORED), config.ignoredPackagesText()));
        findViewById(R.id.save).setOnClickListener(v -> save());
        findViewById(R.id.test).setOnClickListener(v -> sendTest());
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView service = findViewById(R.id.service_status);
        service.setText(serviceEnabled() ? R.string.service_enabled : R.string.service_disabled);
    }

    private void save() {
        try {
            store.save(readFields());
            status.setText(R.string.saved);
        } catch (IllegalArgumentException e) {
            status.setText(e.getMessage());
        }
    }

    private void sendTest() {
        Config config;
        try {
            config = readFields();
        } catch (IllegalArgumentException e) {
            status.setText(e.getMessage());
            return;
        }
        if (config.webhookUrl().isEmpty()) {
            status.setText(R.string.no_url);
            return;
        }
        status.setText(R.string.sending);
        EventSink sink = new WebhookSink(config.webhookUrl().get());
        new Thread(() -> {
            boolean ok = sink.send(new AppEvent(getPackageName() + ".test", System.currentTimeMillis() / 1000));
            runOnUiThread(() -> status.setText(ok ? R.string.test_ok : R.string.test_failed));
        }).start();
    }

    private Config readFields() {
        return Config.parse(urlField.getText().toString(), ignoredField.getText().toString());
    }

    private boolean serviceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabled != null && enabled.contains(new ComponentName(this, LookoutService.class).flattenToString());
    }

    private static String orDefault(String value, String fallback) {
        return value != null ? value : fallback;
    }
}
