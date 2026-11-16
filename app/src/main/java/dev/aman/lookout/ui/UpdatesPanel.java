package dev.aman.lookout.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.Context;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import dev.aman.lookout.R;
import dev.aman.lookout.setup.Permission;
import dev.aman.lookout.setup.SystemPermissions;
import dev.aman.lookout.update.ApkFetcher;
import dev.aman.lookout.update.GitHubReleases;
import dev.aman.lookout.update.InstalledVersion;
import dev.aman.lookout.update.Release;
import dev.aman.lookout.update.UpdateChannel;
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
    private final Activity activity;
    private final Executor background;
    private final UpdateChannel channel;
    private final SystemPermissions permissions;
    private final Consumer<Optional<Release>> onAvailabilityChanged;
    private final StatusLine status;
    private final MaterialButton checkButton;
    private final View progress;
    private final TextView installButton;
    private final Version installed;
    private final UpdateChecker checker = new UpdateChecker(GitHubReleases.ofLookout());
    private final ApkFetcher fetcher = new ApkFetcher();
    private final UpdateInstaller installer = new UpdateInstaller();

    private Release available;
    private boolean busy;
    private boolean recheckRequested;

    /** @param onAvailabilityChanged told after every check whether a newer release exists */
    public UpdatesPanel(Activity activity, Executor background, UpdateChannel channel, SystemPermissions permissions,
                        Consumer<Optional<Release>> onAvailabilityChanged) {
        this.activity = activity;
        this.permissions = permissions;
        this.background = background;
        this.channel = channel;
        this.onAvailabilityChanged = onAvailabilityChanged;
        status = new StatusLine(activity.findViewById(R.id.update_status));
        checkButton = activity.findViewById(R.id.check_updates);
        progress = activity.findViewById(R.id.update_progress);
        installButton = activity.findViewById(R.id.install_update);
        installed = Version.parse(InstalledVersion.name(activity));
        activity.<TextView>findViewById(R.id.version).setText(activity.getString(R.string.version, installed));
        checkButton.setOnClickListener(v -> check(false));
        installButton.setOnClickListener(v -> install());
        CompoundButton preReleases = activity.findViewById(R.id.include_pre_releases);
        preReleases.setChecked(channel.includesPreReleases());
        // The offer depends on the choice, so look again as soon as it changes
        preReleases.setOnCheckedChangeListener((button, checked) -> {
            channel.setIncludesPreReleases(checked);
            // A check or install already running used the old choice; look again once it ends
            if (busy) recheckRequested = true;
            else check(false);
        });
    }

    /** @param quiet leave the status alone when nothing is found or the network is down */
    public void check(boolean quiet) {
        if (busy) return;
        setBusy(true);
        if (!quiet) status.info(R.string.update_checking);
        background.execute(() -> {
            try {
                Optional<Release> newer = checker.newerThan(installed, channel.includesPreReleases());
                onUiThread(() -> showCheckResult(newer, quiet));
            } catch (IOException | RuntimeException e) {
                onUiThread(() -> showCheckFailure(quiet));
            }
        });
    }

    private void install() {
        if (busy || available == null) return;
        if (!permissions.isGranted(Permission.INSTALL_UPDATES)) {
            openInstallPermissionScreen();
            return;
        }
        Release release = available;
        // The download may outlive the screen, so it holds the application rather than the activity
        Context app = activity.getApplicationContext();
        setBusy(true);
        status.clear();
        progress.setVisibility(View.VISIBLE);
        background.execute(() -> {
            File apk = null;
            try {
                apk = fetcher.fetch(release, new File(app.getCacheDir(), "updates"));
                installer.install(app, apk);
                onUiThread(this::showInstallStarted);
            } catch (IOException | RuntimeException e) {
                onUiThread(this::showInstallFailure);
            } finally {
                if (apk != null) apk.delete(); // The installer session holds its own copy
            }
        });
    }

    // A result that arrives after the screen closed is dropped: its views are gone, and a requested recheck would
    // reach an executor that has been shut down
    private void onUiThread(Runnable task) {
        activity.runOnUiThread(() -> {
            if (!activity.isDestroyed()) task.run();
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
        recheckIfRequested();
    }

    private void showCheckFailure(boolean quiet) {
        setBusy(false);
        if (!quiet) status.error(R.string.update_check_failed);
        recheckIfRequested();
    }

    private void showInstallStarted() {
        setBusy(false);
        progress.setVisibility(View.GONE);
        status.success(R.string.update_installing);
        recheckIfRequested();
    }

    private void showInstallFailure() {
        setBusy(false);
        progress.setVisibility(View.GONE);
        status.error(R.string.update_download_failed);
        recheckIfRequested();
    }

    private void recheckIfRequested() {
        if (!recheckRequested) return;
        recheckRequested = false;
        check(false);
    }

    private void openInstallPermissionScreen() {
        try {
            Optional<Intent> screen = permissions.screenFor(Permission.INSTALL_UPDATES);
            if (screen.isPresent()) {
                activity.startActivity(screen.get());
                status.warning(R.string.update_allow_installs);
                return;
            }
        } catch (ActivityNotFoundException e) {
            // Treated like a missing screen below
        }
        // Some TV builds ship without this screen
        status.error(R.string.update_no_permission_screen);
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
}
