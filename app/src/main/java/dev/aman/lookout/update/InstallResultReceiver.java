package dev.aman.lookout.update;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.widget.Toast;

import dev.aman.lookout.R;

/** Receives the installer's verdict: shows its confirmation screen, or reports a failure. */
public final class InstallResultReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm = confirmationIntent(intent);
            if (confirm != null) context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (status != PackageInstaller.STATUS_SUCCESS) {
            // On success the new version replaces this process, so there is nothing to show
            String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            Toast.makeText(context, context.getString(R.string.update_failed, message), Toast.LENGTH_LONG).show();
        }
    }

    private static Intent confirmationIntent(Intent result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return result.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
        }
        @SuppressWarnings("deprecation")
        Intent confirm = result.getParcelableExtra(Intent.EXTRA_INTENT);
        return confirm;
    }
}
