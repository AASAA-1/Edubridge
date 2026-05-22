package com.example.edubridge.shared.notifications;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.example.edubridge.shared.TextSizeHelper;
import com.example.edubridge.shared.messaging.TimeFormatter;

import java.util.List;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    public interface ActionListener {
        void onMarkRead(NotificationItem item);
        void onDelete(NotificationItem item);
        /** Called when the user taps the card body to navigate to the source. */
        void onNavigate(NotificationItem item);
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
        TextSizeHelper.applyScaleRecursively(v); // scale item
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        NotificationItem item = items.get(position);

        // ── Title ─────────────────────────────────────────────────────────────
        h.tvTitle.setText(buildTitle(item));

        h.tvBody.setText(item.body != null ? item.body : "");

        // ── Read / unread styling ─────────────────────────────────────────────
        if (!item.read) {
            h.tvTitle.setTypeface(null, Typeface.BOLD);
            h.unreadDot.setVisibility(View.VISIBLE);
            h.btnMarkRead.setVisibility(View.VISIBLE);
        } else {
            h.tvTitle.setTypeface(null, Typeface.NORMAL);
            h.unreadDot.setVisibility(View.INVISIBLE); // INVISIBLE preserves layout spacing
            h.btnMarkRead.setVisibility(View.GONE);
        }

        // ── Timestamp ─────────────────────────────────────────────────────────
        if (item.createdAt != null) {
            h.tvTime.setText(TimeFormatter.formatTimestamp(
                    item.createdAt.toDate().getTime()));
        } else {
            h.tvTime.setText("");
        }

        // ── Tap whole card → navigate to source ───────────────────────────────
        h.itemView.setOnClickListener(v -> listener.onNavigate(item));

        // ── Action buttons ────────────────────────────────────────────────────
        h.btnMarkRead.setOnClickListener(v -> listener.onMarkRead(item));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    /**
     * Builds the display title based on notification type.
     *  message      → "X New Messages from Name"  (existing stacking logic)
     *  announcement → "New Announcement: {title}"
     *  event        → "New Event: {title}"
     *  other        → stored title field
     */
    private String buildTitle(NotificationItem item) {
        String type = item.type != null ? item.type : "";
        switch (type) {
            case "message":
                if (item.senderName != null && !item.senderName.isEmpty()) {
                    return item.count > 1
                            ? item.count + " New Messages from " + item.senderName
                            : "New Message from " + item.senderName;
                }
                break;
            case "announcement":
                return "New Announcement: " + (item.title != null ? item.title : "");
            case "event":
                return "New Event: " + (item.title != null ? item.title : "");
        }
        return item.title != null ? item.title : "Notification";
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
