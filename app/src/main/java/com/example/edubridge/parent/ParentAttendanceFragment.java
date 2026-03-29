package com.example.edubridge.parent;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ParentAttendanceFragment extends Fragment {

    private static final String PREFS = "attendance_prefs";
    private static final String KEY_SELECTED_CHILD_ID = "selected_child_id";
    private static final String KEY_SELECTED_CHILD_NAME = "selected_child_name";

    private final ArrayList<AttendanceRowItem> allItems = new ArrayList<>();
    private final ArrayList<AttendanceRowItem> filteredItems = new ArrayList<>();
    private final ArrayList<String> childNames = new ArrayList<>();
    private final ArrayList<String> childIds = new ArrayList<>();
    private final Map<String, String> childNameById = new HashMap<>();

    private AttendanceRecordsAdapter adapter;
    private TextView tvTotalDays, tvPresentDays, tvAbsentDays, tvAttendancePercent, tvSelectedChild, emptyText;

    private String selectedChildId = "";
    private String selectedChildName = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_parent_attendance, container, false);

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
        adapter = new AttendanceRecordsAdapter(filteredItems);
        rv.setAdapter(adapter);

        v.findViewById(R.id.btnFilter).setOnClickListener(view -> showChildPicker());
        v.findViewById(R.id.btnExportPdf).setOnClickListener(view -> exportPdf());

        loadLinkedStudents();

        return v;
    }

    private void loadLinkedStudents() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "No user signed in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("students")
                .get()
                .addOnSuccessListener(snap -> {
                    childNames.clear();
                    childIds.clear();
                    childNameById.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String studentId = value(doc.getString("studentId"));
                        String name = value(doc.getString("name"));

                        if (!studentId.isEmpty()) {
                            childIds.add(studentId);
                            childNames.add(name);
                            childNameById.put(studentId, name);
                        }
                    }

                    String savedId = requireContext().getSharedPreferences(PREFS, 0).getString(KEY_SELECTED_CHILD_ID, "");
                    String savedName = requireContext().getSharedPreferences(PREFS, 0).getString(KEY_SELECTED_CHILD_NAME, "");

                    if (!savedId.isEmpty() && childNameById.containsKey(savedId)) {
                        selectedChildId = savedId;
                        selectedChildName = savedName;
                    } else if (!childIds.isEmpty()) {
                        selectedChildId = childIds.get(0);
                        selectedChildName = childNames.get(0);
                        saveSelectedChild();
                    }

                    tvSelectedChild.setText("Child: " + selectedChildName);
                    loadAttendance();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Failed to load linked children.", Toast.LENGTH_SHORT).show()
                );
    }

    private void showChildPicker() {
        if (childNames.isEmpty()) return;

        String[] items = childNames.toArray(new String[0]);
        int checked = Math.max(childIds.indexOf(selectedChildId), 0);

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Child")
                .setSingleChoiceItems(items, checked, (dialog, which) -> {
                    selectedChildId = childIds.get(which);
                    selectedChildName = childNames.get(which);
                })
                .setPositiveButton("Apply", (dialog, which) -> {
                    saveSelectedChild();
                    tvSelectedChild.setText("Child: " + selectedChildName);
                    loadAttendance();
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

    private void loadAttendance() {
        allItems.clear();
        filteredItems.clear();
        adapter.notifyDataSetChanged();
        
        FirebaseFirestore.getInstance()
                .collection("attendance")
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(snap -> {
                    allItems.clear();

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
                                allItems.add(new AttendanceRowItem(
                                        date,
                                        subject,
                                        capitalize(status)
                                ));
                            }
                        }
                    }

                    applySelectedChild();
                })
                .addOnFailureListener(e -> {
                    allItems.clear();
                    applySelectedChild();
                    Toast.makeText(requireContext(), "Failed to load attendance.", Toast.LENGTH_SHORT).show();
                });
    }

    private void applySelectedChild() {
        filteredItems.clear();
        filteredItems.addAll(allItems);
        updateSummary();
        emptyText.setVisibility(filteredItems.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

    private void updateSummary() {
        int total = filteredItems.size();
        int present = 0;
        int absent = 0;

        for (AttendanceRowItem item : filteredItems) {
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

    private void exportPdf() {
        if (filteredItems.isEmpty()) {
            Toast.makeText(requireContext(), "No attendance data to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        Paint paint = new Paint();
        Paint bold = new Paint();
        bold.setFakeBoldText(true);

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(1200, 1600, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        int y = 60;
        page.getCanvas().drawText("Attendance Report - " + selectedChildName, 50, y, bold);
        y += 40;

        page.getCanvas().drawText("Total Days: " + tvTotalDays.getText(), 50, y, paint);
        y += 25;
        page.getCanvas().drawText("Days Present: " + tvPresentDays.getText(), 50, y, paint);
        y += 25;
        page.getCanvas().drawText("Days Absent: " + tvAbsentDays.getText(), 50, y, paint);
        y += 25;
        page.getCanvas().drawText("Attendance: " + tvAttendancePercent.getText(), 50, y, paint);
        y += 40;

        page.getCanvas().drawText("Date", 50, y, bold);
        page.getCanvas().drawText("Subject", 400, y, bold);
        page.getCanvas().drawText("Status", 800, y, bold);
        y += 25;

        for (AttendanceRowItem item : filteredItems) {
            page.getCanvas().drawText(item.date, 50, y, paint);
            page.getCanvas().drawText(item.subject, 400, y, paint);
            page.getCanvas().drawText(item.status, 800, y, paint);
            y += 30;
            if (y > 1500) break;
        }

        document.finishPage(page);

        try {
            String fileName = "attendance_report_" + selectedChildName + ".pdf";
            OutputStream outputStream;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new Exception();
                outputStream = requireContext().getContentResolver().openOutputStream(uri);
            } else {
                throw new Exception();
            }

            if (outputStream == null) throw new Exception();
            document.writeTo(outputStream);
            outputStream.close();
            document.close();

            Toast.makeText(requireContext(), "PDF exported to Downloads.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            document.close();
            Toast.makeText(requireContext(), "Failed to export PDF.", Toast.LENGTH_SHORT).show();
        }
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