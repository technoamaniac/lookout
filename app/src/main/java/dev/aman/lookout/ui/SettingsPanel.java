package dev.aman.lookout.ui;

import android.app.Activity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import dev.aman.lookout.R;
import dev.aman.lookout.config.Config;
import dev.aman.lookout.config.ConfigStore;
import dev.aman.lookout.config.DeviceName;
import dev.aman.lookout.config.SaveGate;
import dev.aman.lookout.delivery.DeliveryResult;
import dev.aman.lookout.delivery.WebhookTester;
import dev.aman.lookout.foreground.AppEvent;

import java.util.concurrent.Executor;

/** The Settings tab: the webhook form, with Save and Test webhook. A new webhook URL must pass a test before Save. */
public final class SettingsPanel {
    private final Activity activity;
    private final ConfigStore store;
    private final WebhookTester tester;
    private final Executor background;
    private final EditText urlField;
    private final EditText ignoredField;
    private final CompoundButton deviceNameSwitch;
    private final StatusLine status;
    private final View saveButton;
    private final View testButton;
    private final TextView saveHint;
    private final View urlHelper;
    private final TextView urlError;

    private final SaveGate gate;
    private boolean sending;

    /** @param prefill optional values that override the saved ones until the user saves */
    public SettingsPanel(Activity activity, ConfigStore store, WebhookTester tester, Executor background,
                         Prefill prefill) {
        this.activity = activity;
        this.store = store;
        this.tester = tester;
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

        Config saved = store.load();
        gate = new SaveGate(saved);
        showUrl(prefill.url != null ? prefill.url.trim() : saved.webhookUrlText());
        TextFields.show(ignoredField, prefill.ignored != null ? prefill.ignored : saved.ignoredPackagesText());
        TextFields.onChanged(urlField, this::onFieldsEdited);
        TextFields.onChanged(ignoredField, this::onFieldsEdited);
        deviceNameSwitch.setChecked(saved.sendDeviceName());
        deviceNameSwitch.setOnCheckedChangeListener((button, checked) -> onFieldsEdited());
        saveButton.setOnClickListener(v -> save());
        testButton.setOnClickListener(v -> sendTest());
        updateSaveButton();
        // An ADB prefill is waiting to be tested or saved, so focus the next step; without a URL that is typing one
        if (prefill.url != null || prefill.ignored != null) {
            if (saveButton.isEnabled()) saveButton.requestFocus();
            else if (testButton.isEnabled()) testButton.requestFocus();
            else urlField.requestFocus();
        }
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
        Config config = readFieldsOrNull();
        // Save is disabled otherwise, fields that do not parse included; this guards a press that raced the update
        if (gate.stateOf(config) != SaveGate.State.SAVEABLE) return;
        store.save(config);
        gate.onSaved(config);
        // Show the normalised values (trimmed, de-duplicated) rather than what was typed
        showUrl(config.webhookUrlText());
        TextFields.show(ignoredField, config.ignoredPackagesText());
        status.success(R.string.saved);
        updateSaveButton();
        // The disabled Save button gave up focus; Test webhook is the one action left
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
        // Not Optional#isEmpty: that needs API 33 and minSdk is 28. The button is off without a URL; this guards
        // a press that raced the update
        if (!config.webhookUrl().isPresent()) return;
        // Status first, so the lock's hint update sees it showing and hides "No unsaved changes" beside it
        status.info(R.string.sending);
        setSending(true);
        AppEvent testEvent = new AppEvent(activity.getPackageName() + ".test", System.currentTimeMillis() / 1000)
                .withActivity(activity.getClass().getName())
                .withDevice(config.sendDeviceName() ? DeviceName.read(activity.getContentResolver()) : null);
        background.execute(() -> {
            DeliveryResult result = tester.test(config.webhookUrl().get(), testEvent);
            activity.runOnUiThread(() -> {
                if (activity.isDestroyed()) return;
                gate.onTested(config, result == DeliveryResult.DELIVERED);
                setSending(false);
                showTestResult(result, config);
            });
        });
    }

    private void showTestResult(DeliveryResult result, Config tested) {
        switch (result) {
            case DELIVERED:
                // A new URL has just become saveable, so Save is the next step
                if (gate.isNewUrlReadyToSave(tested)) {
                    status.success(R.string.test_ok_save_next);
                    saveButton.requestFocus();
                    return;
                }
                status.success(R.string.test_ok);
                break;
            case PERMANENT_FAILURE:
                status.error(R.string.test_rejected);
                break;
            default:
                status.error(R.string.test_failed);
        }
        testButton.requestFocus();
    }

    // The form is locked while a test runs, so the URL cannot change under it. Test webhook stays enabled to keep
    // the D-pad focus on it, which a disabled view would give up; sendTest ignores presses until the result
    private void setSending(boolean sending) {
        this.sending = sending;
        urlField.setEnabled(!sending);
        ignoredField.setEnabled(!sending);
        deviceNameSwitch.setEnabled(!sending);
        updateSaveButton();
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

    // Save stays off until a new URL has passed a test, and the hint beside the buttons says so
    private void updateSaveButton() {
        SaveGate.State state = gate.stateOf(readFieldsOrNull());
        testButton.setEnabled(state != SaveGate.State.NO_URL);
        saveButton.setEnabled(state == SaveGate.State.SAVEABLE && !sending);
        switch (state) {
            case NO_URL:
                saveHint.setText(R.string.no_url);
                saveHint.setVisibility(View.VISIBLE);
                break;
            case NEEDS_TEST:
                saveHint.setText(R.string.save_needs_test);
                saveHint.setVisibility(sending ? View.GONE : View.VISIBLE);
                break;
            default:
                saveHint.setText(R.string.no_unsaved_changes);
                saveHint.setVisibility(state == SaveGate.State.UNCHANGED && !status.isShowing()
                        ? View.VISIBLE : View.GONE);
        }
    }

    private Config readFields() {
        return Config.parse(urlField.getText().toString(), ignoredField.getText().toString(),
                deviceNameSwitch.isChecked());
    }

    private Config readFieldsOrNull() {
        try {
            return readFields();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // An empty field starts with https://, with the cursor after it, since that is the only scheme allowed
    private void showUrl(String url) {
        if (url.isEmpty()) {
            urlField.setText(Config.URL_PREFIX);
            urlField.setSelection(Config.URL_PREFIX.length());
        } else {
            TextFields.show(urlField, url);
        }
    }
}
