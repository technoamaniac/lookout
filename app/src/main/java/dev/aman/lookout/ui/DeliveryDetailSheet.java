package dev.aman.lookout.ui;

import android.app.Activity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.aman.lookout.R;
import dev.aman.lookout.delivery.DeliveryRecord;

/** The sheet over the Deliveries tab that shows one attempt and the exact payload it sent. */
final class DeliveryDetailSheet {
    private static final Pattern JSON_KEY = Pattern.compile("\"[^\"]+\"(?=:)");
    private static final Pattern JSON_NUMBER = Pattern.compile("(?<=: )\\d+");

    private final Activity activity;
    private final View overlay;
    private final ViewGroup mainContent;
    private final StatusLine result;
    private final TextView app;
    private final TextView time;
    private final TextView response;
    private final ScrollView payloadScroll;
    private final TextView payload;
    private final View closeButton;
    private final DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
    private Runnable onClosed = () -> {};

    DeliveryDetailSheet(Activity activity) {
        this.activity = activity;
        overlay = activity.findViewById(R.id.detail_overlay);
        mainContent = activity.findViewById(R.id.main_content);
        result = new StatusLine(activity.findViewById(R.id.detail_result));
        app = activity.findViewById(R.id.detail_app);
        time = activity.findViewById(R.id.detail_time);
        response = activity.findViewById(R.id.detail_response);
        payloadScroll = activity.findViewById(R.id.detail_payload_scroll);
        payload = activity.findViewById(R.id.detail_payload);
        closeButton = activity.findViewById(R.id.detail_close);
        closeButton.setOnClickListener(v -> hide());
    }

    boolean isShowing() {
        return overlay.getVisibility() == View.VISIBLE;
    }

    /** @param onClosed runs after the sheet closes, to hand focus back to where it came from */
    void show(DeliveryRecord record, Runnable onClosed) {
        this.onClosed = onClosed;
        result.show(activity.getString(DeliveryAppearance.labelFor(record.result())),
                DeliveryAppearance.kindFor(record.result()));
        app.setText(record.message().summary());
        time.setText(dateTimeFormat.format(new Date(record.attemptedAtMillis())));
        response.setText(DeliveryAppearance.responseFor(record.result()));
        payload.setText(highlight(record.message().toPrettyJson()));
        // The sheet is reused, so start each payload at its top rather than where the last one was left
        payloadScroll.scrollTo(0, 0);
        // The screen behind must not take D-pad focus while the sheet is open
        mainContent.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        overlay.setVisibility(View.VISIBLE);
        closeButton.requestFocus();
    }

    void hide() {
        if (!isShowing()) return;
        overlay.setVisibility(View.GONE);
        mainContent.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        onClosed.run();
    }

    // Keys in the accent colour and numbers in the warm one, matching the design tokens
    private Spannable highlight(String json) {
        SpannableString text = new SpannableString(json);
        colour(text, JSON_KEY, R.color.accent);
        colour(text, JSON_NUMBER, R.color.on_warning_container);
        return text;
    }

    private void colour(SpannableString text, Pattern pattern, int colorRes) {
        int color = activity.getColor(colorRes);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            text.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
