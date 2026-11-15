package dev.aman.lookout.ui;

import android.app.Activity;
import android.widget.RadioGroup;

import dev.aman.lookout.R;

/** The heartbeat row on the Settings tab: one radio button per interval, read and set in minutes. */
final class HeartbeatChoice {
    // Button ids in the row's order, and the minutes each stands for; 0 is off
    private static final int[] BUTTONS = {R.id.heartbeat_off, R.id.heartbeat_1, R.id.heartbeat_5, R.id.heartbeat_15};
    private static final int[] MINUTES = {0, 1, 5, 15};

    private final RadioGroup group;
    // Shown with no button ticked, and kept, so opening the form never changes a saved interval
    private int unofferedMinutes;

    HeartbeatChoice(Activity activity) {
        group = activity.findViewById(R.id.heartbeat);
    }

    /** Ticks the button for {@code minutes}; for an interval the row does not offer, ticks none. */
    void show(int minutes) {
        for (int i = 0; i < MINUTES.length; i++) {
            if (MINUTES[i] == minutes) {
                group.check(BUTTONS[i]);
                return;
            }
        }
        unofferedMinutes = minutes;
        group.clearCheck();
    }

    int minutes() {
        int checked = group.getCheckedRadioButtonId();
        for (int i = 0; i < BUTTONS.length; i++) {
            if (BUTTONS[i] == checked) return MINUTES[i];
        }
        return unofferedMinutes;
    }

    void setEnabled(boolean enabled) {
        for (int i = 0; i < group.getChildCount(); i++) group.getChildAt(i).setEnabled(enabled);
    }

    void onChanged(Runnable listener) {
        group.setOnCheckedChangeListener((g, id) -> listener.run());
    }
}
