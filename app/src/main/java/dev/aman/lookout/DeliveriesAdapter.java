package dev.aman.lookout;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Renders delivery records as rows in the Deliveries tab. */
final class DeliveriesAdapter extends RecyclerView.Adapter<DeliveriesAdapter.Row> {
    private final DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);
    private List<DeliveryRecord> records = Collections.emptyList();

    void show(List<DeliveryRecord> newRecords) {
        records = newRecords;
        notifyDataSetChanged(); // The list is capped at a few dozen rows
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Row(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_delivery, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Row row, int position) {
        DeliveryRecord record = records.get(position);
        row.pkg.setText(record.event().packageName());
        row.time.setText(timeFormat.format(new Date(record.attemptedAtMillis())));
        row.result.setText(labelFor(record.result()));
        row.result.setTextColor(row.itemView.getContext().getColor(colorFor(record.result())));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    private static int labelFor(DeliveryResult result) {
        switch (result) {
            case DELIVERED:
                return R.string.delivery_delivered;
            case PERMANENT_FAILURE:
                return R.string.delivery_rejected;
            default:
                return R.string.delivery_failed;
        }
    }

    private static int colorFor(DeliveryResult result) {
        switch (result) {
            case DELIVERED:
                return R.color.ok;
            case PERMANENT_FAILURE:
                return R.color.error;
            default:
                return R.color.warn;
        }
    }

    static final class Row extends RecyclerView.ViewHolder {
        final TextView pkg;
        final TextView time;
        final TextView result;

        Row(View view) {
            super(view);
            pkg = view.findViewById(R.id.delivery_package);
            time = view.findViewById(R.id.delivery_time);
            result = view.findViewById(R.id.delivery_result);
        }
    }
}
