package dev.aman.lookout.ui;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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

    private final AppCompatActivity activity;
    private final Sheet sheet;
    private final StatusLine result;
    private final TextView app;
    private final TextView time;
    private final TextView response;
    private final ScrollView payloadScroll;
    private final TextView payload;
    private final View closeButton;
    private final DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

    DeliveryDetailSheet(AppCompatActivity activity) {
        this.activity = activity;
        sheet = new Sheet(activity, R.id.detail_overlay);
        result = new StatusLine(activity.findViewById(R.id.detail_result));
        app = activity.findViewById(R.id.detail_app);
        time = activity.findViewById(R.id.detail_time);
        response = activity.findViewById(R.id.detail_response);
        payloadScroll = activity.findViewById(R.id.detail_payload_scroll);
        payload = activity.findViewById(R.id.detail_payload);
        closeButton = activity.findViewById(R.id.detail_close);
        closeButton.setOnClickListener(v -> sheet.hide());
    }

    /** @param onClosed runs after the sheet closes, to hand focus back to where it came from */
    void show(DeliveryRecord record, Runnable onClosed) {
        result.show(activity.getString(DeliveryAppearance.labelFor(record)),
                DeliveryAppearance.kindFor(record.result()));
        app.setText(record.message().summary());
        time.setText(dateTimeFormat.format(new Date(record.attemptedAtMillis())));
        response.setText(DeliveryAppearance.responseFor(record));
        payload.setText(highlight(record.message().toPrettyJson()));
        // The sheet is reused, so start each payload at its top rather than where the last one was left
        payloadScroll.scrollTo(0, 0);
        sheet.show(closeButton, onClosed);
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
