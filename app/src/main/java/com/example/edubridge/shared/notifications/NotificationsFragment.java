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
        });
        rv.setAdapter(adapter);

        loadNotifications();
        return v;
    }

    // Real-time listener: rebuilds the list on every Firestore change.
    // No orderBy used — avoids the composite-index requirement on the Spark plan.
    // Sorting is done client-side (newest first) after each rebuild.
    private void loadNotifications() {
        listenerReg = db.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .limit(50)
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

    // Update the `read` field and reset the count so the next message starts fresh.
    private void markAsRead(NotificationItem item) {
        db.collection("notifications").document(item.id)
                .update("read", true, "count", 0L)
                .addOnFailureListener(e -> {
                    if (isAdded())
                        Toast.makeText(getContext(),
                                R.string.notification_update_failed, Toast.LENGTH_SHORT).show();
                });
    }

    // Delete the document. The snapshot listener removes the row automatically.
    private void deleteNotification(NotificationItem item) {
        db.collection("notifications").document(item.id)
                .delete()
                .addOnFailureListener(e -> {
                    if (isAdded())
                        Toast.makeText(getContext(),
                                R.string.notification_delete_failed, Toast.LENGTH_SHORT).show();
                });
    }

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
