package com.example.edubridge.shared.notifications;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.example.edubridge.shared.messaging.TimeFormatter;

import java.util.List;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    public interface ActionListener {
        void onMarkRead(NotificationItem item);
        void onDelete(NotificationItem item);
    }

    private final List<NotificationItem> items;
    private final ActionListener listener;

    public NotificationsAdapter(List<NotificationItem> items, ActionListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        NotificationItem item = items.get(position);

        // Build title with count and resolved sender name (Mod 1 & 2)
        if (item.senderName != null && !item.senderName.isEmpty()) {
            String displayTitle = item.count > 1
                    ? item.count + " New Messages from " + item.senderName
                    : "New Message from " + item.senderName;
            h.tvTitle.setText(displayTitle);
        } else {
            h.tvTitle.setText(item.title);
        }
        h.tvBody.setText(item.body);

        // Bold title and show dot for unread; normal style when read
        if (!item.read) {
            h.tvTitle.setTypeface(null, Typeface.BOLD);
            h.unreadDot.setVisibility(View.VISIBLE);
            h.btnMarkRead.setVisibility(View.VISIBLE);
        } else {
            h.tvTitle.setTypeface(null, Typeface.NORMAL);
            h.unreadDot.setVisibility(View.INVISIBLE);
            h.btnMarkRead.setVisibility(View.GONE);
        }

        // Format timestamp
        if (item.createdAt != null) {
            h.tvTime.setText(TimeFormatter.formatTimestamp(
                    item.createdAt.toDate().getTime()));
        } else {
            h.tvTime.setText("");
        }

        h.btnMarkRead.setOnClickListener(v -> listener.onMarkRead(item));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View     unreadDot;
        TextView tvTitle;
        TextView tvTime;
        TextView tvBody;
        TextView btnMarkRead;
        TextView btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            unreadDot   = itemView.findViewById(R.id.unreadDot);
            tvTitle     = itemView.findViewById(R.id.tvTitle);
            tvTime      = itemView.findViewById(R.id.tvTime);
            tvBody      = itemView.findViewById(R.id.tvBody);
            btnMarkRead = itemView.findViewById(R.id.btnMarkRead);
            btnDelete   = itemView.findViewById(R.id.btnDelete);
        }
    }
}
