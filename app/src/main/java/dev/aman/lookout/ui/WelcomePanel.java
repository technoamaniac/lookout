package dev.aman.lookout.ui;

import android.app.Activity;
import android.view.View;

import dev.aman.lookout.R;

/** Setup step 1: what Lookout does, with a way to start or skip. */
public final class WelcomePanel {
    private final View panel;
    private final View getStarted;

    public WelcomePanel(Activity activity, Runnable onGetStarted, Runnable onSkip) {
        panel = activity.findViewById(R.id.welcome_panel);
        getStarted = activity.findViewById(R.id.get_started);
        getStarted.setOnClickListener(v -> onGetStarted.run());
        activity.findViewById(R.id.skip_setup).setOnClickListener(v -> onSkip.run());
    }

    public void show() {
        panel.setVisibility(View.VISIBLE);
        getStarted.requestFocus();
    }

    public boolean isShowing() {
        return panel.getVisibility() == View.VISIBLE;
    }

    public void hide() {
        panel.setVisibility(View.GONE);
    }
}
