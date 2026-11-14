package dev.aman.lookout.ui;

import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import dev.aman.lookout.R;
import dev.aman.lookout.delivery.DeliveryLog;
import dev.aman.lookout.delivery.DeliveryRecord;

/** The Deliveries tab: a live view of the delivery log while the screen is visible. */
public final class DeliveriesPanel {
    private final AppCompatActivity activity;
    private final DeliveryLog log;
    private final DeliveriesAdapter adapter = new DeliveriesAdapter(this::showDetail);
    private final DeliveryDetailSheet detail;
    private final RecyclerView list;
    private final View emptyMessage;
    private final Runnable onLogged;
    private final OnBackPressedCallback closeDetailOnBack;

    public DeliveriesPanel(AppCompatActivity activity, DeliveryLog log) {
        this.activity = activity;
        this.log = log;
        // The log notifies from the service's worker thread
        this.onLogged = () -> activity.runOnUiThread(this::refresh);
        detail = new DeliveryDetailSheet(activity);
        emptyMessage = activity.findViewById(R.id.deliveries_empty);
        list = activity.findViewById(R.id.deliveries);
        list.setAdapter(adapter);
        // Back closes the sheet while it is open; otherwise the callback is off and Back behaves as usual
        closeDetailOnBack = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                detail.hide();
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, closeDetailOnBack);
    }

    /** Starts following the log; pair with {@link #stop()}. */
    public void start() {
        log.addListener(onLogged);
        refresh();
    }

    public void stop() {
        log.removeListener(onLogged);
    }

    private void showDetail(DeliveryRecord record, View row) {
        closeDetailOnBack.setEnabled(true);
        detail.show(record, () -> {
            closeDetailOnBack.setEnabled(false);
            // The row may have been recycled while the sheet was open
            if (!row.isAttachedToWindow() || !row.requestFocus()) list.requestFocus();
        });
    }

    private void refresh() {
        List<DeliveryRecord> recent = log.recent();
        adapter.show(recent);
        emptyMessage.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
