package dev.aman.lookout.ui;

import android.app.Activity;
import android.widget.LinearLayout;

import com.google.android.material.checkbox.MaterialCheckBox;

import dev.aman.lookout.R;
import dev.aman.lookout.delivery.DeliveryFilter;
import dev.aman.lookout.event.EventCategory;

/** The sheet over the Deliveries tab where each category of event is shown or hidden. */
final class DeliveryFilterSheet {
    private final Sheet sheet;
    private final LinearLayout options;
    private final Runnable onChanged;
    private DeliveryFilter filter = DeliveryFilter.DEFAULT;

    /** @param onChanged runs each time a box is ticked or cleared; read the new filter with {@link #filter()} */
    DeliveryFilterSheet(Activity activity, Runnable onChanged) {
        this.onChanged = onChanged;
        sheet = new Sheet(activity, R.id.filter_overlay);
        activity.findViewById(R.id.filter_done).setOnClickListener(v -> hide());
        options = activity.findViewById(R.id.filter_options);
        for (EventCategory category : EventCategory.values()) options.addView(optionFor(activity, category));
    }

    DeliveryFilter filter() {
        return filter;
    }

    /** @param onClosed runs after the sheet closes, to hand focus back to where it came from */
    void show(Runnable onClosed) {
        // The options are what the sheet is for, so focus starts on the first; Done is a Down away
        sheet.show(options.getChildAt(0), onClosed);
    }

    void hide() {
        sheet.hide();
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
