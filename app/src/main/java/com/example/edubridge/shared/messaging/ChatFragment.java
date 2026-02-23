package com.example.edubridge.shared.messaging;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerMessages;
    private TextInputEditText inputMessage;
    private ImageView btnSend;
    private ProgressBar progressBar;
    private MessageAdapter adapter;
    private final List<Message> messages = new ArrayList<>();
    private final Set<String> loadedMessageIds = new HashSet<>();

    private String currentUserId;
    private String receiverId;
    private String receiverName;

    private FirebaseFirestore db;
    private ListenerRegistration sentListener;
    private ListenerRegistration receivedListener;

    public ChatFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerMessages = v.findViewById(R.id.recycler_messages);
        inputMessage = v.findViewById(R.id.input_message);
        btnSend = v.findViewById(R.id.btn_send);
        progressBar = v.findViewById(R.id.progress_bar);
        TextView contactNameView = v.findViewById(R.id.contact_name);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            receiverId = getArguments().getString("receiverId");
            receiverName = getArguments().getString("receiverName");
        }

        contactNameView.setText(receiverName);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(layoutManager);
        adapter = new MessageAdapter(messages, currentUserId);
        recyclerMessages.setAdapter(adapter);

        // Back button
        v.findViewById(R.id.btn_back).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Send button — disabled initially
        btnSend.setEnabled(false);
        btnSend.setAlpha(0.3f);

        // Enable/disable send based on text input
        inputMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.toString().trim().length() > 0;
                btnSend.setEnabled(hasText);
                btnSend.setAlpha(hasText ? 1.0f : 0.3f);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Send button click
        btnSend.setOnClickListener(view -> sendMessage());

        // Load messages
        loadMessages();

        // Mark received messages as read
        markMessagesAsRead();

        return v;
    }

    private void loadMessages() {
        progressBar.setVisibility(View.VISIBLE);

        // Listen for messages I sent to them
        sentListener = db.collection("messages")
                .whereEqualTo("senderID", currentUserId)
                .whereEqualTo("receiverID", receiverId)
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .limit(100)
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;
                    if (error != null) {
                        progressBar.setVisibility(View.GONE);
                        return;
                    }
                    if (snapshots != null) {
                        processSnapshot(snapshots.getDocumentChanges());
                    }
                });

        // Listen for messages they sent to me
        receivedListener = db.collection("messages")
                .whereEqualTo("senderID", receiverId)
                .whereEqualTo("receiverID", currentUserId)
                .orderBy("sentAt", Query.Direction.ASCENDING)
                .limit(100)
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;
                    if (error != null) {
                        progressBar.setVisibility(View.GONE);
                        return;
                    }
                    if (snapshots != null) {
                        processSnapshot(snapshots.getDocumentChanges());
                        // Mark newly received messages as read
                        markMessagesAsRead();
                    }
                });
    }

    private void processSnapshot(List<DocumentChange> changes) {
        boolean hasNew = false;
        for (DocumentChange dc : changes) {
            if (dc.getType() == DocumentChange.Type.ADDED) {
                DocumentSnapshot doc = dc.getDocument();
                String docId = doc.getId();

                if (loadedMessageIds.contains(docId)) continue;
                loadedMessageIds.add(docId);

                Message msg = new Message();
                msg.setMessageId(docId);
                msg.setSenderID(doc.getString("senderID"));
                msg.setReceiverID(doc.getString("receiverID"));
                msg.setBody(doc.getString("body"));
                msg.setSentAt(doc.getTimestamp("sentAt"));
                Boolean readVal = doc.getBoolean("read");
                msg.setRead(readVal != null && readVal);

                // Insert in sorted order by sentAt
                insertSorted(msg);
                hasNew = true;
            }
        }

        if (hasNew) {
            progressBar.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
            scrollToBottom();
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void insertSorted(Message msg) {
        if (msg.getSentAt() == null) {
            messages.add(msg);
            return;
        }
        long msgTime = msg.getSentAt().toDate().getTime();
        int insertIdx = messages.size();
        for (int i = messages.size() - 1; i >= 0; i--) {
            Timestamp ts = messages.get(i).getSentAt();
            if (ts != null && ts.toDate().getTime() <= msgTime) {
                insertIdx = i + 1;
                break;
            }
            if (i == 0) {
                insertIdx = 0;
            }
        }
        messages.add(insertIdx, msg);
    }

    private void scrollToBottom() {
        if (!messages.isEmpty()) {
            recyclerMessages.scrollToPosition(messages.size() - 1);
        }
    }

    private void sendMessage() {
        String text = inputMessage.getText() != null
                ? inputMessage.getText().toString().trim() : "";
        if (text.isEmpty()) return;

        Timestamp now = Timestamp.now();

        // Pre-generate the document reference so its ID is known before the write.
        DocumentReference newDocRef = db.collection("messages").document();
        String newDocId = newDocRef.getId();

        Map<String, Object> msgData = new HashMap<>();
        msgData.put("senderID", currentUserId);
        msgData.put("receiverID", receiverId);
        msgData.put("body", text);
        msgData.put("sentAt", now);
        msgData.put("read", false);

        // Optimistic insert: add the message to the UI immediately.
        Message optimistic = new Message();
        optimistic.setMessageId(newDocId);
        optimistic.setSenderID(currentUserId);
        optimistic.setReceiverID(receiverId);
        optimistic.setBody(text);
        optimistic.setSentAt(now);
        optimistic.setRead(false);

        // Register the ID before writing so processSnapshot skips it when
        // the sentListener fires for this document (prevents a duplicate).
        loadedMessageIds.add(newDocId);
        insertSorted(optimistic);
        adapter.notifyItemInserted(messages.indexOf(optimistic));
        scrollToBottom();

        inputMessage.setText("");
        hideKeyboard();

        newDocRef.set(msgData)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), R.string.message_sent, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Roll back the optimistic insert if the write failed.
                    int idx = messages.indexOf(optimistic);
                    if (idx >= 0) {
                        messages.remove(idx);
                        loadedMessageIds.remove(newDocId);
                        adapter.notifyItemRemoved(idx);
                    }
                    if (isAdded()) {
                        Toast.makeText(getContext(), R.string.send_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void markMessagesAsRead() {
        db.collection("messages")
                .whereEqualTo("senderID", receiverId)
                .whereEqualTo("receiverID", currentUserId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) return;
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.update(doc.getReference(), "read", true);
                    }
                    batch.commit();
                });
    }

    private void hideKeyboard() {
        if (inputMessage != null && getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(inputMessage.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (sentListener != null) sentListener.remove();
        if (receivedListener != null) receivedListener.remove();
    }
}
