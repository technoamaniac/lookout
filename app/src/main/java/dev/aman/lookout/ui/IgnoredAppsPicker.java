package dev.aman.lookout.ui;

import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import dev.aman.lookout.R;
import dev.aman.lookout.apps.AppCatalog;
import dev.aman.lookout.apps.IgnoredChoice;
import dev.aman.lookout.apps.InstalledApp;

/**
 * The Ignored apps row on the Settings tab. It holds the packages the form will save, shows them by name once the
 * TV's apps are listed, and opens the sheet to change them. Nothing is kept until Save, like the URL.
 */
final class IgnoredAppsPicker {
    private final View row;
    private final TextView summary;
    private final IgnoredAppsSheet sheet;
    private final Runnable onChanged;
    private final OnBackPressedCallback closeSheetOnBack;

    private List<String> ignored = new ArrayList<>();
    private List<String> reported = new ArrayList<>();
    // Listed once, in the background: asking Android for every launcher entry is slow enough to notice on a TV
    private volatile List<InstalledApp> apps;
    private volatile List<InstalledApp> builtIns;

    IgnoredAppsPicker(AppCompatActivity activity, AppCatalog catalog, AppCatalog builtInCatalog, Executor background,
                      Runnable onChanged) {
        this.onChanged = onChanged;
        row = activity.findViewById(R.id.ignored_row);
        summary = activity.findViewById(R.id.ignored_summary);
        sheet = new IgnoredAppsSheet(activity);
        row.setOnClickListener(v -> open());
        // Back closes the open sheet; otherwise the callback is off and Back behaves as usual
        closeSheetOnBack = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                sheet.hide();
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, closeSheetOnBack);
        background.execute(() -> {
            List<InstalledApp> listed = catalog.apps();
            List<InstalledApp> defaults = builtInCatalog.apps();
            activity.runOnUiThread(() -> {
                if (activity.isDestroyed()) return;
                apps = listed;
                builtIns = defaults;
                showSummary();
            });
        });
        showSummary();
    }

    /** The packages to save, as the comma-separated text the config reads. */
    String text() {
        return String.join(", ", ignored);
    }

    /** The ignored-by-default packages the user cleared, to save beside {@link #text()}. */
    List<String> reported() {
        return reported;
    }

    void set(List<String> ignoredPackages, List<String> reportedPackages) {
        ignored = new ArrayList<>(ignoredPackages);
        reported = new ArrayList<>(reportedPackages);
        showSummary();
    }

    void setEnabled(boolean enabled) {
        row.setEnabled(enabled);
    }

    private void open() {
        // Listing takes well under a second; a press before then is ignored rather than opening a sheet later,
        // over whatever the user has moved on to
        if (apps == null) return;
        closeSheetOnBack.setEnabled(true);
        IgnoredChoice choice = new IgnoredChoice(apps, builtIns, ignored, reported);
        sheet.show(choice, () -> {
            ignored = choice.ignoredPackages();
            reported = choice.reportedPackages();
            showSummary();
            onChanged.run();
        }, () -> {
            closeSheetOnBack.setEnabled(false);
            row.requestFocus();
        });
    }

    private void showSummary() {
        // Names once the apps are listed, packages until then
        List<String> shown = apps == null ? ignored : new IgnoredChoice(apps, builtIns, ignored, reported).ignoredLabels();
        if (shown.isEmpty()) summary.setText(R.string.ignored_none);
        else summary.setText(String.join(", ", shown));
    }
}
