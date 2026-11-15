package dev.aman.lookout.ui;

import android.view.View;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import dev.aman.lookout.R;
import dev.aman.lookout.delivery.DeliveryFilter;
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
    private final DeliveryFilterSheet filterSheet;
    private final Button filterButton;
    private final OnBackPressedCallback closeSheetOnBack;

    public DeliveriesPanel(AppCompatActivity activity, DeliveryLog log) {
        this.activity = activity;
        this.log = log;
        // The log notifies from the service's worker thread
        this.onLogged = () -> activity.runOnUiThread(this::refresh);
        detail = new DeliveryDetailSheet(activity);
        emptyMessage = activity.findViewById(R.id.deliveries_empty);
        filterSheet = new DeliveryFilterSheet(activity, this::refresh);
        filterButton = activity.findViewById(R.id.filter_button);
        filterButton.setOnClickListener(v -> showFilter());
        list = activity.findViewById(R.id.deliveries);
        list.setAdapter(adapter);
        // Back closes the open sheet; otherwise the callback is off and Back behaves as usual
        closeSheetOnBack = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                detail.hide();
                filterSheet.hide();
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, closeSheetOnBack);
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
        closeSheetOnBack.setEnabled(true);
        detail.show(record, () -> {
            closeSheetOnBack.setEnabled(false);
            // The row may have been recycled while the sheet was open
            if (!row.isAttachedToWindow() || !row.requestFocus()) list.requestFocus();
        });
    }

    /** The filter button sits in the page header, outside the panel, so it follows the selected tab itself. */
    public void setSelected(boolean selected) {
        filterButton.setVisibility(selected ? View.VISIBLE : View.GONE);
    }

    private void showFilter() {
        closeSheetOnBack.setEnabled(true);
        filterSheet.show(() -> {
            closeSheetOnBack.setEnabled(false);
            filterButton.requestFocus();
        });
    }

    private void refresh() {
        List<DeliveryRecord> recent = filterSheet.filter().apply(log.recent());
        adapter.show(recent);
        emptyMessage.setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
