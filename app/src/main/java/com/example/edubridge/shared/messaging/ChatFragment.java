package com.example.edubridge.shared.messaging;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatFragment extends Fragment {

    private static final int REQUEST_IMAGE_PICK = 101;
    private static final int REQUEST_FILE_PICK = 102;
    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 200;

    private RecyclerView recyclerMessages;
    private TextInputEditText inputMessage;
    private ImageView btnSend, btnAttach, btnImage, btnFile, btnVoice;
    private LinearLayout attachmentBar;
    private ProgressBar progressBar, uploadProgress;
    private MessageAdapter adapter;
    private final List<Message> messages = new ArrayList<>();
    private final Set<String> loadedMessageIds = new HashSet<>();

    private String currentUserId;
    private String currentUserFullName = "Someone";
    private String receiverId;
    private String receiverName;

    private FirebaseFirestore db;
    private ListenerRegistration sentListener;
    private ListenerRegistration receivedListener;

    private FileUploadHelper fileUploadHelper;
    private VoiceRecorderHelper voiceRecorderHelper;
    private boolean isRecordingVoice = false;

    public ChatFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);
        TextSizeHelper.applyScaleRecursively(v);

        // Initialize views
        recyclerMessages = v.findViewById(R.id.recycler_messages);
        inputMessage = v.findViewById(R.id.input_message);
        btnSend = v.findViewById(R.id.btn_send);
        progressBar = v.findViewById(R.id.progress_bar);
        btnAttach = v.findViewById(R.id.btn_attach);
        btnImage = v.findViewById(R.id.btn_image);
        btnFile = v.findViewById(R.id.btn_file);
        btnVoice = v.findViewById(R.id.btn_voice);
        attachmentBar = v.findViewById(R.id.attachment_bar);
        uploadProgress = v.findViewById(R.id.upload_progress);
        TextView contactNameView = v.findViewById(R.id.contact_name);

        // Initialize helpers
        fileUploadHelper = new FileUploadHelper();
        voiceRecorderHelper = new VoiceRecorderHelper();

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        // Fetch current user's full name
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("fullname");
                    if (name != null && !name.isEmpty()) {
                        currentUserFullName = name;
                    }
                });

        if (getArguments() != null) {
            receiverId = getArguments().getString("receiverId");
            receiverName = getArguments().getString("receiverName");
        }

        contactNameView.setText(receiverName);

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(layoutManager);
        adapter = new MessageAdapter(messages, currentUserId);
        recyclerMessages.setAdapter(adapter);

        // Back button
        v.findViewById(R.id.btn_back).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Send button - disabled initially
        btnSend.setEnabled(false);
        btnSend.setAlpha(0.3f);

        // Setup attachment features
        setupAttachmentFeatures();

        // Text change listener
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
        markMessagesAsRead();
        dismissIncomingNotification();

        return v;
    }

    private void setupAttachmentFeatures() {
        // Toggle attachment bar
        btnAttach.setOnClickListener(view -> {
            if (attachmentBar.getVisibility() == View.VISIBLE) {
                attachmentBar.setVisibility(View.GONE);
            } else {
                attachmentBar.setVisibility(View.VISIBLE);
            }
        });

        // Image picker
        btnImage.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Image"),
                    REQUEST_IMAGE_PICK);
            attachmentBar.setVisibility(View.GONE);
        });

        // File picker
        btnFile.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select File"),
                    REQUEST_FILE_PICK);
            attachmentBar.setVisibility(View.GONE);
        });

        // Voice recording
        btnVoice.setOnClickListener(view -> {
            if (isRecordingVoice) {
                stopVoiceRecording();
            } else {
                startVoiceRecording();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();

            if (requestCode == REQUEST_IMAGE_PICK) {
                uploadFile(fileUri, Message.TYPE_IMAGE);
            } else if (requestCode == REQUEST_FILE_PICK) {
                uploadFile(fileUri, Message.TYPE_FILE);
            }
        }
    }

    private void uploadFile(Uri fileUri, int messageType) {
        // Show upload progress
        if (uploadProgress != null) {
            uploadProgress.setVisibility(View.VISIBLE);
            uploadProgress.setProgress(0);
        }
        btnSend.setEnabled(false);

        fileUploadHelper.uploadFile(fileUri, new FileUploadHelper.UploadCallback() {
            @Override
            public void onProgress(double progress) {
                if (isAdded() && uploadProgress != null) {
                    uploadProgress.setProgress((int) progress);
                }
            }

            @Override
            public void onSuccess(String downloadUrl, String fileName, long fileSize) {
                if (isAdded()) {
                    if (uploadProgress != null) {
                        uploadProgress.setVisibility(View.GONE);
                    }
                    btnSend.setEnabled(true);
                    sendMediaMessage(downloadUrl, fileName, fileSize, messageType);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    if (uploadProgress != null) {
                        uploadProgress.setVisibility(View.GONE);
                    }
                    btnSend.setEnabled(true);
                    Toast.makeText(getContext(), "Upload failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendMediaMessage(String fileUrl, String fileName, long fileSize, int messageType) {
        Timestamp now = Timestamp.now();
        DocumentReference newDocRef = db.collection("messages").document();
        String newDocId = newDocRef.getId();

        Map<String, Object> msgData = new HashMap<>();
        msgData.put("senderID", currentUserId);
        msgData.put("receiverID", receiverId);
        msgData.put("body", fileName);
        msgData.put("messageType", messageType);
        msgData.put("fileUrl", fileUrl);
        msgData.put("fileName", fileName);
        msgData.put("fileSize", fileSize);
        msgData.put("sentAt", now);
        msgData.put("read", false);

        // Optimistic insert
        Message optimistic = new Message();
        optimistic.setMessageId(newDocId);
        optimistic.setSenderID(currentUserId);
        optimistic.setReceiverID(receiverId);
        optimistic.setBody(fileName);
        optimistic.setMessageType(messageType);
        optimistic.setFileUrl(fileUrl);
        optimistic.setFileName(fileName);
        optimistic.setFileSize(fileSize);
        optimistic.setSentAt(now);
        optimistic.setRead(false);

        loadedMessageIds.add(newDocId);
        insertSorted(optimistic);
        adapter.notifyItemInserted(messages.indexOf(optimistic));
        scrollToBottom();

        newDocRef.set(msgData)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "File sent", Toast.LENGTH_SHORT).show();
                    }
                    createOrStackNotification(receiverId, currentUserFullName, "📎 " + fileName);
                })
                .addOnFailureListener(e -> {
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

    private void startVoiceRecording() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_RECORD_AUDIO);
            return;
        }

        File outputDir = requireContext().getCacheDir();
        voiceRecorderHelper.startRecording(outputDir, new VoiceRecorderHelper.RecordingCallback() {
            @Override
            public void onRecordingStarted() {
                isRecordingVoice = true;
                btnVoice.setImageResource(android.R.drawable.ic_media_pause);
                inputMessage.setHint("Recording...");
                inputMessage.setEnabled(false);
                btnSend.setEnabled(false);
            }

            @Override
            public void onDurationChanged(int seconds) {
                inputMessage.setHint(String.format("Recording: %ds", seconds));
            }

            @Override
            public void onRecordingStopped(File audioFile, int duration) {
                isRecordingVoice = false;
                btnVoice.setImageResource(android.R.drawable.ic_btn_speak_now);
                inputMessage.setHint(R.string.type_message_hint);
                inputMessage.setEnabled(true);
                btnSend.setEnabled(true);

                // Upload voice recording
                uploadVoiceMessage(audioFile, duration);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                isRecordingVoice = false;
                btnVoice.setImageResource(android.R.drawable.ic_btn_speak_now);
                inputMessage.setHint(R.string.type_message_hint);
                inputMessage.setEnabled(true);
                btnSend.setEnabled(true);
            }
        });
    }

    private void stopVoiceRecording() {
        voiceRecorderHelper.stopRecording();
    }

    private void uploadVoiceMessage(File audioFile, int duration) {
        Uri audioUri = Uri.fromFile(audioFile);

        fileUploadHelper.uploadFile(audioUri, new FileUploadHelper.UploadCallback() {
            @Override
            public void onProgress(double progress) {
                // Optional: show upload progress
            }

            @Override
            public void onSuccess(String downloadUrl, String fileName, long fileSize) {
                if (isAdded()) {
                    sendVoiceMessage(downloadUrl, duration);
                }
                // Clean up temp file
                audioFile.delete();
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to send voice message",
                            Toast.LENGTH_SHORT).show();
                }
                audioFile.delete();
            }
        });
    }

    private void sendVoiceMessage(String fileUrl, int duration) {
        Timestamp now = Timestamp.now();
        DocumentReference newDocRef = db.collection("messages").document();
        String newDocId = newDocRef.getId();

        Map<String, Object> msgData = new HashMap<>();
        msgData.put("senderID", currentUserId);
        msgData.put("receiverID", receiverId);
        msgData.put("body", "🎤 Voice message (" + duration + "s)");
        msgData.put("messageType", Message.TYPE_VOICE);
        msgData.put("fileUrl", fileUrl);
        msgData.put("fileName", "Voice message");
        msgData.put("fileSize", 0);
        msgData.put("voiceDuration", duration);
        msgData.put("sentAt", now);
        msgData.put("read", false);

        // Optimistic insert
        Message optimistic = new Message();
        optimistic.setMessageId(newDocId);
        optimistic.setSenderID(currentUserId);
        optimistic.setReceiverID(receiverId);
        optimistic.setBody("🎤 Voice message (" + duration + "s)");
        optimistic.setMessageType(Message.TYPE_VOICE);
        optimistic.setFileUrl(fileUrl);
        optimistic.setFileName("Voice message");
        optimistic.setVoiceDuration(duration);
        optimistic.setSentAt(now);
        optimistic.setRead(false);

        loadedMessageIds.add(newDocId);
        insertSorted(optimistic);
        adapter.notifyItemInserted(messages.indexOf(optimistic));
        scrollToBottom();

        newDocRef.set(msgData)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Voice message sent", Toast.LENGTH_SHORT).show();
                    }
                    createOrStackNotification(receiverId, currentUserFullName, "🎤 Voice message");
                })
                .addOnFailureListener(e -> {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecording();
            } else {
                Toast.makeText(getContext(), "Audio permission required for voice messages",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadMessages() {
        progressBar.setVisibility(View.VISIBLE);

        sentListener = db.collection("messages")
                .whereEqualTo("senderID", currentUserId)
                .whereEqualTo("receiverID", receiverId)
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

        receivedListener = db.collection("messages")
                .whereEqualTo("senderID", receiverId)
                .whereEqualTo("receiverID", currentUserId)
                .limit(100)
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;
                    if (error != null) {
                        progressBar.setVisibility(View.GONE);
                        return;
                    }
                    if (snapshots != null) {
                        processSnapshot(snapshots.getDocumentChanges());
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
                msg.setMessageType(doc.getLong("messageType") != null ?
                        doc.getLong("messageType").intValue() : Message.TYPE_TEXT);
                msg.setFileUrl(doc.getString("fileUrl"));
                msg.setFileName(doc.getString("fileName"));
                msg.setFileSize(doc.getLong("fileSize") != null ? doc.getLong("fileSize") : 0);
                msg.setVoiceDuration(doc.getLong("voiceDuration") != null ?
                        doc.getLong("voiceDuration").intValue() : 0);
                msg.setSentAt(doc.getTimestamp("sentAt"));
                Boolean readVal = doc.getBoolean("read");
                msg.setRead(readVal != null && readVal);

                insertSorted(msg);
                hasNew = true;
            }
        }

        if (hasNew) {
            Collections.sort(messages, (a, b) -> {
                if (a.getSentAt() == null) return 1;
                if (b.getSentAt() == null) return -1;
                return Long.compare(
                        a.getSentAt().toDate().getTime(),
                        b.getSentAt().toDate().getTime());
            });
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
        DocumentReference newDocRef = db.collection("messages").document();
        String newDocId = newDocRef.getId();

        Map<String, Object> msgData = new HashMap<>();
        msgData.put("senderID", currentUserId);
        msgData.put("receiverID", receiverId);
        msgData.put("body", text);
        msgData.put("messageType", Message.TYPE_TEXT);
        msgData.put("sentAt", now);
        msgData.put("read", false);

        Message optimistic = new Message();
        optimistic.setMessageId(newDocId);
        optimistic.setSenderID(currentUserId);
        optimistic.setReceiverID(receiverId);
        optimistic.setBody(text);
        optimistic.setMessageType(Message.TYPE_TEXT);
        optimistic.setSentAt(now);
        optimistic.setRead(false);

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
                    createOrStackNotification(receiverId, currentUserFullName, text);
                })
                .addOnFailureListener(e -> {
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

    private void createOrStackNotification(String receiverId, String senderName, String text) {
        String docId = receiverId + "_" + currentUserId;

        Map<String, Object> notif = new HashMap<>();
        notif.put("userId", receiverId);
        notif.put("senderID", currentUserId);
        notif.put("senderName", senderName);
        notif.put("title", "New Message from " + senderName);
        notif.put("body", text);
        notif.put("type", "message");
        notif.put("read", false);
        notif.put("createdAt", Timestamp.now());
        notif.put("count", FieldValue.increment(1));

        db.collection("notifications").document(docId)
                .set(notif, SetOptions.merge());
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

    private void dismissIncomingNotification() {
        String docId = currentUserId + "_" + receiverId;
        db.collection("notifications").document(docId)
                .update("read", true, "count", 0L);
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
        voiceRecorderHelper.cleanup();
    }
}