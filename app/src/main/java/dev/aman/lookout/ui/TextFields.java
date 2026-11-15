package dev.aman.lookout.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/** Small helpers for the form's text fields. */
final class TextFields {
    private TextFields() {}

    // Park the cursor at the start so a long value shows its beginning, not its tail
    static void show(EditText field, String text) {
        field.setText(text);
        field.setSelection(0);
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
