package dev.aman.lookout.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import dev.aman.lookout.R;

/** An overlay over the screen that holds the D-pad while open and hands focus back when it closes. */
final class Sheet {
    private final View overlay;
    private final ViewGroup mainContent;
    private Runnable onClosed = () -> {};

    /** @param overlayId the overlay's root view, hidden until {@link #show} */
    Sheet(Activity activity, int overlayId) {
        overlay = activity.findViewById(overlayId);
        mainContent = activity.findViewById(R.id.main_content);
    }

    /**
     * @param firstFocus the view inside the sheet that takes the focus
     * @param onClosed   runs after the sheet closes, to hand focus back to where it came from
     */
    void show(View firstFocus, Runnable onClosed) {
        this.onClosed = onClosed;
        // The screen behind must not take D-pad focus while the sheet is open
        mainContent.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        overlay.setVisibility(View.VISIBLE);
        firstFocus.requestFocus();
    }

    /** Closes the sheet; does nothing when it is not open. Returns whether it was. */
    boolean hide() {
        if (!isShowing()) return false;
        overlay.setVisibility(View.GONE);
        mainContent.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        onClosed.run();
        return true;
    }

    boolean isShowing() {
        return overlay.getVisibility() == View.VISIBLE;
    }
}
