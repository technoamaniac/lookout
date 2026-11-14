package dev.aman.lookout.ui;

import android.app.Activity;
import android.view.View;

import java.util.function.IntConsumer;

/**
 * The rail's tabs. Selecting a tab (by click, or by moving the D-pad focus onto it) shows its
 * panel and hides the rest.
 */
public final class PanelTabs {
    private static final long FADE_IN_MS = 180;

    private final Activity activity;
    private final View[] tabs;
    private final int[] panelIds;
    private IntConsumer onSelected = index -> {};
    private int selected = -1;

    /** @param tabIds   one view id per tab
     *  @param panelIds the panel that belongs to each tab, in the same order */
    public PanelTabs(Activity activity, int[] tabIds, int[] panelIds) {
        this.activity = activity;
        this.panelIds = panelIds;
        tabs = new View[tabIds.length];
        for (int i = 0; i < tabIds.length; i++) {
            int index = i;
            tabs[i] = activity.findViewById(tabIds[i]);
            tabs[i].setOnClickListener(v -> select(index));
            // On a TV the panel follows the D-pad, like the system settings: no OK press needed
            tabs[i].setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) select(index);
            });
        }
        select(0);
    }

    /** Runs now with the current tab, and again whenever the selection changes. */
    public void onSelected(IntConsumer listener) {
        onSelected = listener;
        listener.accept(selected);
    }

    /**
     * Moves the D-pad focus onto a tab, which also selects it. Use this rather than
     * {@link #select} from a view that hides when the tab changes: a focused view that hides
     * hands focus to the first tab, which would switch the panel straight back.
     */
    public void focus(int index) {
        if (!tabs[index].requestFocus()) select(index);
    }

    public void select(int index) {
        if (index == selected) return;
        selected = index;
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setSelected(i == index);
            View panel = activity.findViewById(panelIds[i]);
            if (i != index) {
                panel.setVisibility(View.GONE);
            } else if (panel.getVisibility() != View.VISIBLE) {
                panel.setAlpha(0f);
                panel.setVisibility(View.VISIBLE);
                panel.animate().alpha(1f).setDuration(FADE_IN_MS).start();
            }
        }
        onSelected.accept(index);
    }
}
