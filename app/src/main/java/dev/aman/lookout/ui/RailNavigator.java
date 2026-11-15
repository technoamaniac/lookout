package dev.aman.lookout.ui;

import android.app.Activity;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import dev.aman.lookout.R;
import dev.aman.lookout.navigation.ExitConfirmation;
import dev.aman.lookout.navigation.RailFocus;

/** Applies {@link RailFocus} to the screen: where Left, Up and Down go between a page and the side rail. */
public final class RailNavigator {
    private static final long EXIT_WINDOW_MILLIS = 2_000;

    private final Activity activity;
    private final PanelTabs tabs;
    private final View rail;
    private final View screen;
    private final ScrollView page;
    private final ExitConfirmation exit = new ExitConfirmation(EXIT_WINDOW_MILLIS);

    public RailNavigator(Activity activity, PanelTabs tabs) {
        this.activity = activity;
        this.tabs = tabs;
        rail = activity.findViewById(R.id.rail);
        screen = activity.findViewById(R.id.main_content);
        page = activity.findViewById(R.id.page);
    }

    /** @return whether the key was handled here, and so must not reach the focused view */
    public boolean onKey(KeyEvent event) {
        RailFocus.Key key = keyOf(event.getKeyCode());
        View focused = activity.getCurrentFocus();
        if (key == null || focused == null || event.getAction() != KeyEvent.ACTION_DOWN || !event.hasNoModifiers()) {
            return false;
        }
        if (isVertical(key) && areaOf(focused) == RailFocus.Area.PAGE) return moveWithinPage(focused, directionOf(key));
        View next = focused.focusSearch(directionOf(key));
        switch (RailFocus.onKey(key, areaOf(focused), areaOf(next), cursorCanMove(focused))) {
            case SELECTED_TAB:
                tabs.focus(tabs.selected());
                return true;
            default:
                return false;
        }
    }

    /**
     * Up and Down stay on the page: Android's own search from a page field can pick a side menu item over the field
     * right beside it, so the page is searched alone, from the whole field rather than from its text cursor.
     * With nothing further, the page scrolls instead.
     */
    private boolean moveWithinPage(View focused, int direction) {
        Rect from = new Rect(0, 0, focused.getWidth(), focused.getHeight());
        page.offsetDescendantRectToMyCoords(focused, from);
        View next = FocusFinder.getInstance().findNextFocusFromRect(page, from, direction);
        if (next == null || !next.requestFocus(direction)) page.arrowScroll(direction);
        return true;
    }

    /**
     * Back from a page returns to the rail's selected tab; Back on the rail asks for a second press before leaving.
     *
     * @return whether to leave the screen now
     */
    public boolean leaveOnBack() {
        if (areaOf(activity.getCurrentFocus()) != RailFocus.Area.RAIL) {
            tabs.focus(tabs.selected());
            return false;
        }
        if (exit.confirmed(SystemClock.elapsedRealtime())) return true;
        Toast.makeText(activity, R.string.press_back_again, Toast.LENGTH_SHORT).show();
        return false;
    }

    private RailFocus.Area areaOf(View view) {
        if (view == null) return RailFocus.Area.OTHER;
        if (isWithin(view, rail)) return RailFocus.Area.RAIL;
        return isWithin(view, screen) ? RailFocus.Area.PAGE : RailFocus.Area.OTHER;
    }

    private static boolean isWithin(View view, View ancestor) {
        for (View v = view; v != null; v = v.getParent() instanceof View ? (View) v.getParent() : null) {
            if (v == ancestor) return true;
        }
        return false;
    }

    // Left in a text field moves its cursor until it is back at the start, with no text selected
    private static boolean cursorCanMove(View view) {
        if (!(view instanceof EditText)) return false;
        EditText field = (EditText) view;
        return field.getSelectionStart() > 0 || field.getSelectionStart() != field.getSelectionEnd();
    }

    private static boolean isVertical(RailFocus.Key key) {
        return key == RailFocus.Key.UP || key == RailFocus.Key.DOWN;
    }

    private static RailFocus.Key keyOf(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                return RailFocus.Key.UP;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return RailFocus.Key.DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                return RailFocus.Key.LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                return RailFocus.Key.RIGHT;
            default:
                return null;
        }
    }

    private static int directionOf(RailFocus.Key key) {
        switch (key) {
            case UP:
                return View.FOCUS_UP;
            case DOWN:
                return View.FOCUS_DOWN;
            case LEFT:
                return View.FOCUS_LEFT;
            default:
                return View.FOCUS_RIGHT;
        }
    }
}
