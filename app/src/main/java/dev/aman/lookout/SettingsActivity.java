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
    private boolean sending;

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
        boolean enabled = serviceEnabled();
        service.setText(enabled ? R.string.service_enabled : R.string.service_disabled);
        service.setTextColor(getColor(enabled ? R.color.ok : R.color.warn));
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
        if (sending) return;
        Config config;
        try {
            config = readFields();
        } catch (IllegalArgumentException e) {
            status.setText(e.getMessage());
            return;
        }
        // Not Optional#isEmpty: that needs API 33 and minSdk is 28
        if (!config.webhookUrl().isPresent()) {
            status.setText(R.string.no_url);
            return;
        }
        sending = true;
        status.setText(R.string.sending);
        EventSink sink = new WebhookSink(config.webhookUrl().get());
        new Thread(() -> {
            DeliveryResult result = sink.send(new AppEvent(getPackageName() + ".test", System.currentTimeMillis() / 1000));
            runOnUiThread(() -> {
                sending = false;
                status.setText(messageFor(result));
            });
        }).start();
    }

    private static int messageFor(DeliveryResult result) {
        switch (result) {
            case DELIVERED:
                return R.string.test_ok;
            case PERMANENT_FAILURE:
                return R.string.test_rejected;
            default:
                return R.string.test_failed;
        }
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
