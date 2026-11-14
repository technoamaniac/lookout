package dev.aman.lookout.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import dev.aman.lookout.R;
import dev.aman.lookout.delivery.DeliveryRecord;
import dev.aman.lookout.delivery.DeliveryResult;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/** Renders delivery records as rows in the Deliveries tab. */
final class DeliveriesAdapter extends ListAdapter<DeliveryRecord, DeliveriesAdapter.Row> {
    // Records are immutable, so the same instance is the same row
    private static final DiffUtil.ItemCallback<DeliveryRecord> SAME_RECORD = new DiffUtil.ItemCallback<DeliveryRecord>() {
        @Override
        public boolean areItemsTheSame(@NonNull DeliveryRecord a, @NonNull DeliveryRecord b) {
            return a == b;
        }

        @Override
        public boolean areContentsTheSame(@NonNull DeliveryRecord a, @NonNull DeliveryRecord b) {
            return true;
        }
    };

    private final DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM);

    DeliveriesAdapter() {
        super(SAME_RECORD);
    }

    void show(List<DeliveryRecord> records) {
        submitList(records);
    }

    @NonNull
    @Override
    public Row onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Row(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_delivery, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Row row, int position) {
        DeliveryRecord record = getItem(position);
        row.pkg.setText(record.event().packageName());
        row.time.setText(timeFormat.format(new Date(record.attemptedAtMillis())));
        row.result.setText(labelFor(record.result()));
        row.result.setTextColor(row.itemView.getContext().getColor(colorFor(record.result())));
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
