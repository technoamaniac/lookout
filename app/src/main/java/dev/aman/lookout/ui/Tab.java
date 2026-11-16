package dev.aman.lookout.ui;

import dev.aman.lookout.R;

/** The screen's tabs, in the order of the rail, with the views and text that belong to each. */
public enum Tab {
    SETTINGS(R.id.tab_settings, R.id.settings_panel, R.string.tab_settings, R.string.subtitle_settings),
    DELIVERIES(R.id.tab_deliveries, R.id.deliveries_panel, R.string.tab_deliveries, R.string.subtitle_deliveries),
    UPDATES(R.id.tab_updates, R.id.updates_panel, R.string.tab_updates, R.string.subtitle_updates);

    final int tabId;
    final int panelId;
    final int title;
    final int subtitle;

    Tab(int tabId, int panelId, int title, int subtitle) {
        this.tabId = tabId;
        this.panelId = panelId;
        this.title = title;
        this.subtitle = subtitle;
    }
}
