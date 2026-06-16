package com.example.edubridge.teacher;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TeacherCalendarFragment extends Fragment {

    private static final String TAG = "TeacherCalendar";
    private RecyclerView recyclerView;
    private TextView emptyText;
    private CalendarAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserId;
    private final List<CalendarEvent> events = new ArrayList<>();
    private final List<String> classIds = new ArrayList<>();
    private final List<String> classNames = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_teacher_calendar, container, false);
        TextSizeHelper.applyScaleRecursively(v);

        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack());

        toolbar.getMenu().add(0, 1, 0, "+")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                showAddEventDialog();
                return true;
            }
            return false;
        });

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

    private void loadTeacherClasses() {
        db.collection("classes")
                .whereEqualTo("teacherId", currentUserId)
                .get()
                .addOnSuccessListener(snap -> {
                    classIds.clear();
                    classNames.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        classIds.add(doc.getId());
                        String name = doc.getString("name");
                        classNames.add(name != null ? name : doc.getId());
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

    private void loadEvents() {
        db.collection("events")
                .get()
                .addOnSuccessListener(snap -> {
                    events.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

                    Log.d(TAG, "Total events in database: " + snap.size());

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String title = doc.getString("title");
                        String eventClassId = doc.getString("classId");
                        String className = doc.getString("className");

                        boolean showEvent = false;

                        if (eventClassId == null || eventClassId.isEmpty()) {
                            showEvent = true;
                        } else if (classIds.contains(eventClassId)) {
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

    private void showAddEventDialog() {
        if (classIds.isEmpty()) {
            Toast.makeText(requireContext(), "No classes loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = dpToPx(16);
        layout.setPadding(p, p, p, 0);

        EditText etTitle = new EditText(requireContext());
        etTitle.setHint("Title");
        layout.addView(etTitle);

        EditText etDesc = new EditText(requireContext());
        etDesc.setHint("Description (optional)");
        layout.addView(etDesc);

        Spinner typeSpinner = new Spinner(requireContext());
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Homework", "Test"});
        typeSpinner.setAdapter(typeAdapter);
        layout.addView(typeSpinner);

        final String[] selectedDate = {""};
        final Calendar cal = Calendar.getInstance();
        Button btnDate = new Button(requireContext());
        btnDate.setText("Pick Date");
        btnDate.setOnClickListener(bv -> new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    selectedDate[0] = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                    btnDate.setText(selectedDate[0]);
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show());
        layout.addView(btnDate);

        Spinner classSpinner = new Spinner(requireContext());
        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                classNames.toArray(new String[0]));
        classSpinner.setAdapter(classAdapter);
        layout.addView(classSpinner);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Add Event")
                .setView(layout)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(bv -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            String type = typeSpinner.getSelectedItem().toString();
            String date = selectedDate[0];
            int idx = classSpinner.getSelectedItemPosition();

            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Title is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (date.isEmpty()) {
                Toast.makeText(requireContext(), "Please pick a date", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> event = new HashMap<>();
            event.put("title", title);
            event.put("description", desc);
            event.put("startAt", date);
            event.put("endAt", date);
            event.put("type", type);
            event.put("classId", classIds.get(idx));
            event.put("className", classNames.get(idx));

            db.collection("events").add(event)
                    .addOnSuccessListener(ref -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Event added", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadEvents();
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        Toast.makeText(requireContext(), "Failed to save", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getResources().getDisplayMetrics());
    }

    private void showEmptyState(String message) {
        emptyText.setText(message);
        emptyText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }
}
