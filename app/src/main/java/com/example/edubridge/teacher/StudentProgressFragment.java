package com.example.edubridge.teacher;

import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.example.edubridge.shared.BigModeHelper;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentProgressFragment extends Fragment {

    // ── Views ──────────────────────────────────────────────────────────────────
    private Spinner spinnerClass, spinnerStudent, spinnerWeek;
    private TextView tvClassLabel, tvStudentLabel, tvMetricsLabel, tvNotesLabel, tvWeekLabel;
    private TextView tvAttendanceLabel, tvAcademicLabel, tvBehaviorLabel, tvParticipationLabel;
    private TextView tvClassStatus;
    private ProgressBar pbAttendance, pbAcademic, pbBehavior, pbParticipation;
    private EditText etAttendanceVal, etAcademicVal, etBehaviorVal, etParticipationVal;
    private EditText etNotes, etObservations;
    private MaterialButton btnSave, btnExportPdf, btnViewHistory;
    private RecyclerView rvPreviousReports;

    // ── Firebase ───────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private String currentTeacherId;

    // ── Spinner data ──────────────────────────────────────────────────────────
    private final List<String> classNames = new ArrayList<>();
    private final List<String> classIds = new ArrayList<>();
    private final List<String> studentNames = new ArrayList<>();
    private final List<String> studentIds = new ArrayList<>();
    private final List<DocumentReference> studentDocRefs = new ArrayList<>();

    // Week selection
    private final List<String> weekOptions = new ArrayList<>();
    private final List<String> weekKeys = new ArrayList<>();

    // ── Selected state ────────────────────────────────────────────────────────
    private String selectedClassId = null;
    private String selectedClassName = null;
    private String selectedStudentId = null;
    private String selectedStudentName = null;
    private DocumentReference selectedStudentDocRef = null;
    private String selectedWeekKey = null;

    // ── Cached metric values ──────────────────────────────────────────────────
    private int currentAttendancePct = 0;
    private int currentAcademicPct = 0;
    private int currentBehaviorPct = 0;
    private int currentParticipationPct = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_student_progress, container, false);
        TextSizeHelper.applyScaleRecursively(v);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        currentTeacherId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        bindViews(view);
        setupToolbar(view);
        applyBigMode();
        setupMetricWatchers();
        setupWeekSpinner();
        setupStudentSpinnerListener();
        setupClassSpinnerListener();
        setupButtonListeners();
        setupPreviousReportsRecyclerView(view);

        spinnerClass.setEnabled(false);
        spinnerStudent.setEnabled(false);
        spinnerWeek.setEnabled(false);

        fetchAssignedClasses();
    }

    private void bindViews(View view) {
        spinnerClass = view.findViewById(R.id.spinner_class);
        spinnerStudent = view.findViewById(R.id.spinner_student);
        spinnerWeek = view.findViewById(R.id.spinner_week);
        tvClassLabel = view.findViewById(R.id.tv_class_label);
        tvStudentLabel = view.findViewById(R.id.tv_student_label);
        tvWeekLabel = view.findViewById(R.id.tv_week_label);
        tvMetricsLabel = view.findViewById(R.id.tv_metrics_label);
        tvNotesLabel = view.findViewById(R.id.tv_notes_label);
        tvAttendanceLabel = view.findViewById(R.id.tv_attendance_label);
        tvAcademicLabel = view.findViewById(R.id.tv_academic_label);
        tvBehaviorLabel = view.findViewById(R.id.tv_behavior_label);
        tvParticipationLabel = view.findViewById(R.id.tv_participation_label);
        tvClassStatus = view.findViewById(R.id.tv_class_status);
        pbAttendance = view.findViewById(R.id.pb_attendance);
        pbAcademic = view.findViewById(R.id.pb_academic);
        pbBehavior = view.findViewById(R.id.pb_behavior);
        pbParticipation = view.findViewById(R.id.pb_participation);
        etAttendanceVal = view.findViewById(R.id.et_attendance_val);
        etAcademicVal = view.findViewById(R.id.et_academic_val);
        etBehaviorVal = view.findViewById(R.id.et_behavior_val);
        etParticipationVal = view.findViewById(R.id.et_participation_val);
        etNotes = view.findViewById(R.id.et_notes);
        etObservations = view.findViewById(R.id.et_observations);
        btnSave = view.findViewById(R.id.btn_save_notes);
        btnExportPdf = view.findViewById(R.id.btn_export_pdf);
        btnViewHistory = view.findViewById(R.id.btn_view_history);
        rvPreviousReports = view.findViewById(R.id.rv_previous_reports);
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void setupPreviousReportsRecyclerView(View view) {
        rvPreviousReports.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPreviousReports.setNestedScrollingEnabled(false);
    }

    private void setupWeekSpinner() {
        generateWeekOptions();
        ArrayAdapter<String> weekAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                weekOptions);
        spinnerWeek.setAdapter(weekAdapter);
        spinnerWeek.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedWeekKey = weekKeys.get(position);
                if (selectedStudentId != null) {
                    loadStudentProgress(selectedStudentId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void generateWeekOptions() {
        weekOptions.clear();
        weekKeys.clear();

        SimpleDateFormat sdf = new SimpleDateFormat("MMM d - ", Locale.getDefault());
        SimpleDateFormat endSdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        SimpleDateFormat keySdf = new SimpleDateFormat("yyyy-ww", Locale.getDefault());

        // Generate last 12 weeks
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 12; i++) {
            // Set to Monday of the week
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            Date weekStart = cal.getTime();

            // Set to Friday of the week
            cal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
            Date weekEnd = cal.getTime();

            String weekLabel = sdf.format(weekStart) + endSdf.format(weekEnd);
            String weekKey = keySdf.format(weekStart);

            weekOptions.add(weekLabel);
            weekKeys.add(weekKey);

            // Go back one week
            cal.add(Calendar.WEEK_OF_YEAR, -1);
        }

        // Default to current week
        selectedWeekKey = weekKeys.get(0);
    }

    private void applyBigMode() {
        float scale = BigModeHelper.getScale(requireContext());
        if (scale <= 1.0f) return;

        float density = getResources().getDisplayMetrics().density;
        float smallSp = getResources().getDimension(R.dimen.text_small) / density;
        float mediumSp = getResources().getDimension(R.dimen.text_medium) / density;

        tvClassLabel.setTextSize(mediumSp * scale);
        tvStudentLabel.setTextSize(mediumSp * scale);
        tvWeekLabel.setTextSize(mediumSp * scale);
        tvMetricsLabel.setTextSize(mediumSp * scale);
        tvNotesLabel.setTextSize(mediumSp * scale);

        // Scale other views...
        int scaledBtnH = (int) (getResources().getDimensionPixelSize(R.dimen.button_height) * scale);
        ViewGroup.LayoutParams lpS = btnSave.getLayoutParams();
        lpS.height = scaledBtnH;
        btnSave.setLayoutParams(lpS);
        ViewGroup.LayoutParams lpE = btnExportPdf.getLayoutParams();
        lpE.height = scaledBtnH;
        btnExportPdf.setLayoutParams(lpE);
    }

    private void setupMetricWatchers() {
        etAttendanceVal.addTextChangedListener(new MetricWatcher(pbAttendance) {
            @Override void onValue(int v) { currentAttendancePct = v; }
        });
        etAcademicVal.addTextChangedListener(new MetricWatcher(pbAcademic) {
            @Override void onValue(int v) { currentAcademicPct = v; }
        });
        etBehaviorVal.addTextChangedListener(new MetricWatcher(pbBehavior) {
            @Override void onValue(int v) { currentBehaviorPct = v; }
        });
        etParticipationVal.addTextChangedListener(new MetricWatcher(pbParticipation) {
            @Override void onValue(int v) { currentParticipationPct = v; }
        });
    }

    private abstract static class MetricWatcher implements TextWatcher {
        private final ProgressBar pb;
        MetricWatcher(ProgressBar pb) { this.pb = pb; }
        abstract void onValue(int v);

        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
        @Override
        public void afterTextChanged(Editable s) {
            int val = parsePercent(s.toString());
            onValue(val);
            pb.setProgress(val);
        }
    }

    private static int parsePercent(String s) {
        try {
            return Math.max(0, Math.min(100, Integer.parseInt(s.trim())));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void setupClassSpinnerListener() {
        spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= classIds.size()) return;
                selectedClassId = classIds.get(position);
                selectedClassName = classNames.get(position);
                loadStudentsForClass(selectedClassId);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupStudentSpinnerListener() {
        spinnerStudent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= studentIds.size()) return;
                selectedStudentId = studentIds.get(position);
                selectedStudentName = studentNames.get(position);
                selectedStudentDocRef = studentDocRefs.get(position);
                spinnerWeek.setEnabled(true);
                loadStudentProgress(selectedStudentId);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedStudentId = null;
                selectedStudentDocRef = null;
                spinnerWeek.setEnabled(false);
            }
        });
    }

    private void setupButtonListeners() {
        btnSave.setOnClickListener(v -> saveProgress());
        btnExportPdf.setOnClickListener(v -> exportPdf());
        btnViewHistory.setOnClickListener(v -> viewHistory());
    }

    // ── Firebase: class loading ───────────────────────────────────────────────

    private void fetchAssignedClasses() {
        db.collection("classes")
                .whereEqualTo("teacherId", currentTeacherId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (getContext() == null || !isAdded()) return;
                    classNames.clear();
                    classIds.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            classNames.add(name);
                            classIds.add(doc.getId());
                        }
                    }

                    if (classNames.isEmpty()) {
                        tvClassStatus.setText(getString(R.string.no_classes_assigned));
                        tvClassStatus.setVisibility(View.VISIBLE);
                        spinnerClass.setVisibility(View.GONE);
                    } else {
                        tvClassStatus.setVisibility(View.GONE);
                        spinnerClass.setVisibility(View.VISIBLE);
                        spinnerClass.setAdapter(new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_spinner_dropdown_item,
                                classNames));
                        spinnerClass.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null || !isAdded()) return;
                    tvClassStatus.setText(getString(R.string.classes_load_failed));
                    tvClassStatus.setVisibility(View.VISIBLE);
                    spinnerClass.setVisibility(View.GONE);
                });
    }

    private void loadStudentsForClass(String classId) {
        selectedStudentId = null;
        selectedStudentName = null;
        selectedStudentDocRef = null;
        studentNames.clear();
        studentIds.clear();
        studentDocRefs.clear();
        spinnerStudent.setEnabled(false);
        spinnerWeek.setEnabled(false);
        resetMetricsUI();

        db.collectionGroup("students")
                .get()
                .addOnSuccessListener(snap -> {
                    if (getContext() == null || !isAdded()) return;

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        if (!classId.equals(doc.getString("classId"))) continue;

                        String name = doc.getString("name");
                        String studentId = doc.getString("studentId");
                        if (studentId == null || studentId.trim().isEmpty()) {
                            studentId = doc.getId();
                        }
                        if (name != null && !name.isEmpty()) {
                            studentNames.add(name);
                            studentIds.add(studentId);
                            studentDocRefs.add(doc.getReference());
                        }
                    }

                    if (studentNames.isEmpty()) {
                        Toast.makeText(getContext(),
                                getString(R.string.no_students_in_class),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        spinnerStudent.setAdapter(new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_spinner_dropdown_item,
                                studentNames));
                        spinnerStudent.setEnabled(true);
                    }
                });
    }

    // ── Firebase: progress loading (weekly) ───────────────────────────────────

    private void loadStudentProgress(String studentId) {
        if (selectedWeekKey == null) return;

        calculateAttendance(studentId, attendancePct ->
                calculateAcademicPerformance(studentId, academicPct -> {
                    if (getContext() == null || !isAdded()) return;

                    // Load weekly report
                    String reportId = studentId + "_" + selectedWeekKey;
                    db.collection("weeklyProgress").document(reportId)
                            .get()
                            .addOnSuccessListener(doc -> {
                                if (getContext() == null || !isAdded()) return;

                                int attendance = attendancePct;
                                int academic = academicPct;
                                int behavior = 0;
                                int participation = 0;
                                String notes = "";
                                String observations = "";

                                if (doc.exists()) {
                                    Long a = doc.getLong("attendance");
                                    Long ac = doc.getLong("academic");
                                    Long b = doc.getLong("behavior");
                                    Long p = doc.getLong("participation");
                                    String n = doc.getString("notes");
                                    String ob = doc.getString("observations");
                                    if (a != null) attendance = a.intValue();
                                    if (ac != null) academic = ac.intValue();
                                    if (b != null) behavior = b.intValue();
                                    if (p != null) participation = p.intValue();
                                    if (n != null) notes = n;
                                    if (ob != null) observations = ob;
                                }

                                updateAllUI(attendance, academic, behavior, participation,
                                        notes, observations);
                                loadPreviousReports();
                            })
                            .addOnFailureListener(e -> {
                                updateAllUI(attendancePct, academicPct, 0, 0, "", "");
                                loadPreviousReports();
                            });
                })
        );
    }

    private void loadPreviousReports() {
        if (selectedStudentId == null) return;

        // FIX: Removed orderBy to avoid composite index requirement
        // Will sort manually after fetching
        db.collection("weeklyProgress")
                .whereEqualTo("studentId", selectedStudentId)
                .get()
                .addOnSuccessListener(snap -> {
                    List<WeeklyReport> reports = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        WeeklyReport report = new WeeklyReport();
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
                    Collections.sort(reports, new Comparator<WeeklyReport>() {
                        @Override
                        public int compare(WeeklyReport r1, WeeklyReport r2) {
                            if (r1.getWeekKey() == null && r2.getWeekKey() == null) return 0;
                            if (r1.getWeekKey() == null) return 1;
                            if (r2.getWeekKey() == null) return -1;
                            return r2.getWeekKey().compareTo(r1.getWeekKey()); // Descending
                        }
                    });

                    // Limit to 10 most recent
                    if (reports.size() > 10) {
                        reports = reports.subList(0, 10);
                    }

                    PreviousReportsAdapter adapter = new PreviousReportsAdapter(reports);
                    rvPreviousReports.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    // Silently fail - previous reports are supplementary
                    android.util.Log.e("StudentProgress", "Failed to load previous reports: " + e.getMessage());
                });
    }

    private void calculateAttendance(String studentId, OnIntResult callback) {
        db.collection("attendance")
                .whereEqualTo("class", selectedClassName)
                .get()
                .addOnSuccessListener(snap -> {
                    int total = 0, present = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> students =
                                (List<Map<String, Object>>) doc.get("students");
                        if (students == null) continue;
                        for (Map<String, Object> s : students) {
                            if (studentId.equals(s.get("studentId"))) {
                                total++;
                                if (Boolean.TRUE.equals(s.get("present"))) present++;
                                break;
                            }
                        }
                    }
                    callback.onResult(total == 0 ? 0 : (present * 100 / total));
                })
                .addOnFailureListener(e -> callback.onResult(0));
    }

    private void calculateAcademicPerformance(String studentId, OnIntResult callback) {
        db.collection("grades")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(snap -> {
                    double total = 0;
                    int count = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Double score = doc.getDouble("score");
                        if (score != null) { total += score; count++; }
                    }
                    callback.onResult(count == 0 ? 0 : (int) (total / count));
                })
                .addOnFailureListener(e -> callback.onResult(0));
    }

    private interface OnIntResult { void onResult(int value); }

    // ── Save (Weekly) ─────────────────────────────────────────────────────────

    private void saveProgress() {
        if (selectedStudentId == null || selectedWeekKey == null) {
            Toast.makeText(getContext(), "Please select student and week", Toast.LENGTH_SHORT).show();
            return;
        }

        int attendance = parsePercent(etAttendanceVal.getText().toString());
        int academic = parsePercent(etAcademicVal.getText().toString());
        int behavior = parsePercent(etBehaviorVal.getText().toString());
        int participation = parsePercent(etParticipationVal.getText().toString());
        String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";
        String obs = etObservations.getText() != null ? etObservations.getText().toString().trim() : "";

        String reportId = selectedStudentId + "_" + selectedWeekKey;
        String weekLabel = weekOptions.get(weekKeys.indexOf(selectedWeekKey));

        Map<String, Object> data = new HashMap<>();
        data.put("attendance", attendance);
        data.put("academic", academic);
        data.put("behavior", behavior);
        data.put("participation", participation);
        data.put("notes", notes);
        data.put("observations", obs);
        data.put("teacherId", currentTeacherId);
        data.put("studentId", selectedStudentId);
        data.put("studentName", selectedStudentName);
        data.put("classId", selectedClassId);
        data.put("className", selectedClassName);
        data.put("weekKey", selectedWeekKey);
        data.put("weekLabel", weekLabel);
        data.put("updatedAt", Timestamp.now());

        // Save to weeklyProgress collection
        db.collection("weeklyProgress").document(reportId)
                .set(data)
                .addOnSuccessListener(unused -> {
                    if (getContext() == null || !isAdded()) return;

                    // Also update student's profile for parent view
                    if (selectedStudentDocRef != null) {
                        Map<String, Object> profileUpdate = new HashMap<>();
                        profileUpdate.put("latestAttendance", attendance);
                        profileUpdate.put("latestAcademic", academic);
                        profileUpdate.put("latestBehavior", behavior);
                        profileUpdate.put("latestParticipation", participation);
                        profileUpdate.put("latestWeekKey", selectedWeekKey);
                        profileUpdate.put("latestWeekLabel", weekLabel);
                        profileUpdate.put("teacherUpdatedAt", Timestamp.now());

                        selectedStudentDocRef.update(profileUpdate);
                    }

                    Toast.makeText(getContext(), "Progress saved for " + weekLabel, Toast.LENGTH_SHORT).show();
                    loadPreviousReports(); // Refresh previous reports
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to save progress", Toast.LENGTH_SHORT).show();
                });
    }

    private void viewHistory() {
        if (selectedStudentId == null) {
            Toast.makeText(getContext(), "Please select a student first", Toast.LENGTH_SHORT).show();
            return;
        }

        WeeklyHistoryFragment fragment = new WeeklyHistoryFragment();
        Bundle args = new Bundle();
        args.putString("studentId", selectedStudentId);
        args.putString("studentName", selectedStudentName);
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // ── PDF export ────────────────────────────────────────────────────────────

    private void exportPdf() {
        if (selectedStudentId == null || selectedStudentName == null) {
            Toast.makeText(getContext(), "Please select a student first", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null) {
                Toast.makeText(getContext(), "Cannot access storage", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!dir.exists()) dir.mkdirs();

            String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String safeName = selectedStudentName.replaceAll("[^a-zA-Z0-9]", "_");
            File pdfFile = new File(dir, "Progress_" + safeName + "_" + selectedWeekKey + "_" + ts + ".pdf");

            PdfWriter writer = new PdfWriter(pdfFile.getAbsolutePath());
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            String today = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(new Date());
            String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";
            String obs = etObservations.getText() != null ? etObservations.getText().toString().trim() : "";
            String weekLabel = weekOptions.get(weekKeys.indexOf(selectedWeekKey));

            document.add(new Paragraph("Student Progress Report").setFontSize(18f).setBold());
            document.add(new Paragraph("Generated: " + today).setFontSize(10f));
            document.add(new Paragraph("Week: " + weekLabel).setFontSize(10f));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Student: " + selectedStudentName).setFontSize(12f));
            document.add(new Paragraph("Student ID: " + selectedStudentId).setFontSize(12f));
            document.add(new Paragraph("Class: " + selectedClassName).setFontSize(12f));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Performance Metrics").setFontSize(14f).setBold());
            document.add(new Paragraph("Attendance:    " + currentAttendancePct + "%").setFontSize(12f));
            document.add(new Paragraph("Academic:      " + currentAcademicPct + "%").setFontSize(12f));
            document.add(new Paragraph("Behavior:      " + currentBehaviorPct + "%").setFontSize(12f));
            document.add(new Paragraph("Participation: " + currentParticipationPct + "%").setFontSize(12f));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Teacher Notes").setFontSize(14f).setBold());
            document.add(new Paragraph(notes.isEmpty() ? "No notes added." : notes).setFontSize(12f));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Observations").setFontSize(14f).setBold());
            document.add(new Paragraph(obs.isEmpty() ? "No observations added." : obs).setFontSize(12f));

            document.close();
            Toast.makeText(getContext(), "PDF exported to Downloads", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Failed to export PDF", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void updateAllUI(int attendance, int academic, int behavior, int participation,
                             String notes, String observations) {
        if (getContext() == null || !isAdded()) return;
        setMetric(etAttendanceVal, pbAttendance, attendance);
        setMetric(etAcademicVal, pbAcademic, academic);
        setMetric(etBehaviorVal, pbBehavior, behavior);
        setMetric(etParticipationVal, pbParticipation, participation);
        etNotes.setText(notes);
        etObservations.setText(observations);
    }

    private void setMetric(EditText et, ProgressBar pb, int value) {
        et.setText(String.valueOf(value));
        pb.setProgress(value);
    }

    private void resetMetricsUI() {
        updateAllUI(0, 0, 0, 0, "", "");
    }

    // ── Data classes ──────────────────────────────────────────────────────────

    public static class WeeklyReport {
        private String weekLabel;
        private String weekKey;
        private int attendance;
        private int academic;
        private int behavior;
        private int participation;
        private String notes;

        public String getWeekLabel() { return weekLabel; }
        public void setWeekLabel(String weekLabel) { this.weekLabel = weekLabel; }
        public String getWeekKey() { return weekKey; }
        public void setWeekKey(String weekKey) { this.weekKey = weekKey; }
        public int getAttendance() { return attendance; }
        public void setAttendance(int attendance) { this.attendance = attendance; }
        public int getAcademic() { return academic; }
        public void setAcademic(int academic) { this.academic = academic; }
        public int getBehavior() { return behavior; }
        public void setBehavior(int behavior) { this.behavior = behavior; }
        public int getParticipation() { return participation; }
        public void setParticipation(int participation) { this.participation = participation; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    // ── Adapters ──────────────────────────────────────────────────────────────

    private class PreviousReportsAdapter extends RecyclerView.Adapter<PreviousReportsAdapter.VH> {
        private final List<WeeklyReport> reports;

        PreviousReportsAdapter(List<WeeklyReport> reports) {
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
            WeeklyReport r = reports.get(position);
            h.tvWeek.setText(r.getWeekLabel());
            h.tvSummary.setText(String.format(Locale.getDefault(),
                    "A:%d%% B:%d%% P:%d%%", r.getAttendance(), r.getBehavior(), r.getParticipation()));
            h.tvNotes.setText(r.getNotes() != null ? r.getNotes() : "No notes");
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