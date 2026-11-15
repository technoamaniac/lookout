package dev.aman.lookout.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.material.checkbox.MaterialCheckBox;

import dev.aman.lookout.R;
import dev.aman.lookout.delivery.DeliveryFilter;
import dev.aman.lookout.event.EventCategory;

/** The sheet over the Deliveries tab where each category of event is shown or hidden. */
final class DeliveryFilterSheet {
    private final View overlay;
    private final ViewGroup mainContent;
    private final View doneButton;
    private final Runnable onChanged;
    private DeliveryFilter filter = DeliveryFilter.DEFAULT;
    private Runnable onClosed = () -> {};

    /** @param onChanged runs each time a box is ticked or cleared; read the new filter with {@link #filter()} */
    DeliveryFilterSheet(Activity activity, Runnable onChanged) {
        this.onChanged = onChanged;
        overlay = activity.findViewById(R.id.filter_overlay);
        mainContent = activity.findViewById(R.id.main_content);
        doneButton = activity.findViewById(R.id.filter_done);
        doneButton.setOnClickListener(v -> hide());
        LinearLayout options = activity.findViewById(R.id.filter_options);
        for (EventCategory category : EventCategory.values()) options.addView(optionFor(activity, category));
    }

    DeliveryFilter filter() {
        return filter;
    }

    boolean isShowing() {
        return overlay.getVisibility() == View.VISIBLE;
    }

    /** @param onClosed runs after the sheet closes, to hand focus back to where it came from */
    void show(Runnable onClosed) {
        this.onClosed = onClosed;
        // The screen behind must not take D-pad focus while the sheet is open
        mainContent.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        overlay.setVisibility(View.VISIBLE);
        doneButton.requestFocus();
    }

    void hide() {
        if (!isShowing()) return;
        overlay.setVisibility(View.GONE);
        mainContent.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        onClosed.run();
    }

    private MaterialCheckBox optionFor(Activity activity, EventCategory category) {
        MaterialCheckBox box = new MaterialCheckBox(activity);
        box.setText(DeliveryAppearance.labelFor(category));
        box.setTextAppearance(R.style.Text_Body);
        box.setChecked(filter.shows(category));
        box.setOnCheckedChangeListener((view, checked) -> {
            filter = filter.with(category, checked);
            onChanged.run();
        });
        return box;
    }
}
