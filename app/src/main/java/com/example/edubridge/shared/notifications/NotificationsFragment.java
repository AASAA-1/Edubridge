package com.example.edubridge.shared.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.example.edubridge.parent.AnnouncementDetailsFragment;
import com.example.edubridge.shared.messaging.ChatFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private final List<NotificationItem> items = new ArrayList<>();
    private NotificationsAdapter adapter;
    private RecyclerView rv;
    private ImageView emptyIcon;
    private TextView emptyText;
    private ListenerRegistration listenerReg;

    private FirebaseFirestore db;
    private String currentUserId;

    public NotificationsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_notifications, container, false);

        rv        = v.findViewById(R.id.rvNotifications);
        emptyIcon = v.findViewById(R.id.empty_icon);
        emptyText = v.findViewById(R.id.emptyText);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return v;
        currentUserId = user.getUid();

        db = FirebaseFirestore.getInstance();

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationsAdapter(items, new NotificationsAdapter.ActionListener() {
            @Override
            public void onMarkRead(NotificationItem item) {
                markAsRead(item);
            }
            @Override
            public void onDelete(NotificationItem item) {
                deleteNotification(item);
            }
            @Override
            public void onNavigate(NotificationItem item) {
                navigateToSource(item);
            }
        });
        rv.setAdapter(adapter);

        loadNotifications();
        return v;
    }

    // ── Real-time listener ────────────────────────────────────────────────────
    // No orderBy — avoids composite-index requirement on Spark plan.
    // Sorting is done client-side (newest first) after each rebuild.

    private void loadNotifications() {
        listenerReg = db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .limit(100)
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;
                    if (error != null || snapshots == null) return;

                    items.clear();
                    snapshots.getDocuments().forEach(doc -> {
                        NotificationItem n = new NotificationItem();
                        n.id         = doc.getId();
                        n.userId     = doc.getString("userId");
                        n.senderID   = doc.getString("senderID");
                        n.senderName = doc.getString("senderName");
                        Long countVal = doc.getLong("count");
                        n.count    = countVal != null ? countVal : 1L;
                        n.title    = doc.getString("title");
                        n.body     = doc.getString("body");
                        n.type     = doc.getString("type");
                        Boolean readVal = doc.getBoolean("read");
                        n.read     = readVal != null && readVal;
                        n.createdAt = doc.getTimestamp("createdAt");
                        n.refId    = doc.getString("refId");
                        n.refDate  = doc.getString("refDate");

                        if (n.title == null) n.title = "Notification";
                        if (n.body  == null) n.body  = "";
                        items.add(n);
                    });

                    // Sort newest first (replaces server-side orderBy)
                    Collections.sort(items, (a, b) -> {
                        if (a.createdAt == null) return 1;
                        if (b.createdAt == null) return -1;
                        return Long.compare(
                                b.createdAt.toDate().getTime(),
                                a.createdAt.toDate().getTime());
                    });

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void markAsRead(NotificationItem item) {
        db.collection("notifications").document(item.id)
                .update("read", true, "count", 0L)
                .addOnFailureListener(e -> {
                    if (isAdded())
                        Toast.makeText(getContext(),
                                R.string.notification_update_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteNotification(NotificationItem item) {
        db.collection("notifications").document(item.id)
                .delete()
                .addOnFailureListener(e -> {
                    if (isAdded())
                        Toast.makeText(getContext(),
                                R.string.notification_delete_failed, Toast.LENGTH_SHORT).show();
                });
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    // Tapping a notification card marks it read, then opens the relevant screen.

    private void navigateToSource(NotificationItem item) {
        // Always mark as read when tapped
        if (!item.read) markAsRead(item);

        String type = item.type != null ? item.type : "";
        switch (type) {
            case "message":
                openChat(item);
                break;
            case "announcement":
                openAnnouncementDetails(item);
                break;
            case "event":
                openAnnouncementDetails(item); // reuse detail screen for events
                break;
            case "incident":
                openLiveStream(item);
                break;
            default:
                // Unknown type — just mark read, nothing to navigate to
                break;
        }
    }

    /** Opens ChatFragment directed at the notification sender. */
    private void openChat(NotificationItem item) {
        if (item.senderID == null) return;
        ChatFragment chat = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("receiverId",   item.senderID);
        args.putString("receiverName", item.senderName != null ? item.senderName : "");
        chat.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, chat)
                .addToBackStack(null)
                .commit();
    }

    /** Opens AnnouncementDetailsFragment with data stored in the notification. */
    private void openAnnouncementDetails(NotificationItem item) {
        Bundle args = new Bundle();
        args.putString("title", item.title);
        args.putString("body",  item.body);
        args.putString("date",  item.refDate  != null ? item.refDate  : "");
        args.putString("by",    item.senderName != null ? item.senderName : "");

        AnnouncementDetailsFragment frag = new AnnouncementDetailsFragment();
        frag.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, frag)
                .addToBackStack(null)
                .commit();
    }

    private void openLiveStream(NotificationItem item) {

        String channel = item.refId != null ? item.refId : "class101";

        com.example.edubridge.parent.ParentLiveClassFragment fragment =
                new com.example.edubridge.parent.ParentLiveClassFragment();

        Bundle args = new Bundle();
        args.putString("channel", channel);
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // ── Empty state ───────────────────────────────────────────────────────────

    private void updateEmptyState() {
        boolean empty = items.isEmpty();
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyIcon.setVisibility(empty ? View.VISIBLE : View.GONE);
        emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerReg != null) listenerReg.remove();
    }
}
