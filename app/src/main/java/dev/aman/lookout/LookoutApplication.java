package dev.aman.lookout;

import android.app.Application;

import dev.aman.lookout.delivery.DeliveryLog;

/** Holds what the service and the settings screen share; they run in one process. */
public class LookoutApplication extends Application {
    private DeliveryLog deliveryLog;

    @Override
    public void onCreate() {
        super.onCreate();
        deliveryLog = new DeliveryLog(DeliveryLog.CAPACITY);
        deleteOldDeliveryFiles();
    }

    // Versions up to 0.6.0 kept deliveries in deliveries.json, and a crash could leave temporary copies beside it
    private void deleteOldDeliveryFiles() {
        for (String name : fileList()) {
            if (name.startsWith("deliveries.json")) deleteFile(name);
        }
    }

    public DeliveryLog deliveryLog() {
        return deliveryLog;
    }
}
