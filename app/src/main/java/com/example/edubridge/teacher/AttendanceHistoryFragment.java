package com.example.edubridge.teacher;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceHistoryFragment extends Fragment {

    private RecyclerView historyRecyclerView;
    private TextView emptyText;
    private HistoryAdapter adapter;
    private List<AttendanceRecord> historyList;
    private FirebaseFirestore db;
    private String className;

    private static final String TAG = "AttendanceHistory";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_attendance_history, container, false);
        TextSizeHelper.applyScaleRecursively(v);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            className = getArguments().getString("class");
        }

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
        toolbar.setTitle(getString(R.string.attendance_history_title, className != null ? className : ""));

        historyRecyclerView = view.findViewById(R.id.history_recycler_view);
        emptyText = view.findViewById(R.id.empty_text);

        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        historyRecyclerView.setAdapter(adapter);

        if (className != null && !className.isEmpty()) {
            loadAttendanceHistory();
        } else {
            showEmptyState("No class selected");
        }
    }

    private void loadAttendanceHistory() {
        Log.d(TAG, "Loading attendance history for class: " + className);

        // Show loading state (optional)
        emptyText.setVisibility(View.GONE);
        historyRecyclerView.setVisibility(View.VISIBLE);

        // REMOVED orderBy to avoid composite index requirement
        // Will sort manually after fetching
        db.collection("attendance")
                .whereEqualTo("class", className)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historyList.clear();

                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " attendance records");

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AttendanceRecord record = new AttendanceRecord();
                        record.setDate(doc.getString("date"));
                        record.setTeacherId(doc.getString("teacherId"));

                        // Get timestamp for sorting
                        Date timestamp = doc.getDate("timestamp");
                        record.setTimestamp(timestamp);

                        List<Object> students = (List<Object>) doc.get("students");
                        if (students != null) {
                            record.setStudentCount(students.size());

                            // Count present students
                            int present = 0;
                            for (Object obj : students) {
                                if (obj instanceof java.util.Map) {
                                    java.util.Map<String, Object> student = (java.util.Map<String, Object>) obj;

                                    // Check status field first (new format)
                                    String status = (String) student.get("status");
                                    if (status != null) {
                                        if ("present".equalsIgnoreCase(status)) {
                                            present++;
                                        }
                                    } else {
                                        // Fall back to present boolean (old format)
                                        Boolean isPresent = (Boolean) student.get("present");
                                        if (isPresent != null && isPresent) {
                                            present++;
                                        }
                                    }
                                }
                            }
                            record.setPresentCount(present);
                        }

                        historyList.add(record);
                    }

                    // Manual sorting by timestamp (newest first)
                    Collections.sort(historyList, new Comparator<AttendanceRecord>() {
                        @Override
                        public int compare(AttendanceRecord r1, AttendanceRecord r2) {
                            if (r1.getTimestamp() == null && r2.getTimestamp() == null) return 0;
                            if (r1.getTimestamp() == null) return 1;
                            if (r2.getTimestamp() == null) return -1;
                            return r2.getTimestamp().compareTo(r1.getTimestamp()); // Descending
                        }
                    });

                    if (historyList.isEmpty()) {
                        showEmptyState("No attendance records found for " + className);
                    } else {
                        emptyText.setVisibility(View.GONE);
                        historyRecyclerView.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading attendance history: " + e.getMessage());

                    // If the error is about missing index, try without filter
                    if (e.getMessage() != null && e.getMessage().contains("index")) {
                        loadAttendanceHistoryWithoutFilter();
                    } else {
                        showEmptyState("Error loading history: " + e.getMessage());
                    }
                });
    }

    /**
     * Fallback method: load all attendance and filter manually
     */
    private void loadAttendanceHistoryWithoutFilter() {
        Log.d(TAG, "Trying to load all attendance records and filter manually");

        db.collection("attendance")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historyList.clear();

                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " total attendance records");

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String recordClass = doc.getString("class");

                        // Only include records for this class
                        if (className.equals(recordClass)) {
                            AttendanceRecord record = new AttendanceRecord();
                            record.setDate(doc.getString("date"));
                            record.setTeacherId(doc.getString("teacherId"));

                            Date timestamp = doc.getDate("timestamp");
                            record.setTimestamp(timestamp);

                            List<Object> students = (List<Object>) doc.get("students");
                            if (students != null) {
                                record.setStudentCount(students.size());

                                int present = 0;
                                for (Object obj : students) {
                                    if (obj instanceof java.util.Map) {
                                        java.util.Map<String, Object> student = (java.util.Map<String, Object>) obj;

                                        String status = (String) student.get("status");
                                        if (status != null) {
                                            if ("present".equalsIgnoreCase(status)) {
                                                present++;
                                            }
                                        } else {
                                            Boolean isPresent = (Boolean) student.get("present");
                                            if (isPresent != null && isPresent) {
                                                present++;
                                            }
                                        }
                                    }
                                }
                                record.setPresentCount(present);
                            }

                            historyList.add(record);
                        }
                    }

                    // Sort by timestamp (newest first)
                    Collections.sort(historyList, new Comparator<AttendanceRecord>() {
                        @Override
                        public int compare(AttendanceRecord r1, AttendanceRecord r2) {
                            if (r1.getTimestamp() == null && r2.getTimestamp() == null) return 0;
                            if (r1.getTimestamp() == null) return 1;
                            if (r2.getTimestamp() == null) return -1;
                            return r2.getTimestamp().compareTo(r1.getTimestamp());
                        }
                    });

                    if (historyList.isEmpty()) {
                        showEmptyState("No attendance records found for " + className);
                    } else {
                        emptyText.setVisibility(View.GONE);
                        historyRecyclerView.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error in fallback query: " + e.getMessage());
                    showEmptyState("Error loading history: " + e.getMessage());
                });
    }

    private void showEmptyState(String message) {
        emptyText.setText(message);
        emptyText.setVisibility(View.VISIBLE);
        historyRecyclerView.setVisibility(View.GONE);
    }

    public static class AttendanceRecord {
        private String date;
        private String teacherId;
        private int studentCount;
        private int presentCount;
        private Date timestamp;  // Added for sorting

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getTeacherId() { return teacherId; }
        public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
        public int getStudentCount() { return studentCount; }
        public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
        public int getPresentCount() { return presentCount; }
        public void setPresentCount(int presentCount) { this.presentCount = presentCount; }
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<AttendanceRecord> records;

        public HistoryAdapter(List<AttendanceRecord> records) {
            this.records = records;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_attendance_history, parent, false);
            TextSizeHelper.applyScaleRecursively(view);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AttendanceRecord record = records.get(position);
            holder.tvDate.setText(record.getDate() != null ? record.getDate() : "Unknown date");
            holder.tvStats.setText(String.format(Locale.getDefault(),
                    "Present: %d/%d (%.0f%%)",
                    record.getPresentCount(),
                    record.getStudentCount(),
                    record.getStudentCount() > 0 ?
                            (record.getPresentCount() * 100.0 / record.getStudentCount()) : 0));
        }

        @Override
        public int getItemCount() {
            return records.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvStats;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvStats = itemView.findViewById(R.id.tv_stats);
            }
        }
    }
}