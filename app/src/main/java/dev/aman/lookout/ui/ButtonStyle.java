package dev.aman.lookout.ui;

import android.content.Context;

import com.google.android.material.button.MaterialButton;

import dev.aman.lookout.R;

/** Switches a button between the filled primary look and the outlined secondary one while the screen is up. */
final class ButtonStyle {
    private ButtonStyle() {}

    static void setPrimary(MaterialButton button, boolean primary) {
        Context context = button.getContext();
        button.setBackgroundTintList(context.getColorStateList(primary ? R.color.primary_bg : R.color.secondary_bg));
        button.setStrokeColor(context.getColorStateList(
                primary ? R.color.primary_stroke : R.color.secondary_stroke_state));
        button.setTextColor(context.getColorStateList(primary ? R.color.primary_text : R.color.secondary_text));
        button.setIconTint(context.getColorStateList(primary ? R.color.primary_text : R.color.secondary_text));
    }
}
