package dev.aman.lookout.ui;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import java.util.Optional;

import dev.aman.lookout.R;
import dev.aman.lookout.update.Release;

/** The alert in the panel header announcing a newer release. */
public final class UpdateBadge {
    private final TextView view;
    private Optional<Release> update = Optional.empty();
    private boolean onSettingsTab = true;

    /** @param onSelected runs when the user selects the badge */
    public UpdateBadge(Activity activity, Runnable onSelected) {
        view = activity.findViewById(R.id.update_badge);
        view.setOnClickListener(v -> onSelected.run());
    }

    public void show(Optional<Release> update) {
        this.update = update;
        update.ifPresent(release ->
                view.setText(view.getContext().getString(R.string.update_badge, release.version())));
        refresh();
    }

    /** The badge belongs to the Settings tab; on the others it is hidden, and on Updates it would repeat the panel. */
    public void setOnSettingsTab(boolean onSettingsTab) {
        this.onSettingsTab = onSettingsTab;
        refresh();
    }

    private void refresh() {
        view.setVisibility(update.isPresent() && onSettingsTab ? View.VISIBLE : View.GONE);
    }
}
