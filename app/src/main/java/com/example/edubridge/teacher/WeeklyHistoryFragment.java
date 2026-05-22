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
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class WeeklyHistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private TextView tvStudentName, emptyText;
    private FirebaseFirestore db;
    private String studentId;
    private String studentName;
    private WeeklyHistoryAdapter adapter;
    private final List<StudentProgressFragment.WeeklyReport> reports = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_weekly_history, container, false);
        TextSizeHelper.applyScaleRecursively(v);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            studentId = getArguments().getString("studentId");
            studentName = getArguments().getString("studentName");
        }

        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack());
        toolbar.setTitle("Weekly History - " + (studentName != null ? studentName : ""));

        tvStudentName = v.findViewById(R.id.tv_student_name);
        rvHistory = v.findViewById(R.id.rv_history);
        emptyText = v.findViewById(R.id.empty_text);

        if (studentName != null) {
            tvStudentName.setText("Student: " + studentName);
        }

        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new WeeklyHistoryAdapter(reports);
        rvHistory.setAdapter(adapter);

        loadHistory();

        return v;
    }

    private void loadHistory() {
        if (studentId == null) return;

        // FIX: Removed orderBy to avoid composite index requirement
        // Will sort manually after fetching
        db.collection("weeklyProgress")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(snap -> {
                    reports.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        StudentProgressFragment.WeeklyReport report = new StudentProgressFragment.WeeklyReport();
                        report.setWeekLabel(doc.getString("weekLabel"));
                        report.setWeekKey(doc.getString("weekKey"));
                        report.setAttendance(doc.getLong("attendance") != null ? doc.getLong("attendance").intValue() : 0);
                        report.setAcademic(doc.getLong("academic") != null ? doc.getLong("academic").intValue() : 0);
                        report.setBehavior(doc.getLong("behavior") != null ? doc.getLong("behavior").intValue() : 0);
                        report.setParticipation(doc.getLong("participation") != null ? doc.getLong("participation").intValue() : 0);
                        report.setNotes(doc.getString("notes"));
                        reports.add(report);
                    }

                    // Sort by weekKey descending (newest first)
                    Collections.sort(reports, new Comparator<StudentProgressFragment.WeeklyReport>() {
                        @Override
                        public int compare(StudentProgressFragment.WeeklyReport r1, StudentProgressFragment.WeeklyReport r2) {
                            if (r1.getWeekKey() == null && r2.getWeekKey() == null) return 0;
                            if (r1.getWeekKey() == null) return 1;
                            if (r2.getWeekKey() == null) return -1;
                            return r2.getWeekKey().compareTo(r1.getWeekKey()); // Descending
                        }
                    });

                    if (reports.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        rvHistory.setVisibility(View.GONE);
                    } else {
                        emptyText.setVisibility(View.GONE);
                        rvHistory.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    emptyText.setVisibility(View.VISIBLE);
                    rvHistory.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to load history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private class WeeklyHistoryAdapter extends RecyclerView.Adapter<WeeklyHistoryAdapter.VH> {
        private final List<StudentProgressFragment.WeeklyReport> reports;

        WeeklyHistoryAdapter(List<StudentProgressFragment.WeeklyReport> reports) {
            this.reports = reports;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_weekly_report_summary, parent, false);
            TextSizeHelper.applyScaleRecursively(v);

            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            StudentProgressFragment.WeeklyReport r = reports.get(position);
            h.tvWeek.setText(r.getWeekLabel() != null ? r.getWeekLabel() : "Unknown week");
            h.tvSummary.setText(String.format(Locale.getDefault(),
                    "Attendance: %d%% | Academic: %d%% | Behavior: %d%% | Participation: %d%%",
                    r.getAttendance(), r.getAcademic(), r.getBehavior(), r.getParticipation()));
            h.tvNotes.setText(r.getNotes() != null && !r.getNotes().isEmpty() ? r.getNotes() : "No notes");
        }

        @Override
        public int getItemCount() {
            return reports.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvWeek, tvSummary, tvNotes;

            VH(@NonNull View itemView) {
                super(itemView);
                tvWeek = itemView.findViewById(R.id.tv_week);
                tvSummary = itemView.findViewById(R.id.tv_summary);
                tvNotes = itemView.findViewById(R.id.tv_notes);
            }
        }
    }
}