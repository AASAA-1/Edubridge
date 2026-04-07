package com.example.edubridge.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AdminEventModificationFragment extends Fragment {

    private EditText titleEdit;
    private EditText descriptionEdit;
    private EditText startDateEdit;
    private EditText endDateEdit;

    private Spinner typeSpinner;
    private Spinner classSpinner;

    private Button saveButton;

    private FirebaseFirestore db;

    private String eventId = null;

    private ArrayList<String> classNames = new ArrayList<>();

    public AdminEventModificationFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_admin_event_modification,
                container,
                false
        );

        titleEdit = view.findViewById(R.id.event_title_edit);
        descriptionEdit = view.findViewById(R.id.event_description_edit);
        startDateEdit = view.findViewById(R.id.start_date_edit);
        endDateEdit = view.findViewById(R.id.end_date_edit);

        typeSpinner = view.findViewById(R.id.event_type_spinner);
        classSpinner = view.findViewById(R.id.class_spinner);

        saveButton = view.findViewById(R.id.save_event_button);

        db = FirebaseFirestore.getInstance();

        setupTypeSpinner();
        loadClasses();

        if (getArguments() != null) {

            eventId = getArguments().getString("eventId");

            if (eventId != null) {
                loadEvent();
            }
        }

        saveButton.setOnClickListener(v -> saveEvent());

        return view;
    }

    /**
     * Setup static event types
     */
    private void setupTypeSpinner() {

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList("Field Trip", "School Event", "Important", "Other")
        );

        typeSpinner.setAdapter(typeAdapter);
    }

    /**
     * Load class names from Firestore
     */
    private void loadClasses() {

        db.collection("classes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    classNames.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {

                        String name = doc.getString("name");

                        if (name != null) {
                            classNames.add(name);
                        }
                    }

                    ArrayAdapter<String> classAdapter = new ArrayAdapter<>(
                            getContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            classNames
                    );

                    classSpinner.setAdapter(classAdapter);
                });
    }

    /**
     * Load existing event (edit mode)
     */
    private void loadEvent() {

        db.collection("events")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {

                        titleEdit.setText(documentSnapshot.getString("title"));
                        descriptionEdit.setText(documentSnapshot.getString("description"));
                        startDateEdit.setText(documentSnapshot.getString("startAt"));
                        endDateEdit.setText(documentSnapshot.getString("endAt"));

                        String type = documentSnapshot.getString("type");
                        String className = documentSnapshot.getString("className");

                        if (type != null) {
                            setSpinnerValue(typeSpinner, type);
                        }

                        if (className != null) {
                            setSpinnerValue(classSpinner, className);
                        }
                    }
                });
    }

    /**
     * Helper to set spinner selected value
     */
    private void setSpinnerValue(Spinner spinner, String value) {

        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();

        for (int i = 0; i < adapter.getCount(); i++) {

            if (adapter.getItem(i).toString().equals(value)) {

                spinner.setSelection(i);
                break;
            }
        }
    }

    /**
     * Notifies all parents and teachers when a new event is created.
     * Doc ID: {uid}_event_{eventId} — deterministic, no duplicates.
     */
    private void fanOutEventNotification(String eventId, String title,
                                          String description, String startAt) {
        String adminId = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "admin";

        for (String userType : new String[]{"parent", "teacher"}) {
            db.collection("users").whereEqualTo("usertype", userType).get()
                    .addOnSuccessListener(snap -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                            String uid = doc.getId();

                            java.util.Map<String, Object> notif = new java.util.HashMap<>();
                            notif.put("userId",     uid);
                            notif.put("senderID",   adminId);
                            notif.put("senderName", "Admin");
                            notif.put("title",      title);
                            notif.put("body",       description);
                            notif.put("type",       "event");
                            notif.put("read",       false);
                            notif.put("count",      1L);
                            notif.put("refId",      eventId);
                            notif.put("refDate",    startAt);
                            notif.put("createdAt",  com.google.firebase.Timestamp.now());

                            db.collection("notifications")
                                    .document(uid + "_event_" + eventId)
                                    .set(notif);
                        }
                    });
        }
    }

    /**
     * Save event to Firestore
     */
    private void saveEvent() {

        String title = titleEdit.getText().toString().trim();
        String description = descriptionEdit.getText().toString().trim();
        String start = startDateEdit.getText().toString().trim();
        String end = endDateEdit.getText().toString().trim();

        String type = typeSpinner.getSelectedItem().toString();
        String className = classSpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(title)) {
            Toast.makeText(getContext(), "Title required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> event = new HashMap<>();

        event.put("title", title);
        event.put("description", description);
        event.put("startAt", start);
        event.put("endAt", end);
        event.put("type", type);
        event.put("classId", className);

        if (eventId == null) {

            db.collection("events")
                    .add(event)
                    .addOnSuccessListener(doc -> {
                        fanOutEventNotification(doc.getId(), title, description, start);

                        Toast.makeText(getContext(),
                                "Event created",
                                Toast.LENGTH_SHORT).show();

                        requireActivity()
                                .getSupportFragmentManager()
                                .popBackStack();
                    });

        } else {

            db.collection("events")
                    .document(eventId)
                    .update(event)
                    .addOnSuccessListener(aVoid -> {

                        Toast.makeText(getContext(),
                                "Event updated",
                                Toast.LENGTH_SHORT).show();

                        requireActivity()
                                .getSupportFragmentManager()
                                .popBackStack();
                    });
        }
    }
}