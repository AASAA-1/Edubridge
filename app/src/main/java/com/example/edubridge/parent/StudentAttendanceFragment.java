package com.example.edubridge.parent;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.edubridge.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentAttendanceFragment extends Fragment {

    private static final String PREFS = "attendance_prefs";
    private static final String KEY_SELECTED_CHILD_ID = "selected_child_id";
    private static final String KEY_SELECTED_CHILD_NAME = "selected_child_name";

    private final ArrayList<AttendanceRowItem> items = new ArrayList<>();
    private AttendanceRecordsAdapter adapter;

    private TextView tvTotalDays, tvPresentDays, tvAbsentDays, tvAttendancePercent, tvSelectedChild, emptyText;

    private String selectedChildId = "";
    private String selectedChildName = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_student_attendance, container, false);

        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        tvTotalDays = v.findViewById(R.id.tvTotalDays);
        tvPresentDays = v.findViewById(R.id.tvPresentDays);
        tvAbsentDays = v.findViewById(R.id.tvAbsentDays);
        tvAttendancePercent = v.findViewById(R.id.tvAttendancePercent);
        tvSelectedChild = v.findViewById(R.id.tvSelectedChild);
        emptyText = v.findViewById(R.id.emptyText);

        RecyclerView rv = v.findViewById(R.id.rvAttendance);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AttendanceRecordsAdapter(items);
        rv.setAdapter(adapter);

        loadSelectedChild();

        return v;
    }

    private void loadSelectedChild() {
        selectedChildId = requireContext().getSharedPreferences(PREFS, 0).getString(KEY_SELECTED_CHILD_ID, "");
        selectedChildName = requireContext().getSharedPreferences(PREFS, 0).getString(KEY_SELECTED_CHILD_NAME, "");

        if (selectedChildId.isEmpty()) {
            Toast.makeText(requireContext(), "No selected child from parent mode.", Toast.LENGTH_SHORT).show();
            tvSelectedChild.setText("Child: None");
            return;
        }

        tvSelectedChild.setText("Child: " + selectedChildName);
        loadAttendance();
    }

    private void loadAttendance() {
        FirebaseFirestore.getInstance()
                .collection("attendance")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(snap -> {
                    items.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String date = value(doc.getString("date"));
                        String subject = value(doc.getString("subject"));
                        if (subject.isEmpty()) {
                            subject = value(doc.getString("class"));
                        }

                        List<Map<String, Object>> students = (List<Map<String, Object>>) doc.get("students");
                        if (students == null) continue;

                        for (Map<String, Object> student : students) {
                            String studentId = value(student.get("studentId") == null ? "" : String.valueOf(student.get("studentId")));
                            String status = extractStatus(student);

                            if (studentId.equals(selectedChildId)) {
                                items.add(new AttendanceRowItem(
                                        date,
                                        subject,
                                        capitalize(status)
                                ));
                            }
                        }
                    }

                    updateSummary();
                    emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    items.clear();
                    updateSummary();
                    emptyText.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                });
    }

    private void updateSummary() {
        int total = items.size();
        int present = 0;
        int absent = 0;

        for (AttendanceRowItem item : items) {
            String s = item.status.toLowerCase(Locale.US);
            if (s.equals("present")) present++;
            if (s.equals("absent")) absent++;
        }

        int percentage = total == 0 ? 0 : Math.round((present * 100f) / total);

        tvTotalDays.setText(String.valueOf(total));
        tvPresentDays.setText(String.valueOf(present));
        tvAbsentDays.setText(String.valueOf(absent));
        tvAttendancePercent.setText(percentage + "%");
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        s = s.trim().toLowerCase(Locale.US);
        return s.substring(0, 1).toUpperCase(Locale.US) + s.substring(1);
    }

    private String extractStatus(Map<String, Object> student) {
        Object statusObj = student.get("status");
        if (statusObj != null) {
            return String.valueOf(statusObj);
        }

        Object presentObj = student.get("present");
        if (presentObj instanceof Boolean) {
            return ((Boolean) presentObj) ? "present" : "absent";
        }

        return "";
    }
    private String value(String s) {
        return s == null ? "" : s;
    }
}