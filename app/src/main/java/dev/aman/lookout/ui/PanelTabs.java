package dev.aman.lookout.ui;

import android.app.Activity;
import android.view.View;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The rail's tabs. Selecting a tab (by click, or by moving the D-pad focus onto it) shows its
 * panel and hides the rest.
 */
public final class PanelTabs {
    private static final long FADE_IN_MS = 180;

    private final Map<Tab, View> tabs = new EnumMap<>(Tab.class);
    private final Map<Tab, View> panels = new EnumMap<>(Tab.class);
    private Consumer<Tab> onSelected = tab -> {};
    private Tab selected;

    public PanelTabs(Activity activity) {
        for (Tab tab : Tab.values()) {
            View view = activity.findViewById(tab.tabId);
            view.setOnClickListener(v -> select(tab));
            // On a TV the panel follows the D-pad, like the system settings: no OK press needed
            view.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) select(tab);
            });
            tabs.put(tab, view);
            panels.put(tab, activity.findViewById(tab.panelId));
        }
        select(Tab.values()[0]);
    }

    public Tab selected() {
        return selected;
    }

    /** Runs now with the current tab, and again whenever the selection changes. */
    public void onSelected(Consumer<Tab> listener) {
        onSelected = listener;
        listener.accept(selected);
    }

    /**
     * Moves the D-pad focus onto a tab, which also selects it. Use this rather than
     * {@link #select} from a view that hides when the tab changes: a focused view that hides
     * hands focus to the first tab, which would switch the panel straight back.
     */
    public void focus(Tab tab) {
        if (!tabs.get(tab).requestFocus()) select(tab);
    }

    public void select(Tab tab) {
        if (tab == selected) return;
        selected = tab;
        for (Tab each : Tab.values()) {
            tabs.get(each).setSelected(each == tab);
            View panel = panels.get(each);
            if (each != tab) {
                panel.setVisibility(View.GONE);
            } else if (panel.getVisibility() != View.VISIBLE) {
                panel.setAlpha(0f);
                panel.setVisibility(View.VISIBLE);
                panel.animate().alpha(1f).setDuration(FADE_IN_MS).start();
            }
        }
        onSelected.accept(tab);
    }
}
