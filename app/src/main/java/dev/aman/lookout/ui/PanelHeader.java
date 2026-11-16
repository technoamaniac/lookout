package dev.aman.lookout.ui;

import android.app.Activity;
import android.widget.TextView;

import dev.aman.lookout.R;

/** The title and one-line subtitle above the content sheet, following the selected tab. */
public final class PanelHeader {
    private final TextView title;
    private final TextView subtitle;

    public PanelHeader(Activity activity) {
        title = activity.findViewById(R.id.panel_title);
        subtitle = activity.findViewById(R.id.panel_subtitle);
    }

    public void show(Tab tab) {
        title.setText(tab.title);
        subtitle.setText(tab.subtitle);
    }
}
