package com.example.edubridge.parent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;

import java.util.ArrayList;
import java.util.Locale;

public class AttendanceRecordsAdapter extends RecyclerView.Adapter<AttendanceRecordsAdapter.VH> {

    private final ArrayList<AttendanceRowItem> items;

    public AttendanceRecordsAdapter(ArrayList<AttendanceRowItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AttendanceRowItem it = items.get(position);

        h.tvDate.setText(it.date);
        h.tvSubject.setText(it.subject);
        h.tvStatus.setText(it.status);

        String s = it.status == null ? "" : it.status.trim().toLowerCase(Locale.US);

        if (s.equals("present")) {
            h.tvStatus.setTextColor(ContextCompat.getColor(h.itemView.getContext(), android.R.color.holo_green_dark));
        } else if (s.equals("absent")) {
            h.tvStatus.setTextColor(ContextCompat.getColor(h.itemView.getContext(), android.R.color.holo_red_dark));
        } else if (s.equals("late")) {
            h.tvStatus.setTextColor(ContextCompat.getColor(h.itemView.getContext(), android.R.color.holo_orange_dark));
        } else {
            h.tvStatus.setTextColor(ContextCompat.getColor(h.itemView.getContext(), android.R.color.black));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate, tvSubject, tvStatus;

        VH(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}