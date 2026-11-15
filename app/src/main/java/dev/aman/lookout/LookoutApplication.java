package dev.aman.lookout;

import android.app.Application;

import dev.aman.lookout.delivery.DeliveryLog;
import dev.aman.lookout.delivery.DeliveryLogFile;

import java.io.File;

/** Holds what the service and the settings screen share; they run in one process. */
public class LookoutApplication extends Application {
    private DeliveryLog deliveryLog;

    @Override
    public void onCreate() {
        super.onCreate();
        // Ten small records, read once per process start
        deliveryLog = new DeliveryLog(DeliveryLog.CAPACITY, new DeliveryLogFile(new File(getFilesDir(), "deliveries.json")));
    }

    public DeliveryLog deliveryLog() {
        return deliveryLog;
    }
}
