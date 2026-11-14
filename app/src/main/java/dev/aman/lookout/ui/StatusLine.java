package dev.aman.lookout.ui;

import android.content.Context;
import android.widget.TextView;

import dev.aman.lookout.R;

/** A one-line message area that colours its text by how the news is. */
final class StatusLine {
    private final TextView view;

    StatusLine(TextView view) {
        this.view = view;
    }

    void info(int messageRes) {
        show(context().getString(messageRes), R.color.accent);
    }

    void success(int messageRes) {
        success(context().getString(messageRes));
    }

    void success(String message) {
        show(message, R.color.ok);
    }

    void error(int messageRes) {
        error(context().getString(messageRes));
    }

    void error(String message) {
        show(message, R.color.warn);
    }

    void clear() {
        view.setText("");
    }

    private void show(String message, int colorRes) {
        view.setText(message);
        view.setTextColor(context().getColor(colorRes));
    }

    private Context context() {
        return view.getContext();
    }
}
