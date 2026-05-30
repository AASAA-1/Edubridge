package com.example.edubridge.teacher;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TeacherCalendarFragment extends Fragment {

    private static final String TAG = "TeacherCalendar";
    private RecyclerView recyclerView;
    private TextView emptyText;
    private CalendarAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserId;
    private final List<CalendarEvent> events = new ArrayList<>();
    private final Set<String> classIds = new HashSet<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_teacher_calendar, container, false);
        TextSizeHelper.applyScaleRecursively(v);

        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Hide the student selector since teachers don't need it
        TextView tvSelectedChild = v.findViewById(R.id.tvSelectedChild);
        if (tvSelectedChild != null) {
            tvSelectedChild.setVisibility(View.GONE);
        }

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        emptyText = v.findViewById(R.id.emptyText);
        recyclerView = v.findViewById(R.id.rvCalendar);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CalendarAdapter();
        recyclerView.setAdapter(adapter);

        loadTeacherClasses();

        return v;
    }

    /**
     * Step 1: Get all class IDs assigned to this teacher
     */
    private void loadTeacherClasses() {
        db.collection("classes")
                .whereEqualTo("teacherId", currentUserId)
                .get()
                .addOnSuccessListener(snap -> {
                    classIds.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        classIds.add(doc.getId());
                    }

                    Log.d(TAG, "Teacher assigned to " + classIds.size() + " classes: " + classIds);

                    if (classIds.isEmpty()) {
                        showEmptyState("You are not assigned to any groups");
                    } else {
                        loadEvents();
                    }
                })
                .addOnFailureListener(e ->
                        showEmptyState("Failed to load groups")
                );
    }

    /**
     * Step 2: Load events - only for teacher's classes + general events
     */
    private void loadEvents() {
        db.collection("events")
                .get()
                .addOnSuccessListener(snap -> {
                    events.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    Log.d(TAG, "Total events in database: " + snap.size());

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String title = doc.getString("title");
                        String eventClassId = doc.getString("classId");
                        String className = doc.getString("className");

                        // Show event if:
                        // 1. It's a general event (empty/null classId means "All Classes")
                        // 2. The event's classId matches one of the teacher's class IDs
                        boolean showEvent = false;

                        if (eventClassId == null || eventClassId.isEmpty()) {
                            // General event - show to everyone
                            showEvent = true;
                        } else if (classIds.contains(eventClassId)) {
                            // Event specifically for this teacher's class
                            showEvent = true;
                        }

                        if (showEvent) {
                            CalendarEvent event = new CalendarEvent();
                            event.setId(doc.getId());
                            event.setTitle(title);
                            event.setDescription(doc.getString("description"));
                            event.setType(doc.getString("type"));
                            event.setStartAt(doc.getString("startAt"));
                            event.setEndAt(doc.getString("endAt"));
                            event.setClassId(eventClassId);
                            event.setClassName(className);

                            // Parse dates
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
                                Log.w(TAG, "Date parse error for event: " + title, e);
                            }

                            events.add(event);
                        }
                    }

                    Log.d(TAG, "Filtered events count: " + events.size());

                    if (events.isEmpty()) {
                        showEmptyState("No events found for your groups");
                    } else {
                        adapter.setEvents(events);
                        emptyText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        showEmptyState("Failed to load events")
                );
    }

    private void showEmptyState(String message) {
        emptyText.setText(message);
        emptyText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }
}