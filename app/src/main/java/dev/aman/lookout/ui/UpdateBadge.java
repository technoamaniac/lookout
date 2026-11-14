package dev.aman.lookout.ui;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import dev.aman.lookout.R;
import dev.aman.lookout.update.Release;

import java.util.Optional;

/** The corner alert announcing a newer release. */
public final class UpdateBadge {
    private final TextView view;

    /** @param onSelected runs when the user selects the badge */
    public UpdateBadge(Activity activity, Runnable onSelected) {
        view = activity.findViewById(R.id.update_badge);
        view.setOnClickListener(v -> onSelected.run());
    }

    public void show(Optional<Release> update) {
        view.setVisibility(update.isPresent() ? View.VISIBLE : View.GONE);
        update.ifPresent(release ->
                view.setText(view.getContext().getString(R.string.update_badge, release.version())));
    }
}
