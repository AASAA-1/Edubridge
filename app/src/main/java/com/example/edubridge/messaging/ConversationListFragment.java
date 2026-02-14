package com.example.edubridge.messaging;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ConversationListFragment extends Fragment {

    private RecyclerView recyclerContacts;
    private ProgressBar progressBar;
    private TextView emptyText;
    private final List<Contact> contacts = new ArrayList<>();
    private ContactAdapter adapter;
    private String currentUserId;

    public ConversationListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_conversation_list, container, false);

        recyclerContacts = v.findViewById(R.id.recycler_contacts);
        progressBar = v.findViewById(R.id.progress_bar);
        emptyText = v.findViewById(R.id.empty_text);

        recyclerContacts.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ContactAdapter(contacts, this::openChat);
        recyclerContacts.setAdapter(adapter);

        v.findViewById(R.id.btn_back).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack());

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        loadContacts();

        return v;
    }

    private void loadContacts() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerContacts.setVisibility(View.GONE);
        emptyText.setVisibility(View.GONE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // First get current user's type
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(currentUserDoc -> {
                    if (!isAdded()) return;
                    String myType = currentUserDoc.getString("usertype");

                    // Determine the opposite role
                    String targetType;
                    if ("teacher".equals(myType)) {
                        targetType = "parent";
                    } else {
                        targetType = "teacher";
                    }

                    // Query users of opposite role
                    db.collection("users")
                            .whereEqualTo("usertype", targetType)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!isAdded()) return;
                                contacts.clear();

                                if (querySnapshot.isEmpty()) {
                                    showEmpty();
                                    return;
                                }

                                List<DocumentSnapshot> docs = querySnapshot.getDocuments();
                                AtomicInteger pending = new AtomicInteger(docs.size());

                                for (DocumentSnapshot doc : docs) {
                                    String userId = doc.getId();
                                    String fullName = doc.getString("fullName");
                                    if (fullName == null) fullName = doc.getString("fullname");
                                    if (fullName == null) fullName = "Unknown";

                                    Contact contact = new Contact(userId, fullName, targetType);
                                    contacts.add(contact);

                                    // Fetch last message and unread count for this contact
                                    fetchContactMetadata(contact, () -> {
                                        if (pending.decrementAndGet() == 0) {
                                            // sort by last message time
                                            Collections.sort(contacts, (a, b) ->
                                                    Long.compare(b.getLastMessageTime(), a.getLastMessageTime()));
                                            showContacts();
                                        }
                                    });
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(getContext(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), R.string.no_internet, Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchContactMetadata(Contact contact, Runnable onDone) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get last message between me and this contact
        // Then pick the most recent
        db.collection("messages")
                .whereEqualTo("senderID", currentUserId)
                .whereEqualTo("receiverID", contact.getUserId())
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(sent -> {
                    db.collection("messages")
                            .whereEqualTo("senderID", contact.getUserId())
                            .whereEqualTo("receiverID", currentUserId)
                            .orderBy("sentAt", Query.Direction.DESCENDING)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(received -> {
                                // Find most recent message from both directions
                                DocumentSnapshot lastSent = !sent.isEmpty() ? sent.getDocuments().get(0) : null;
                                DocumentSnapshot lastReceived = !received.isEmpty() ? received.getDocuments().get(0) : null;

                                DocumentSnapshot lastMsg = null;
                                if (lastSent != null && lastReceived != null) {
                                    com.google.firebase.Timestamp tSent = lastSent.getTimestamp("sentAt");
                                    com.google.firebase.Timestamp tReceived = lastReceived.getTimestamp("sentAt");
                                    if (tSent != null && tReceived != null) {
                                        lastMsg = tSent.compareTo(tReceived) >= 0 ? lastSent : lastReceived;
                                    } else {
                                        lastMsg = lastSent;
                                    }
                                } else if (lastSent != null) {
                                    lastMsg = lastSent;
                                } else {
                                    lastMsg = lastReceived;
                                }

                                if (lastMsg != null) {
                                    String body = lastMsg.getString("body");
                                    contact.setLastMessage(body != null ? body : "");
                                    com.google.firebase.Timestamp ts = lastMsg.getTimestamp("sentAt");
                                    if (ts != null) {
                                        contact.setLastMessageTime(ts.toDate().getTime());
                                    }
                                }

                                // Count unread messages from this contact to me
                                db.collection("messages")
                                        .whereEqualTo("senderID", contact.getUserId())
                                        .whereEqualTo("receiverID", currentUserId)
                                        .whereEqualTo("read", false)
                                        .get()
                                        .addOnSuccessListener(unread -> {
                                            contact.setUnreadCount(unread.size());
                                            onDone.run();
                                        })
                                        .addOnFailureListener(e -> onDone.run());
                            })
                            .addOnFailureListener(e -> onDone.run());
                })
                .addOnFailureListener(e -> onDone.run());
    }

    private void showContacts() {
        if (!isAdded()) return;
        progressBar.setVisibility(View.GONE);
        if (contacts.isEmpty()) {
            showEmpty();
        } else {
            recyclerContacts.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }
    }

    private void showEmpty() {
        if (!isAdded()) return;
        progressBar.setVisibility(View.GONE);
        recyclerContacts.setVisibility(View.GONE);
        emptyText.setVisibility(View.VISIBLE);
    }

    private void openChat(Contact contact) {
        ChatFragment chatFragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString("receiverId", contact.getUserId());
        args.putString("receiverName", contact.getFullName());
        chatFragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, chatFragment)
                .addToBackStack(null)
                .commit();
    }
}
