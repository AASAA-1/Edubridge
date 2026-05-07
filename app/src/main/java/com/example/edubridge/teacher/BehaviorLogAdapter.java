package com.example.edubridge.teacher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;

import java.util.List;

public class BehaviorLogAdapter extends RecyclerView.Adapter<BehaviorLogAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onEdit(BehaviorLogItem item);
        void onDelete(BehaviorLogItem item);
    }

    private final List<BehaviorLogItem> items;
    private OnItemActionListener listener;

    public BehaviorLogAdapter(List<BehaviorLogItem> items) {
        this.items = items;
    }

    public void setOnItemActionListener(OnItemActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_behavior_log, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        BehaviorLogItem item = items.get(position);

        h.tvDateTime.setText(item.getDayOfWeek() + "  •  " + item.getTimeOfDay());

        h.tvSubject.setText(item.getSubject());
        h.tvIncidentType.setText(item.getIncidentType());

        String desc = item.getDescription();
        if (desc != null && !desc.isEmpty()) {
            h.tvDescription.setText(desc);
            h.tvDescription.setVisibility(View.VISIBLE);
        } else {
            h.tvDescription.setVisibility(View.GONE);
        }

        h.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(item);
        });
        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(item);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvDateTime;
        final TextView tvSubject;
        final TextView tvIncidentType;
        final TextView tvDescription;
        final ImageButton btnEdit;
        final ImageButton btnDelete;

        ViewHolder(View v) {
            super(v);
            tvDateTime     = v.findViewById(R.id.tv_log_datetime);
            tvSubject      = v.findViewById(R.id.tv_log_subject);
            tvIncidentType = v.findViewById(R.id.tv_log_incident_type);
            tvDescription  = v.findViewById(R.id.tv_log_description);
            btnEdit        = v.findViewById(R.id.btn_edit_incident);
            btnDelete      = v.findViewById(R.id.btn_delete_incident);
        }
    }
}
