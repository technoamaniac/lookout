package dev.aman.lookout;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import dev.aman.lookout.setup.FirstRun;
import dev.aman.lookout.setup.SetupStore;

/** Opens the setup screens: by themselves on a first run, and from the Permissions button on the Settings tab. */
final class SetupLauncher {
    private final Activity activity;
    private final SetupStore store;

    SetupLauncher(Activity activity, SetupStore store) {
        this.activity = activity;
        this.store = store;
    }

    /** Makes the button open the permissions screen alone, without the welcome. */
    void openFrom(View button) {
        button.setOnClickListener(v -> activity.startActivity(
                new Intent(activity, SetupActivity.class).putExtra(SetupActivity.EXTRA_PERMISSIONS_ONLY, true)));
    }

    /** @param webhookGiven whether a webhook is already saved or on its way, so the user is past setup */
    void openIfFirstRun(boolean webhookGiven) {
        if (FirstRun.isDue(store.isSeen(), webhookGiven)) {
            activity.startActivity(new Intent(activity, SetupActivity.class));
        }
    }
}
