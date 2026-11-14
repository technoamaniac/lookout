package dev.aman.lookout.ui;

import android.app.Activity;
import android.widget.TextView;

import dev.aman.lookout.R;

/** The title and one-line subtitle above the content sheet, following the selected tab. */
public final class PanelHeader {
    private static final int[] TITLES = {R.string.tab_settings, R.string.tab_deliveries, R.string.tab_updates};
    private static final int[] SUBTITLES = {
            R.string.subtitle_settings, R.string.subtitle_deliveries, R.string.subtitle_updates};

    private final TextView title;
    private final TextView subtitle;

    public PanelHeader(Activity activity) {
        title = activity.findViewById(R.id.panel_title);
        subtitle = activity.findViewById(R.id.panel_subtitle);
    }

    /** @param tab the selected tab, in the order of the rail */
    public void show(int tab) {
        title.setText(TITLES[tab]);
        subtitle.setText(SUBTITLES[tab]);
    }
}
