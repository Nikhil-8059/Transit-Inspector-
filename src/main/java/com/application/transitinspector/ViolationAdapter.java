package com.application.transitinspector;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ViolationAdapter extends RecyclerView.Adapter<ViolationAdapter.ViewHolder> {
    private List<ViolationRecord> records;

    public ViolationAdapter(List<ViolationRecord> records) {
        this.records = records;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView driverName, vehicleNumber, violationType, timestamp;

        public ViewHolder(View view) {
            super(view);
            driverName = view.findViewById(R.id.tv_driver_name);
            vehicleNumber = view.findViewById(R.id.tv_vehicle_number);
            violationType = view.findViewById(R.id.tv_violation_type);
            timestamp = view.findViewById(R.id.tv_timestamp);
        }
    }

    @Override
    public ViolationAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.violation_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ViolationRecord v = records.get(position);
        holder.driverName.setText("Driver: " + v.driverName);
        holder.vehicleNumber.setText("Vehicle: " + v.vehicleNumber);
        holder.violationType.setText("Type: " + v.violationType);
        holder.timestamp.setText("Date: " + v.timestamp);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }
}
