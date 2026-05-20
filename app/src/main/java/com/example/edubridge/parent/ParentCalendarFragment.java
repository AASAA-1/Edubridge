package com.example.edubridge.parent;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.example.edubridge.shared.CalendarAdapter;
import com.example.edubridge.shared.CalendarEvent;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ParentCalendarFragment extends Fragment {

    private static final String TAG = "ParentCalendar";
    private static final String PREFS = "attendance_prefs";
    private static final String KEY_SELECTED_CHILD_ID = "selected_child_id";
    private static final String KEY_SELECTED_CHILD_NAME = "selected_child_name";

    private RecyclerView recyclerView;
    private TextView emptyText;
    private TextView tvSelectedChild;
    private CalendarAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserId;
    private final List<CalendarEvent> events = new ArrayList<>();
    private boolean hideChildSelector = false;

    // Child selection data
    private final List<String> childNames = new ArrayList<>();
    private final List<String> childIds = new ArrayList<>();
    private final Map<String, String> childNameById = new HashMap<>();
    private final Map<String, String> childClassIdByName = new HashMap<>();

    private String selectedChildId = "";
    private String selectedChildName = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_calendar, container, false);
        TextSizeHelper.applyScaleRecursively(v);

        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Check if child selector should be hidden (student mode)
        if (getArguments() != null) {
            hideChildSelector = getArguments().getBoolean("hideChildSelector", false);
        }

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        emptyText = v.findViewById(R.id.emptyText);
        tvSelectedChild = v.findViewById(R.id.tvSelectedChild);
        recyclerView = v.findViewById(R.id.rvCalendar);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CalendarAdapter();
        recyclerView.setAdapter(adapter);

        // Hide child selector in student mode
        if (hideChildSelector) {
            tvSelectedChild.setVisibility(View.GONE);
        } else {
            tvSelectedChild.setOnClickListener(view -> showChildPicker());
        }

        loadParentStudentClasses();

        return v;
    }

    private void loadParentStudentClasses() {
        if (currentUserId == null) {
            showEmptyState("User not logged in");
            return;
        }

        db.collection("users")
                .document(currentUserId)
                .collection("students")
                .get()
                .addOnSuccessListener(snap -> {
                    childNames.clear();
                    childIds.clear();
                    childNameById.clear();
                    childClassIdByName.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String studentId = doc.getString("studentId");
                        String name = doc.getString("name");
                        String classId = doc.getString("classId");

                        if (studentId != null && !studentId.isEmpty()) {
                            childIds.add(studentId);
                            childNames.add(name != null ? name : "Unknown");
                            childNameById.put(studentId, name != null ? name : "Unknown");
                            if (classId != null) {
                                childClassIdByName.put(studentId, classId);
                            }
                        }
                    }

                    // Use same SharedPreferences as attendance/schedule fragments
                    String savedId = requireContext()
                            .getSharedPreferences(PREFS, 0)
                            .getString(KEY_SELECTED_CHILD_ID, "");
                    String savedName = requireContext()
                            .getSharedPreferences(PREFS, 0)
                            .getString(KEY_SELECTED_CHILD_NAME, "");

                    if (!savedId.isEmpty() && childNameById.containsKey(savedId)) {
                        selectedChildId = savedId;
                        selectedChildName = savedName;
                    } else if (!childIds.isEmpty()) {
                        selectedChildId = childIds.get(0);
                        selectedChildName = childNames.get(0);
                        saveSelectedChild();
                    }

                    updateChildDisplay();

                    if (childIds.isEmpty()) {
                        showEmptyState("No students linked");
                    } else {
                        loadEventsForSelectedChild();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load student data", e);
                    showEmptyState("Failed to load student data");
                });
    }

    private void updateChildDisplay() {
        if (hideChildSelector) {
            // In student mode, just show the child's name without the dropdown indicator
            tvSelectedChild.setText("📅 " + selectedChildName);
            return;
        }

        if (childIds.size() > 1) {
            tvSelectedChild.setText("📅 " + selectedChildName + " ▾ (tap to change)");
        } else {
            tvSelectedChild.setText("📅 " + selectedChildName);
        }
    }

    private void showChildPicker() {
        if (childNames.size() <= 1) return;

        String[] items = childNames.toArray(new String[0]);
        int checked = Math.max(childIds.indexOf(selectedChildId), 0);

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Student")
                .setSingleChoiceItems(items, checked, (dialog, which) -> {
                    selectedChildId = childIds.get(which);
                    selectedChildName = childNames.get(which);
                })
                .setPositiveButton("Show Events", (dialog, which) -> {
                    saveSelectedChild();
                    updateChildDisplay();
                    loadEventsForSelectedChild();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveSelectedChild() {
        requireContext()
                .getSharedPreferences(PREFS, 0)
                .edit()
                .putString(KEY_SELECTED_CHILD_ID, selectedChildId)
                .putString(KEY_SELECTED_CHILD_NAME, selectedChildName)
                .apply();
    }

    private void loadEventsForSelectedChild() {
        if (selectedChildId.isEmpty()) {
            showEmptyState("No student selected");
            return;
        }

        String childClassId = childClassIdByName.get(selectedChildId);

        if (childClassId == null || childClassId.isEmpty()) {
            showEmptyState(selectedChildName + " is not assigned to a group");
            return;
        }

        db.collection("events")
                .get()
                .addOnSuccessListener(snap -> {
                    events.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String eventClassId = doc.getString("classId");

                        boolean showEvent = false;

                        if (eventClassId == null || eventClassId.isEmpty()) {
                            showEvent = true; // General event for everyone
                        } else if (eventClassId.equals(childClassId)) {
                            showEvent = true; // Event for this child's specific class
                        }

                        if (showEvent) {
                            CalendarEvent event = new CalendarEvent();
                            event.setId(doc.getId());
                            event.setTitle(doc.getString("title"));
                            event.setDescription(doc.getString("description"));
                            event.setType(doc.getString("type"));
                            event.setStartAt(doc.getString("startAt"));
                            event.setEndAt(doc.getString("endAt"));
                            event.setClassId(eventClassId);
                            event.setClassName(doc.getString("className"));

                            String startAt = doc.getString("startAt");
                            String endAt = doc.getString("endAt");
                            try {
                                if (startAt != null && !startAt.isEmpty()) {
                                    event.setStartDate(sdf.parse(startAt));
                                }
                                if (endAt != null && !endAt.isEmpty()) {
                                    event.setEndDate(sdf.parse(endAt));
                                }
                            } catch (ParseException e) {
                                Log.w(TAG, "Date parse error for event: " + doc.getString("title"), e);
                            }

                            events.add(event);
                        }
                    }

                    if (events.isEmpty()) {
                        showEmptyState("No upcoming events for " + selectedChildName);
                    } else {
                        adapter.setEvents(events);
                        emptyText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load events", e);
                    showEmptyState("Failed to load events");
                });
    }

    private void showEmptyState(String message) {
        emptyText.setText(message);
        emptyText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }
}