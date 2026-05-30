package com.example.edubridge.parent;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.edubridge.R;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleFragment extends Fragment {

    private static final String PREFS = "attendance_prefs";
    private static final String KEY_SELECTED_CHILD_ID = "selected_child_id";
    private static final String KEY_SELECTED_CHILD_NAME = "selected_child_name";

    private TableLayout table;
    private TextView emptyText;
    private TextView tvSelectedChild;
    private ScrollView scrollView;
    private boolean hideChildSelector = false;

    private final List<String> childNames = new ArrayList<>();
    private final List<String> childIds = new ArrayList<>();
    private final Map<String, String> childClassIdByName = new HashMap<>();

    private String selectedChildId = "";
    private String selectedChildName = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_schedule, container, false);
        TextSizeHelper.applyScaleRecursively(v);

        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        if (getArguments() != null) {
            hideChildSelector = getArguments().getBoolean("hideChildSelector", false);
        }

        table = v.findViewById(R.id.table);
        emptyText = v.findViewById(R.id.emptyText);
        tvSelectedChild = v.findViewById(R.id.tvSelectedChild);
        scrollView = v.findViewById(R.id.scrollView);

        if (hideChildSelector) {
            tvSelectedChild.setVisibility(View.GONE);
        } else {
            tvSelectedChild.setOnClickListener(view -> showChildPicker());
        }

        loadStudents();

        return v;
    }

    private void loadStudents() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            showEmptyState("User not logged in");
            return;
        }

        String currentUserId = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(currentUserId)
                .collection("students")
                .get()
                .addOnSuccessListener(studentQuery -> {
                    if (studentQuery.isEmpty()) {
                        showEmptyState("No linked students found");
                        return;
                    }

                    childNames.clear();
                    childIds.clear();
                    childClassIdByName.clear();

                    for (DocumentSnapshot doc : studentQuery.getDocuments()) {
                        String studentId = doc.getString("studentId");
                        String name = doc.getString("name");
                        String classId = doc.getString("classId");

                        if (studentId != null && !studentId.isEmpty()) {
                            childIds.add(studentId);
                            childNames.add(name != null ? name : "Unknown");
                            if (classId != null) {
                                childClassIdByName.put(studentId, classId);
                            }
                        }
                    }

                    String savedId = requireContext()
                            .getSharedPreferences(PREFS, 0)
                            .getString(KEY_SELECTED_CHILD_ID, "");
                    String savedName = requireContext()
                            .getSharedPreferences(PREFS, 0)
                            .getString(KEY_SELECTED_CHILD_NAME, "");

                    if (!savedId.isEmpty() && childClassIdByName.containsKey(savedId)) {
                        selectedChildId = savedId;
                        selectedChildName = savedName;
                    } else if (!childIds.isEmpty()) {
                        selectedChildId = childIds.get(0);
                        selectedChildName = childNames.get(0);
                        saveSelectedChild();
                    }

                    updateChildDisplay();
                    loadSchedule();
                })
                .addOnFailureListener(e ->
                        showEmptyState("Failed to load student data: " + e.getMessage())
                );
    }

    private void updateChildDisplay() {
        if (hideChildSelector) {
            tvSelectedChild.setText("📅 " + selectedChildName);
            return;
        }

        if (childIds.size() > 1) {
            tvSelectedChild.setText("📅 " + selectedChildName + " ▾");
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
                .setPositiveButton("Show Schedule", (dialog, which) -> {
                    saveSelectedChild();
                    updateChildDisplay();
                    loadSchedule();
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

    private void loadSchedule() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (selectedChildId.isEmpty()) {
            showEmptyState("No student selected");
            return;
        }

        String classId = childClassIdByName.get(selectedChildId);

        if (classId == null || classId.isEmpty()) {
            showEmptyState(selectedChildName + " is not assigned to a class");
            return;
        }

        db.collection("classes").document(classId).get()
                .addOnSuccessListener(classDoc -> {
                    if (!classDoc.exists()) {
                        showEmptyState("Class not found");
                        return;
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Object> scheduleData =
                            (Map<String, Object>) classDoc.get("schedule");

                    if (scheduleData == null || scheduleData.isEmpty()) {
                        showEmptyState("No schedule available for " + selectedChildName + "'s class");
                        return;
                    }

                    db.collection("curriculum").get()
                            .addOnSuccessListener(curriculumQuery -> {
                                Map<String, String> curriculumNames = new HashMap<>();
                                for (QueryDocumentSnapshot curriculumDoc : curriculumQuery) {
                                    curriculumNames.put(
                                            curriculumDoc.getId(),
                                            curriculumDoc.getString("name")
                                    );
                                }
                                buildTable(scheduleData, curriculumNames);
                            })
                            .addOnFailureListener(e ->
                                    showEmptyState("Failed to load curriculum data")
                            );
                })
                .addOnFailureListener(e ->
                        showEmptyState("Failed to load schedule")
                );
    }

    private void showEmptyState(String message) {
        if (isAdded()) {
            emptyText.setText(message);
            emptyText.setVisibility(View.VISIBLE);
            scrollView.setVisibility(View.GONE);
        }
    }

    private void buildTable(Map<String, Object> scheduleData, Map<String, String> curriculumNames) {
        table.removeAllViews();

        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday"};
        int numPeriods = 7;

        addHeaderRow(numPeriods);

        for (String day : days) {
            TableRow row = new TableRow(requireContext());
            row.addView(cell(day, true));

            for (int period = 1; period <= numPeriods; period++) {
                String key = day + "_" + period;
                String curriculumId = (String) scheduleData.get(key);
                String displayText;

                if (curriculumId != null && !curriculumId.isEmpty()) {
                    String curriculumName = curriculumNames.get(curriculumId);
                    displayText = curriculumName != null ? curriculumName : "Unknown";
                } else {
                    displayText = "Free";
                }

                row.addView(cell(displayText, false));
            }

            table.addView(row);
        }

        emptyText.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
    }

    private void addHeaderRow(int periods) {
        TableRow header = new TableRow(requireContext());
        header.addView(headerCell("Day"));

        for (int p = 1; p <= periods; p++) {
            header.addView(headerCell("P" + p));
        }

        table.addView(header);
    }

    private TextView headerCell(String t) {
        TextView tv = new TextView(requireContext());
        tv.setText(t);
        tv.setPadding(16, 14, 16, 14);
        tv.setTextSize(12f);
        tv.setTextColor(0xFF111111);
        tv.setBackgroundColor(0xFF9FD0E6);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        return tv;
    }

    private TextView cell(String t, boolean day) {
        TextView tv = new TextView(requireContext());
        tv.setText(t);
        tv.setPadding(16, 14, 16, 14);
        tv.setTextSize(12f);
        tv.setTextColor(0xFF111111);
        tv.setBackgroundColor(day ? 0xFFE6F3FA : 0xFFFFFFFF);
        tv.setMinEms(6);
        return tv;
    }
}