package dev.aman.lookout.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import dev.aman.lookout.R;
import dev.aman.lookout.update.ApkFetcher;
import dev.aman.lookout.update.GitHubReleases;
import dev.aman.lookout.update.Release;
import dev.aman.lookout.update.UpdateChecker;
import dev.aman.lookout.update.UpdateInstaller;
import dev.aman.lookout.update.Version;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/** The Updates tab: looks for a newer release and installs it. */
public final class UpdatesPanel {
    private static final String GITHUB = "https://github.com";
    private static final String REPO = "technoamaniac/lookout";

    private final Activity activity;
    private final Executor background;
    private final Consumer<Optional<Release>> onAvailabilityChanged;
    private final StatusLine status;
    private final MaterialButton checkButton;
    private final View progress;
    private final TextView installButton;
    private final Version installed;
    private final UpdateChecker checker = new UpdateChecker(new GitHubReleases(GITHUB, REPO));
    private final ApkFetcher fetcher = new ApkFetcher();
    private final UpdateInstaller installer = new UpdateInstaller();

    private Release available;
    private boolean busy;

    /** @param onAvailabilityChanged told after every check whether a newer release exists */
    public UpdatesPanel(Activity activity, Executor background, Consumer<Optional<Release>> onAvailabilityChanged) {
        this.activity = activity;
        this.background = background;
        this.onAvailabilityChanged = onAvailabilityChanged;
        status = new StatusLine(activity.findViewById(R.id.update_status));
        checkButton = activity.findViewById(R.id.check_updates);
        progress = activity.findViewById(R.id.update_progress);
        installButton = activity.findViewById(R.id.install_update);
        installed = installedVersion();
        activity.<TextView>findViewById(R.id.version).setText(activity.getString(R.string.version, installed));
        checkButton.setOnClickListener(v -> check(false));
        installButton.setOnClickListener(v -> install());
    }

    /** @param quiet leave the status alone when nothing is found or the network is down */
    public void check(boolean quiet) {
        if (busy) return;
        setBusy(true);
        if (!quiet) status.info(R.string.update_checking);
        background.execute(() -> {
            try {
                Optional<Release> newer = checker.newerThan(installed);
                activity.runOnUiThread(() -> showCheckResult(newer, quiet));
            } catch (IOException | RuntimeException e) {
                activity.runOnUiThread(() -> showCheckFailure(quiet));
            }
        });
    }

    private void install() {
        if (busy || available == null) return;
        if (!activity.getPackageManager().canRequestPackageInstalls()) {
            openInstallPermissionScreen();
            return;
        }
        Release release = available;
        setBusy(true);
        status.clear();
        progress.setVisibility(View.VISIBLE);
        background.execute(() -> {
            File apk = null;
            try {
                apk = fetcher.fetch(release, new File(activity.getCacheDir(), "updates"));
                installer.install(activity, apk);
                activity.runOnUiThread(this::showInstallStarted);
            } catch (IOException | RuntimeException e) {
                activity.runOnUiThread(this::showInstallFailure);
            } finally {
                if (apk != null) apk.delete(); // The installer session holds its own copy
            }
        });
    }

    private void showCheckResult(Optional<Release> newer, boolean quiet) {
        setBusy(false);
        available = newer.orElse(null);
        installButton.setVisibility(newer.isPresent() ? View.VISIBLE : View.GONE);
        // Install is the primary action once there is something to install
        useSecondaryStyle(checkButton, newer.isPresent());
        onAvailabilityChanged.accept(newer);
        if (newer.isPresent()) {
            installButton.setText(activity.getString(R.string.install_version, newer.get().version()));
            status.info(activity.getString(R.string.update_available, newer.get().version()), R.drawable.ic_tab_updates);
        } else if (!quiet) {
            status.success(R.string.update_current);
        }
    }

    private void showCheckFailure(boolean quiet) {
        setBusy(false);
        if (!quiet) status.error(R.string.update_check_failed);
    }

    private void showInstallStarted() {
        setBusy(false);
        progress.setVisibility(View.GONE);
        status.success(R.string.update_installing);
    }

    private void showInstallFailure() {
        setBusy(false);
        progress.setVisibility(View.GONE);
        status.error(R.string.update_download_failed);
    }

    private void openInstallPermissionScreen() {
        try {
            activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.getPackageName())));
            status.warning(R.string.update_allow_installs);
        } catch (ActivityNotFoundException e) {
            // Some TV builds ship without this screen
            status.error(R.string.update_no_permission_screen);
        }
    }

    private static void useSecondaryStyle(MaterialButton button, boolean secondary) {
        Context context = button.getContext();
        button.setBackgroundTintList(context.getColorStateList(secondary ? R.color.secondary_bg : R.color.primary_bg));
        button.setStrokeColor(context.getColorStateList(secondary ? R.color.secondary_stroke_state : R.color.primary_stroke));
        button.setTextColor(context.getColorStateList(secondary ? R.color.secondary_text : R.color.primary_text));
        button.setIconTint(context.getColorStateList(secondary ? R.color.secondary_text : R.color.primary_text));
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        // A disabled view cannot keep focus, and focus would fall back to the first tab (Settings)
        // and switch panels; park it on the Updates tab instead
        if (busy && (checkButton.hasFocus() || installButton.hasFocus())) {
            activity.findViewById(R.id.tab_updates).requestFocus();
        }
        checkButton.setEnabled(!busy);
        installButton.setEnabled(!busy);
    }

    private Version installedVersion() {
        try {
            return Version.parse(activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("The app is not installed under its own package name", e);
        }
    }
}
