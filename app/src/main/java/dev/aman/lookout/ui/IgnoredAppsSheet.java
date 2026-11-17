package dev.aman.lookout.ui;

import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import dev.aman.lookout.R;
import dev.aman.lookout.apps.IgnoredChoice;

/** The sheet over the Settings tab where apps are ticked to be ignored. */
final class IgnoredAppsSheet {
    private final Sheet sheet;
    private final RecyclerView list;
    private final TextView count;
    private final View done;
    private final IgnoredAppsAdapter adapter;
    private IgnoredChoice choice;
    private Runnable onChanged = () -> {};

    IgnoredAppsSheet(AppCompatActivity activity) {
        sheet = new Sheet(activity, R.id.ignored_overlay);
        list = activity.findViewById(R.id.ignored_list);
        count = activity.findViewById(R.id.ignored_count);
        adapter = new IgnoredAppsAdapter(activity.getPackageManager(), this::toggle);
        list.setLayoutManager(new LinearLayoutManager(activity));
        list.setAdapter(adapter);
        // Items are redrawn in place, so the default fade would flash a row on every press
        list.setItemAnimator(null);
        done = activity.findViewById(R.id.ignored_done);
        done.setOnClickListener(v -> sheet.hide());
    }

    /**
     * @param onChanged told after each tick, so the form follows along; read the result from {@code choice}
     * @param onClosed  runs after the sheet closes, to hand focus back to where it came from
     */
    void show(IgnoredChoice choice, Runnable onChanged, Runnable onClosed) {
        this.choice = choice;
        this.onChanged = onChanged;
        adapter.show(choice.entries());
        showCount();
        // Done holds the focus until the rows are laid out and the first one can take it
        sheet.show(done, onClosed);
        focusFirstRow();
    }

    private void toggle(String packageName) {
        choice.toggle(packageName);
        adapter.show(choice.entries());
        showCount();
        onChanged.run();
    }

    private void showCount() {
        int ticked = choice.tickedCount();
        count.setText(count.getResources().getQuantityString(R.plurals.ignored_count, ticked, ticked));
    }

    private void focusFirstRow() {
        // The list is reused, so it would open wherever it was last left; it always starts at the top
        list.scrollToPosition(0);
        int position = adapter.firstFocusPosition();
        if (position < 0) return;
        // The row exists only once the list has laid out after the scroll
        list.post(() -> {
            RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(position);
            if (holder != null) holder.itemView.requestFocus();
        });
    }
}
