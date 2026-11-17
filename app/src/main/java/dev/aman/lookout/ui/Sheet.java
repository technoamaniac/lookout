package dev.aman.lookout.ui;

import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import dev.aman.lookout.R;

/**
 * An overlay over the screen that holds the D-pad while open, closes on Back and hands focus back when it closes.
 * Back reaches the sheet before any callback the activity added earlier, so add the activity's own first.
 */
final class Sheet {
    private final View overlay;
    private final ViewGroup mainContent;
    // Off while the sheet is closed, so Back behaves as usual
    private final OnBackPressedCallback closeOnBack = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            hide();
        }
    };
    private Runnable onClosed = () -> {};

    /** @param overlayId the overlay's root view, hidden until {@link #show} */
    Sheet(AppCompatActivity activity, int overlayId) {
        overlay = activity.findViewById(overlayId);
        mainContent = activity.findViewById(R.id.main_content);
        activity.getOnBackPressedDispatcher().addCallback(activity, closeOnBack);
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
        closeOnBack.setEnabled(true);
        firstFocus.requestFocus();
    }

    /** Closes the sheet; does nothing when it is not open. */
    void hide() {
        if (!isShowing()) return;
        closeOnBack.setEnabled(false);
        overlay.setVisibility(View.GONE);
        mainContent.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        onClosed.run();
    }

    boolean isShowing() {
        return overlay.getVisibility() == View.VISIBLE;
    }
}
