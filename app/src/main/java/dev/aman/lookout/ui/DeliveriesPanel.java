package dev.aman.lookout.ui;

import android.app.Activity;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import dev.aman.lookout.R;
import dev.aman.lookout.delivery.DeliveryLog;
import dev.aman.lookout.delivery.DeliveryRecord;

import java.util.List;

/** The Deliveries tab: a live view of the delivery log while the screen is visible. */
public final class DeliveriesPanel {
    private final Activity activity;
    private final DeliveryLog log;
    private final DeliveriesAdapter adapter = new DeliveriesAdapter();
    private final View emptyMessage;
    private final Runnable onLogged;

    public DeliveriesPanel(Activity activity, DeliveryLog log) {
        this.activity = activity;
        this.log = log;
        // The log notifies from the service's worker thread
        this.onLogged = () -> activity.runOnUiThread(this::refresh);
        emptyMessage = activity.findViewById(R.id.deliveries_empty);
        activity.<RecyclerView>findViewById(R.id.deliveries).setAdapter(adapter);
    }

    /** Starts following the log; pair with {@link #stop()}. */
    public void start() {
        log.addListener(onLogged);
        refresh();
    }

    public void stop() {
        log.removeListener(onLogged);
    }

    private void refresh() {
        List<DeliveryRecord> recent = log.recent();
        adapter.show(recent);
        emptyMessage.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
