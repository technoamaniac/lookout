package dev.aman.lookout.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

/** Small helpers for the form's text fields. */
final class TextFields {
    private TextFields() {}

    // Park the cursor at the start so a long value shows its beginning, not its tail
    static void show(EditText field, String text) {
        field.setText(text);
        field.setSelection(0);
    }

    /**
     * Makes Up and Down leave a one-line field at once. By default the first press only moves the cursor to the start
     * or end of the text, so stepping through the form takes two presses per field. With a modifier held, such as
     * Shift to select text on a keyboard, the keys keep their usual meaning.
     */
    static void leaveOnUpDown(EditText field) {
        field.setOnKeyListener((view, keyCode, event) -> {
            int direction = directionOf(keyCode);
            if (direction == 0 || event.getAction() != KeyEvent.ACTION_DOWN || !event.hasNoModifiers()) return false;
            View next = view.focusSearch(direction);
            return next != null && next.requestFocus(direction);
        });
    }

    private static int directionOf(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                return View.FOCUS_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return View.FOCUS_DOWN;
            default:
                return 0;
        }
    }

    static void onChanged(EditText field, Runnable callback) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                callback.run();
            }
        });
    }
}
