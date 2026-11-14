package dev.aman.lookout.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.TextViewCompat;

import dev.aman.lookout.R;

/** A one-line status chip that is tinted by how the news is, and hidden while empty. */
final class StatusLine {
    /** The colours and icon of one kind of message. */
    enum Kind {
        INFO(R.color.accent_container, R.color.on_accent_container, R.color.accent, R.drawable.ic_info),
        SUCCESS(R.color.success_container, R.color.on_success_container, R.color.success, R.drawable.ic_status_ok),
        WARNING(R.color.warning_container, R.color.on_warning_container, R.color.warning, R.drawable.ic_alert),
        ERROR(R.color.error_container, R.color.on_error_container, R.color.error, R.drawable.ic_alert);

        private final int container;
        private final int text;
        private final int icon;
        private final int defaultIconRes;

        Kind(int container, int text, int icon, int defaultIconRes) {
            this.container = container;
            this.text = text;
            this.icon = icon;
            this.defaultIconRes = defaultIconRes;
        }
    }

    private static final int ICON_SIZE_DP = 20;

    private final TextView view;

    StatusLine(TextView view) {
        this.view = view;
    }

    void info(int messageRes) {
        show(context().getString(messageRes), Kind.INFO, Kind.INFO.defaultIconRes);
    }

    /** An informational message with its own icon, such as an available update. */
    void info(String message, int iconRes) {
        show(message, Kind.INFO, iconRes);
    }

    void success(int messageRes) {
        success(context().getString(messageRes));
    }

    void success(String message) {
        show(message, Kind.SUCCESS, Kind.SUCCESS.defaultIconRes);
    }

    void warning(int messageRes) {
        show(context().getString(messageRes), Kind.WARNING, Kind.WARNING.defaultIconRes);
    }

    void error(int messageRes) {
        error(context().getString(messageRes));
    }

    void error(String message) {
        show(message, Kind.ERROR, Kind.ERROR.defaultIconRes);
    }

    void show(String message, Kind kind) {
        show(message, kind, kind.defaultIconRes);
    }

    void clear() {
        view.setText("");
        view.setVisibility(View.GONE);
    }

    boolean isShowing() {
        return view.getVisibility() == View.VISIBLE;
    }

    private void show(String message, Kind kind, int iconRes) {
        Context context = context();
        view.setText(message);
        view.setTextColor(context.getColor(kind.text));
        view.setBackgroundTintList(context.getColorStateList(kind.container));
        Drawable icon = AppCompatResources.getDrawable(context, iconRes);
        int size = Math.round(ICON_SIZE_DP * context.getResources().getDisplayMetrics().density);
        icon.mutate().setBounds(0, 0, size, size);
        view.setCompoundDrawablesRelative(icon, null, null, null);
        TextViewCompat.setCompoundDrawableTintList(view, context.getColorStateList(kind.icon));
        view.setVisibility(View.VISIBLE);
    }

    private Context context() {
        return view.getContext();
    }
}
