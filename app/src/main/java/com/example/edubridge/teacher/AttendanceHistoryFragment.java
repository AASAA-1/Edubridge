package com.example.edubridge.teacher;

import android.os.Bundle;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_attendance_history, container, false);
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
        toolbar.setTitle("Attendance History - " + (className != null ? className : ""));

        historyRecyclerView = view.findViewById(R.id.history_recycler_view);
        emptyText = view.findViewById(R.id.empty_text);

        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        historyRecyclerView.setAdapter(adapter);

        loadAttendanceHistory();
    }

    private void loadAttendanceHistory() {
        db.collection("attendance")
                .whereEqualTo("class", className)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historyList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AttendanceRecord record = new AttendanceRecord();
                        record.setDate(doc.getString("date"));
                        record.setTeacherId(doc.getString("teacherId"));

                        List<Object> students = (List<Object>) doc.get("students");
                        if (students != null) {
                            record.setStudentCount(students.size());

                            // Count present students
                            int present = 0;
                            for (Object obj : students) {
                                if (obj instanceof java.util.Map) {
                                    java.util.Map<String, Object> student = (java.util.Map<String, Object>) obj;
                                    Boolean isPresent = (Boolean) student.get("present");
                                    if (isPresent != null && isPresent) {
                                        present++;
                                    }
                                }
                            }
                            record.setPresentCount(present);
                        }

                        historyList.add(record);
                    }

                    if (historyList.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        historyRecyclerView.setVisibility(View.GONE);
                    } else {
                        emptyText.setVisibility(View.GONE);
                        historyRecyclerView.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    emptyText.setVisibility(View.VISIBLE);
                    historyRecyclerView.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error loading history: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    public static class AttendanceRecord {
        private String date;
        private String teacherId;
        private int studentCount;
        private int presentCount;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getTeacherId() { return teacherId; }
        public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
        public int getStudentCount() { return studentCount; }
        public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
        public int getPresentCount() { return presentCount; }
        public void setPresentCount(int presentCount) { this.presentCount = presentCount; }
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
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AttendanceRecord record = records.get(position);
            holder.tvDate.setText(record.getDate());
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