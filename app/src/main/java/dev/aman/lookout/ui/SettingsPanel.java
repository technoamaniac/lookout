package dev.aman.lookout.ui;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import dev.aman.lookout.R;
import dev.aman.lookout.config.Config;
import dev.aman.lookout.config.ConfigStore;
import dev.aman.lookout.config.DeviceName;
import dev.aman.lookout.delivery.DeliveryLog;
import dev.aman.lookout.delivery.DeliveryResult;
import dev.aman.lookout.delivery.EventSink;
import dev.aman.lookout.delivery.RecordingSink;
import dev.aman.lookout.delivery.WebhookSink;
import dev.aman.lookout.foreground.AppEvent;

import java.util.concurrent.Executor;

/** The Settings tab: the webhook form, with Save and Send test. */
public final class SettingsPanel {
    private final Activity activity;
    private final ConfigStore store;
    private final DeliveryLog log;
    private final Executor background;
    private final EditText urlField;
    private final EditText ignoredField;
    private final CompoundButton deviceNameSwitch;
    private final StatusLine status;
    private final View saveButton;
    private final View testButton;
    private final View saveHint;
    private final View urlHelper;
    private final TextView urlError;

    private Config saved;
    private boolean sending;

    /** @param prefill optional values that override the saved ones until the user saves */
    public SettingsPanel(Activity activity, ConfigStore store, DeliveryLog log, Executor background,
                         Prefill prefill) {
        this.activity = activity;
        this.store = store;
        this.log = log;
        this.background = background;
        urlField = activity.findViewById(R.id.url);
        ignoredField = activity.findViewById(R.id.ignored);
        deviceNameSwitch = activity.findViewById(R.id.send_device_name);
        status = new StatusLine(activity.findViewById(R.id.status));
        saveButton = activity.findViewById(R.id.save);
        testButton = activity.findViewById(R.id.test);
        saveHint = activity.findViewById(R.id.save_hint);
        urlHelper = activity.findViewById(R.id.url_helper);
        urlError = activity.findViewById(R.id.url_error);

        saved = store.load();
        showText(urlField, prefill.url != null ? prefill.url : saved.webhookUrlText());
        showText(ignoredField, prefill.ignored != null ? prefill.ignored : saved.ignoredPackagesText());
        onTextChanged(urlField, this::onFieldsEdited);
        onTextChanged(ignoredField, this::onFieldsEdited);
        deviceNameSwitch.setChecked(saved.sendDeviceName());
        deviceNameSwitch.setOnCheckedChangeListener((button, checked) -> onFieldsEdited());
        saveButton.setOnClickListener(v -> save());
        testButton.setOnClickListener(v -> sendTest());
        updateSaveButton();
        // An ADB prefill is waiting to be saved, so the next step is Save
        if (prefill.url != null && saveButton.isEnabled()) saveButton.requestFocus();
    }

    /** Field values supplied from outside, such as ADB extras; null leaves a field as saved. */
    public static final class Prefill {
        final String url;
        final String ignored;

        public Prefill(String url, String ignored) {
            this.url = url;
            this.ignored = ignored;
        }
    }

    private void save() {
        Config config;
        try {
            config = readFields();
        } catch (IllegalArgumentException e) {
            showUrlError(e.getMessage());
            return;
        }
        store.save(config);
        saved = config;
        // Show the normalised values (trimmed, de-duplicated) rather than what was typed
        showText(urlField, config.webhookUrlText());
        showText(ignoredField, config.ignoredPackagesText());
        status.success(R.string.saved);
        updateSaveButton();
        // The disabled Save button gave up focus; the next likely step is a test
        testButton.requestFocus();
    }

    private void sendTest() {
        if (sending) return;
        Config config;
        try {
            config = readFields();
        } catch (IllegalArgumentException e) {
            showUrlError(e.getMessage());
            return;
        }
        // Not Optional#isEmpty: that needs API 33 and minSdk is 28
        if (!config.webhookUrl().isPresent()) {
            showUrlError(activity.getString(R.string.no_url));
            return;
        }
        setSending(true);
        status.info(R.string.sending);
        // Recorded too, so a test shows up in the Deliveries tab
        EventSink sink = new RecordingSink(new WebhookSink(config.webhookUrl().get()), log,
                System::currentTimeMillis);
        AppEvent testEvent = new AppEvent(activity.getPackageName() + ".test", System.currentTimeMillis() / 1000)
                .withActivity(activity.getClass().getName())
                .withDevice(config.sendDeviceName() ? DeviceName.read(activity.getContentResolver()) : null);
        background.execute(() -> {
            DeliveryResult result = sink.send(testEvent);
            activity.runOnUiThread(() -> showTestResult(result));
        });
    }

    private void showTestResult(DeliveryResult result) {
        setSending(false);
        switch (result) {
            case DELIVERED:
                status.success(R.string.test_ok);
                break;
            case PERMANENT_FAILURE:
                status.error(R.string.test_rejected);
                break;
            default:
                status.error(R.string.test_failed);
        }
    }

    private void setSending(boolean sending) {
        this.sending = sending;
        testButton.setEnabled(!sending);
    }

    // Typing invalidates any earlier result, and Save is only useful while something differs
    private void onFieldsEdited() {
        status.clear();
        showUrlError(null);
        updateSaveButton();
    }

    // A problem with the form belongs under the field, not in the result chip
    private void showUrlError(String message) {
        boolean failed = message != null;
        urlError.setText(message);
        urlError.setVisibility(failed ? View.VISIBLE : View.GONE);
        urlHelper.setVisibility(failed ? View.GONE : View.VISIBLE);
        urlField.setActivated(failed);
    }

    private void updateSaveButton() {
        boolean changed = hasUnsavedChanges();
        saveButton.setEnabled(changed);
        saveHint.setVisibility(!changed && !status.isShowing() ? View.VISIBLE : View.GONE);
    }

    private boolean hasUnsavedChanges() {
        try {
            Config current = readFields();
            return !current.webhookUrlText().equals(saved.webhookUrlText())
                    || !current.ignoredPackagesText().equals(saved.ignoredPackagesText())
                    || current.sendDeviceName() != saved.sendDeviceName();
        } catch (IllegalArgumentException e) {
            return true; // Let the user press Save to see what is wrong
        }
    }

    private Config readFields() {
        return Config.parse(urlField.getText().toString(), ignoredField.getText().toString(),
                deviceNameSwitch.isChecked());
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
}
