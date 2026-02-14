package com.example.edubridge.parent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.edubridge.R;
import java.util.ArrayList;

public class AnnouncementsAdapter extends RecyclerView.Adapter<AnnouncementsAdapter.VH> {

    public interface OnItemClick {
        void onClick(AnnouncementItem item);
    }

    private final ArrayList<AnnouncementItem> items;
    private final OnItemClick listener;

    public AnnouncementsAdapter(ArrayList<AnnouncementItem> items, OnItemClick listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_announcement, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AnnouncementItem it = items.get(position);

        h.tvTitle.setText(it.title);
        String meta = (it.date == null ? "" : it.date) + " • " + (it.createdByName == null ? "" : it.createdByName);
        h.tvMeta.setText(meta.trim());
        h.tvPreview.setText(it.body == null ? "" : it.body);

        h.itemView.setOnClickListener(v -> listener.onClick(it));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMeta, tvPreview;
        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvPreview = itemView.findViewById(R.id.tvPreview);
        }
    }
}
