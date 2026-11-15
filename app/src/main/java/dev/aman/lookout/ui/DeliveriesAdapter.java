package dev.aman.lookout.ui;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import dev.aman.lookout.R;
import dev.aman.lookout.delivery.DeliveryRecord;
import dev.aman.lookout.delivery.DeliveryResult;

/** Renders delivery records as rows in the Deliveries tab. */
final class DeliveriesAdapter extends ListAdapter<DeliveryRecord, DeliveriesAdapter.Row> {
    /** Told which record was chosen, and the row it was chosen from. */
    interface OnRecordSelected {
        void onSelected(DeliveryRecord record, View row);
    }

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
    private final OnRecordSelected onSelected;

    DeliveriesAdapter(OnRecordSelected onSelected) {
        super(SAME_RECORD);
        this.onSelected = onSelected;
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
        int color = row.itemView.getContext().getColor(DeliveryAppearance.colorFor(record.result()));
        row.icon.setImageResource(DeliveryAppearance.iconFor(record));
        row.icon.setImageTintList(ColorStateList.valueOf(color));
        row.pkg.setText(record.message().summary());
        row.time.setText(timeFormat.format(new Date(record.attemptedAtMillis())));
        row.result.setText(DeliveryAppearance.labelFor(record));
        row.result.setTextColor(color);
        row.itemView.setOnClickListener(v -> onSelected.onSelected(record, v));
    }

    static final class Row extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView pkg;
        final TextView time;
        final TextView result;

        Row(View view) {
            super(view);
            icon = view.findViewById(R.id.delivery_icon);
            pkg = view.findViewById(R.id.delivery_package);
            time = view.findViewById(R.id.delivery_time);
            result = view.findViewById(R.id.delivery_result);
        }
    }
}
