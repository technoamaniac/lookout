package dev.aman.lookout.ui;

import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import dev.aman.lookout.R;
import dev.aman.lookout.apps.AppCatalog;
import dev.aman.lookout.apps.IgnoredChoice;
import dev.aman.lookout.apps.InstalledApp;

/**
 * The Ignored apps row on the Settings tab. It holds the packages the form will save, says how many apps are ignored
 * once the TV's apps are listed, and opens the sheet to change them. Nothing is kept until Save, like the URL.
 */
final class IgnoredAppsPicker {
    private final View row;
    private final TextView summary;
    private final IgnoredAppsSheet sheet;
    private final Runnable onChanged;

    private List<String> ignored = new ArrayList<>();
    private List<String> reported = new ArrayList<>();
    // Listed once, in the background: asking Android for every launcher entry is slow enough to notice on a TV.
    // Both are set together on the UI thread, and null until then
    private List<InstalledApp> apps;
    private List<InstalledApp> builtIns;

    IgnoredAppsPicker(AppCompatActivity activity, AppCatalog catalog, AppCatalog builtInCatalog, Executor background,
                      Runnable onChanged) {
        this.onChanged = onChanged;
        row = activity.findViewById(R.id.ignored_row);
        summary = activity.findViewById(R.id.ignored_summary);
        sheet = new IgnoredAppsSheet(activity);
        row.setOnClickListener(v -> open());
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

    /** The packages to save as ignored. */
    List<String> ignored() {
        return Collections.unmodifiableList(ignored);
    }

    /** The ignored-by-default packages the user cleared, to save beside {@link #ignored()}. */
    List<String> reported() {
        return Collections.unmodifiableList(reported);
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
        IgnoredChoice choice = new IgnoredChoice(apps, builtIns, ignored, reported);
        sheet.show(choice, () -> {
            ignored = choice.ignoredPackages();
            reported = choice.reportedPackages();
            showSummary();
            onChanged.run();
        }, row::requestFocus);
    }

    private void showSummary() {
        // The count includes the ignored-by-default apps, so it waits for the listing that names them
        if (apps == null) return;
        int ticked = new IgnoredChoice(apps, builtIns, ignored, reported).tickedCount();
        summary.setText(summary.getResources().getQuantityString(R.plurals.ignored_summary, ticked, ticked));
    }
}
