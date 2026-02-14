package com.example.edubridge.parent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.edubridge.R;
import java.util.ArrayList;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.VH> {

    private final ArrayList<ScheduleItem> items;

    public ScheduleAdapter(ArrayList<ScheduleItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ScheduleItem it = items.get(position);

        String line1 = it.day + " • " + it.start + " - " + it.end;
        String line2 = it.subject + (it.room == null || it.room.isEmpty() ? "" : " • " + it.room);

        h.tvLine1.setText(line1);
        h.tvLine2.setText(line2);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvLine1, tvLine2;
        VH(@NonNull View itemView) {
            super(itemView);
            tvLine1 = itemView.findViewById(R.id.tvLine1);
            tvLine2 = itemView.findViewById(R.id.tvLine2);
        }
    }
}
