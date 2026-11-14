package dev.aman.lookout.ui;

import android.app.Activity;
import android.view.View;

import com.google.android.material.tabs.TabLayout;

/** Shows the panel that belongs to the selected tab and hides the rest. */
public final class PanelTabs {
    private final Activity activity;
    private final TabLayout tabs;
    private final int[] panelIds;

    /** @param panelIds one view id per tab, in tab order */
    public PanelTabs(Activity activity, int tabLayoutId, int... panelIds) {
        this.activity = activity;
        this.tabs = activity.findViewById(tabLayoutId);
        this.panelIds = panelIds;
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showPanel(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    public void select(int index) {
        TabLayout.Tab tab = tabs.getTabAt(index);
        if (tab != null) tab.select();
    }

    private void showPanel(int index) {
        for (int i = 0; i < panelIds.length; i++) {
            activity.findViewById(panelIds[i]).setVisibility(i == index ? View.VISIBLE : View.GONE);
        }
    }
}
