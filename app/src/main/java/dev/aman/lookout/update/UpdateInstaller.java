package dev.aman.lookout.update;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Hands a downloaded APK to Android's package installer, and asks the new version to reopen Lookout. */
public final class UpdateInstaller {
    public void install(Context context, File apk) throws IOException {
        RelaunchAfterUpdate relaunch = new RelaunchAfterUpdate(new PrefsUpdateChannel(context));
        PackageInstaller installer = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params =
                new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        // Android rejects the session if the APK is for any other package
        params.setAppPackageName(context.getPackageName());
        // Needs UPDATE_PACKAGES_WITHOUT_USER_ACTION; Android may still ask, and then the result
        // receiver shows its confirmation screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
        }
        int sessionId = installer.createSession(params);
        try (PackageInstaller.Session session = installer.openSession(sessionId)) {
            try (InputStream in = new FileInputStream(apk);
                 OutputStream out = session.openWrite("lookout.apk", 0, apk.length())) {
                byte[] buffer = new byte[16 * 1024];
                for (int n; (n = in.read(buffer)) > 0; ) out.write(buffer, 0, n);
                session.fsync(out);
            }
            // The system fills in the result extras, so the intent must be mutable
            PendingIntent result = PendingIntent.getBroadcast(context, sessionId,
                    new Intent(context, InstallResultReceiver.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            // Before the commit, which may end this process at once
            relaunch.request(System.currentTimeMillis());
            session.commit(result.getIntentSender());
        } catch (IOException | RuntimeException e) {
            relaunch.cancel();
            installer.abandonSession(sessionId);
            throw e;
        }
    }
}
