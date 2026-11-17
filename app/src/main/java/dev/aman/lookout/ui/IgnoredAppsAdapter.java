package dev.aman.lookout.ui;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.aman.lookout.R;
import dev.aman.lookout.apps.IgnoredChoice;

/** Renders the ignored-apps choice as section headings and tick rows. */
final class IgnoredAppsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    /** Told which package was pressed. */
    interface OnToggled {
        void onToggled(String packageName);
    }

    private static final int HEADER = 0;
    private static final int APP = 1;

    private final PackageManager packages;
    private final OnToggled onToggled;
    private final Map<String, Drawable> icons = new HashMap<>();
    private List<IgnoredChoice.Entry> entries = Collections.emptyList();

    IgnoredAppsAdapter(PackageManager packages, OnToggled onToggled) {
        this.packages = packages;
        this.onToggled = onToggled;
    }

    /** Replaces the rows, redrawing only those that differ: a tick must not reset the list under the D-pad. */
    void show(List<IgnoredChoice.Entry> next) {
        List<IgnoredChoice.Entry> previous = entries;
        DiffUtil.DiffResult changes = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return previous.size();
            }

            @Override
            public int getNewListSize() {
                return next.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPosition, int newPosition) {
                IgnoredChoice.Entry before = previous.get(oldPosition);
                IgnoredChoice.Entry after = next.get(newPosition);
                return before.isHeader() == after.isHeader() && before.section() == after.section()
                        && Objects.equals(before.packageName(), after.packageName());
            }

            @Override
            public boolean areContentsTheSame(int oldPosition, int newPosition) {
                return previous.get(oldPosition).isTicked() == next.get(newPosition).isTicked();
            }
        });
        entries = next;
        changes.dispatchUpdatesTo(this);
    }

    /** The position of the first app, which takes the focus when the list opens; -1 for an empty list. */
    int firstFocusPosition() {
        for (int i = 0; i < entries.size(); i++) {
            if (!entries.get(i).isHeader()) return i;
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    @Override
    public int getItemViewType(int position) {
        return entries.get(position).isHeader() ? HEADER : APP;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == HEADER) {
            return new RecyclerView.ViewHolder(inflater.inflate(R.layout.item_app_header, parent, false)) {};
        }
        return new AppRow(inflater.inflate(R.layout.item_app_choice, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        IgnoredChoice.Entry entry = entries.get(position);
        if (entry.isHeader()) {
            ((TextView) holder.itemView).setText(headingOf(entry.section()));
            return;
        }
        AppRow row = (AppRow) holder;
        row.label.setText(entry.label());
        row.pkg.setText(entry.packageName());
        row.ticked.setChecked(entry.isTicked());
        Drawable icon = iconOf(entry.packageName());
        if (icon != null) {
            row.icon.setImageDrawable(icon);
            row.icon.setImageTintList(null);
        } else {
            row.icon.setImageResource(R.drawable.ic_info);
            row.icon.setImageTintList(row.itemView.getContext().getColorStateList(R.color.on_surface_variant));
        }
        row.itemView.setOnClickListener(v -> onToggled.onToggled(entry.packageName()));
    }

    private Drawable iconOf(String packageName) {
        Drawable icon = icons.get(packageName);
        if (icon == null) {
            try {
                icon = packages.getApplicationIcon(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                // An ignored package that is not installed or not visible: the caller shows a plain mark
                return null;
            }
            icons.put(packageName, icon);
        }
        return icon;
    }

    private static int headingOf(IgnoredChoice.Section section) {
        switch (section) {
            case HOME_SCREENS:
                return R.string.ignored_section_home;
            case APPS:
                return R.string.ignored_section_apps;
            case OTHER:
                return R.string.ignored_section_other;
            case BUILT_IN:
                return R.string.ignored_section_default;
            default:
                throw new IllegalArgumentException("No heading for " + section);
        }
    }

    private static final class AppRow extends RecyclerView.ViewHolder {
        final MaterialCheckBox ticked;
        final ImageView icon;
        final TextView label;
        final TextView pkg;

        AppRow(View view) {
            super(view);
            ticked = view.findViewById(R.id.app_ticked);
            icon = view.findViewById(R.id.app_icon);
            label = view.findViewById(R.id.app_label);
            pkg = view.findViewById(R.id.app_package);
        }
    }
}
