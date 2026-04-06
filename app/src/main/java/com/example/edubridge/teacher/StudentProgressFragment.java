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

import com.example.edubridge.R;
import com.example.edubridge.shared.BigModeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentProgressFragment extends Fragment {

    // ── Views ──────────────────────────────────────────────────────────────────
    private Spinner spinnerClass, spinnerStudent;
    private TextView tvClassLabel, tvStudentLabel, tvMetricsLabel, tvNotesLabel;
    private TextView tvAttendanceLabel, tvAcademicLabel, tvBehaviorLabel, tvParticipationLabel;
    private TextView tvClassStatus;
    private ProgressBar pbAttendance, pbAcademic, pbBehavior, pbParticipation;
    // Each metric has an editable number field (0-100).  Automated fields are
    // pre-populated from Firestore; the teacher may override any value.
    private EditText etAttendanceVal, etAcademicVal, etBehaviorVal, etParticipationVal;
    private EditText etNotes, etObservations;
    private MaterialButton btnSave, btnExportPdf;

    // ── Firebase ───────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private String currentTeacherId;

    // ── Spinner data — parallel lists, index maps name ↔ id ──────────────────
    private final List<String>            classNames    = new ArrayList<>();
    private final List<String>            classIds      = new ArrayList<>();
    private final List<String>            studentNames  = new ArrayList<>();
    private final List<String>            studentIds    = new ArrayList<>();
    // Full Firestore reference for each student (users/{parentUID}/students/{docId})
    // used to write progress back to the student's own document for parent visibility.
    private final List<DocumentReference> studentDocRefs = new ArrayList<>();

    // ── Selected state ────────────────────────────────────────────────────────
    private String            selectedClassId        = null;
    private String            selectedClassName      = null;
    private String            selectedStudentId      = null;
    private String            selectedStudentName    = null;
    private DocumentReference selectedStudentDocRef  = null;

    // ── Cached metric values (kept in sync with EditTexts via TextWatcher) ────
    private int currentAttendancePct = 0;
    private int currentAcademicPct   = 0;
    private int currentBehaviorPct   = 0;
    private int currentParticipationPct = 0;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        currentTeacherId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        bindViews(view);
        setupToolbar(view);
        applyBigMode();
        setupMetricWatchers();        // EditText → ProgressBar live link
        setupStudentSpinnerListener();
        setupClassSpinnerListener();
        setupButtonListeners();

        spinnerClass.setEnabled(false);
        spinnerStudent.setEnabled(false);

        fetchAssignedClasses();
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void bindViews(View view) {
        spinnerClass          = view.findViewById(R.id.spinner_class);
        spinnerStudent        = view.findViewById(R.id.spinner_student);
        tvClassLabel          = view.findViewById(R.id.tv_class_label);
        tvStudentLabel        = view.findViewById(R.id.tv_student_label);
        tvMetricsLabel        = view.findViewById(R.id.tv_metrics_label);
        tvNotesLabel          = view.findViewById(R.id.tv_notes_label);
        tvAttendanceLabel     = view.findViewById(R.id.tv_attendance_label);
        tvAcademicLabel       = view.findViewById(R.id.tv_academic_label);
        tvBehaviorLabel       = view.findViewById(R.id.tv_behavior_label);
        tvParticipationLabel  = view.findViewById(R.id.tv_participation_label);
        tvClassStatus         = view.findViewById(R.id.tv_class_status);
        pbAttendance          = view.findViewById(R.id.pb_attendance);
        pbAcademic            = view.findViewById(R.id.pb_academic);
        pbBehavior            = view.findViewById(R.id.pb_behavior);
        pbParticipation       = view.findViewById(R.id.pb_participation);
        etAttendanceVal       = view.findViewById(R.id.et_attendance_val);
        etAcademicVal         = view.findViewById(R.id.et_academic_val);
        etBehaviorVal         = view.findViewById(R.id.et_behavior_val);
        etParticipationVal    = view.findViewById(R.id.et_participation_val);
        etNotes               = view.findViewById(R.id.et_notes);
        etObservations        = view.findViewById(R.id.et_observations);
        btnSave               = view.findViewById(R.id.btn_save_notes);
        btnExportPdf          = view.findViewById(R.id.btn_export_pdf);
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    // ── BigModeHelper ─────────────────────────────────────────────────────────

    private void applyBigMode() {
        float scale = BigModeHelper.getScale(requireContext());
        if (scale <= 1.0f) return;

        float density  = getResources().getDisplayMetrics().density;
        float smallSp  = getResources().getDimension(R.dimen.text_small)  / density;
        float mediumSp = getResources().getDimension(R.dimen.text_medium) / density;

        tvClassLabel.setTextSize(mediumSp * scale);
        tvStudentLabel.setTextSize(mediumSp * scale);
        tvMetricsLabel.setTextSize(mediumSp * scale);
        tvNotesLabel.setTextSize(mediumSp * scale);

        tvAttendanceLabel.setTextSize(smallSp * scale);
        tvAcademicLabel.setTextSize(smallSp * scale);
        tvBehaviorLabel.setTextSize(smallSp * scale);
        tvParticipationLabel.setTextSize(smallSp * scale);
        etAttendanceVal.setTextSize(smallSp * scale);
        etAcademicVal.setTextSize(smallSp * scale);
        etBehaviorVal.setTextSize(smallSp * scale);
        etParticipationVal.setTextSize(smallSp * scale);

        int scaledBtnH = (int) (getResources().getDimensionPixelSize(R.dimen.button_height) * scale);
        ViewGroup.LayoutParams lpS = btnSave.getLayoutParams();
        lpS.height = scaledBtnH;
        btnSave.setLayoutParams(lpS);
        ViewGroup.LayoutParams lpE = btnExportPdf.getLayoutParams();
        lpE.height = scaledBtnH;
        btnExportPdf.setLayoutParams(lpE);

        int scaledBarH = (int) (getResources().getDimensionPixelSize(R.dimen.progress_bar_height) * scale);
        for (ProgressBar pb : new ProgressBar[]{pbAttendance, pbAcademic, pbBehavior, pbParticipation}) {
            ViewGroup.LayoutParams lp = pb.getLayoutParams();
            lp.height = scaledBarH;
            pb.setLayoutParams(lp);
        }
    }

    // ── Metric EditText → ProgressBar live link ───────────────────────────────
    // Each EditText drives its ProgressBar in real-time and keeps the cached
    // int value in sync so the PDF always reflects what the teacher sees.

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

    /** TextWatcher that clamps input to 0-100 and syncs a ProgressBar. */
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

    // ── Spinner listeners ─────────────────────────────────────────────────────

    private void setupClassSpinnerListener() {
        spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= classIds.size()) return;
                selectedClassId   = classIds.get(position);
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
                selectedStudentId     = studentIds.get(position);
                selectedStudentName   = studentNames.get(position);
                selectedStudentDocRef = studentDocRefs.get(position);
                loadStudentProgress(selectedStudentId);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedStudentId = null;
                selectedStudentDocRef = null;
            }
        });
    }

    private void setupButtonListeners() {
        btnSave.setOnClickListener(v -> saveProgress());
        btnExportPdf.setOnClickListener(v -> exportPdf());
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

    // ── Firebase: student loading ─────────────────────────────────────────────
    // Students live at users/{parentUID}/students/{docId}.
    // collectionGroup() with no server filter avoids the collection-group index
    // requirement; matching on classId is done client-side.

    private void loadStudentsForClass(String classId) {
        selectedStudentId     = null;
        selectedStudentName   = null;
        selectedStudentDocRef = null;
        studentNames.clear();
        studentIds.clear();
        studentDocRefs.clear();
        spinnerStudent.setEnabled(false);
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
                            studentDocRefs.add(doc.getReference()); // for parent-visibility write
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
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null || !isAdded()) return;
                    android.util.Log.e("StudentProgress", "student fetch failed", e);
                    Toast.makeText(getContext(),
                            getString(R.string.students_load_failed) + ": " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    // ── Firebase: progress loading ────────────────────────────────────────────
    // Chain: attendance (attendance collection) → academic (grades collection)
    //        → saved overrides (studentProgress doc) → update UI.

    private void loadStudentProgress(String studentId) {
        calculateAttendance(studentId, attendancePct ->
            calculateAcademicPerformance(studentId, academicPct -> {
                if (getContext() == null || !isAdded()) return;

                db.collection("studentProgress").document(studentId)
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (getContext() == null || !isAdded()) return;

                            // Teacher-saved values take precedence over computed ones.
                            int attendance    = attendancePct;
                            int academic      = academicPct;
                            int behavior      = 0;
                            int participation = 0;
                            String notes        = "";
                            String observations = "";

                            if (doc.exists()) {
                                Long a  = doc.getLong("attendance");
                                Long ac = doc.getLong("academic");
                                Long b  = doc.getLong("behavior");
                                Long p  = doc.getLong("participation");
                                String n  = doc.getString("notes");
                                String ob = doc.getString("observations");
                                if (a  != null) attendance    = a.intValue();
                                if (ac != null) academic      = ac.intValue();
                                if (b  != null) behavior      = b.intValue();
                                if (p  != null) participation = p.intValue();
                                if (n  != null) notes         = n;
                                if (ob != null) observations  = ob;
                            }

                            updateAllUI(attendance, academic, behavior, participation,
                                    notes, observations);
                        })
                        .addOnFailureListener(e -> {
                            if (getContext() == null || !isAdded()) return;
                            // Fall back to computed values, blank text fields
                            updateAllUI(attendancePct, academicPct, 0, 0, "", "");
                            Toast.makeText(getContext(),
                                    getString(R.string.progress_load_failed),
                                    Toast.LENGTH_SHORT).show();
                        });
            })
        );
    }

    /**
     * Counts present/total across all attendance documents for the class,
     * matching entries by studentId inside the embedded students array.
     * Single whereEqualTo — no composite index needed.
     */
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

    /**
     * Averages the "score" field across all grades documents for this student.
     * Single whereEqualTo — no composite index needed.
     */
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

    // ── Save ──────────────────────────────────────────────────────────────────
    // Writes to two locations:
    //   1. studentProgress/{studentId}  — teacher's authoritative record
    //   2. users/{parentUID}/students/{docId} — the student's own profile so
    //      that parents see the updated notes and scores through their interface.

    private void saveProgress() {
        if (selectedStudentId == null || selectedStudentDocRef == null) {
            Toast.makeText(getContext(),
                    getString(R.string.select_student_first),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int attendance    = parsePercent(etAttendanceVal.getText().toString());
        int academic      = parsePercent(etAcademicVal.getText().toString());
        int behavior      = parsePercent(etBehaviorVal.getText().toString());
        int participation = parsePercent(etParticipationVal.getText().toString());
        String notes      = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";
        String obs        = etObservations.getText() != null ? etObservations.getText().toString().trim() : "";

        Map<String, Object> data = new HashMap<>();
        data.put("attendance",    attendance);
        data.put("academic",      academic);
        data.put("behavior",      behavior);
        data.put("participation", participation);
        data.put("notes",         notes);
        data.put("observations",  obs);
        data.put("teacherId",     currentTeacherId);
        data.put("studentId",     selectedStudentId);
        data.put("studentName",   selectedStudentName);
        data.put("classId",       selectedClassId);
        data.put("className",     selectedClassName);
        data.put("updatedAt",     Timestamp.now());

        // 1 — Teacher's record
        db.collection("studentProgress").document(selectedStudentId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (getContext() == null || !isAdded()) return;
                    // 2 — Student's own profile (parent-visible subset)
                    Map<String, Object> profileUpdate = new HashMap<>();
                    profileUpdate.put("attendance",       attendance);
                    profileUpdate.put("academic",         academic);
                    profileUpdate.put("behavior",         behavior);
                    profileUpdate.put("participation",    participation);
                    profileUpdate.put("notes",            notes);
                    profileUpdate.put("observations",     obs);
                    profileUpdate.put("teacherUpdatedAt", Timestamp.now());

                    selectedStudentDocRef.update(profileUpdate)
                            .addOnSuccessListener(v -> {
                                if (getContext() == null || !isAdded()) return;
                                Toast.makeText(getContext(),
                                        getString(R.string.progress_saved),
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                if (getContext() == null || !isAdded()) return;
                                // studentProgress write succeeded; profile update failed — warn only
                                android.util.Log.e("StudentProgress", "profile update failed", e);
                                Toast.makeText(getContext(),
                                        getString(R.string.progress_saved),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null || !isAdded()) return;
                    Toast.makeText(getContext(),
                            getString(R.string.progress_save_failed),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ── PDF export ────────────────────────────────────────────────────────────

    private void exportPdf() {
        if (selectedStudentId == null || selectedStudentName == null) {
            Toast.makeText(getContext(),
                    getString(R.string.select_student_first),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null) {
                Toast.makeText(getContext(), getString(R.string.pdf_failed), Toast.LENGTH_SHORT).show();
                return;
            }
            if (!dir.exists()) dir.mkdirs();

            String ts       = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String safeName = selectedStudentName.replaceAll("[^a-zA-Z0-9]", "_");
            File   pdfFile  = new File(dir, "Progress_" + safeName + "_" + ts + ".pdf");

            PdfWriter   writer   = new PdfWriter(pdfFile.getAbsolutePath());
            PdfDocument pdfDoc   = new PdfDocument(writer);
            Document    document = new Document(pdfDoc);

            String today = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(new Date());
            String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";
            String obs   = etObservations.getText() != null ? etObservations.getText().toString().trim() : "";

            document.add(new Paragraph("Student Progress Report").setFontSize(18f).setBold());
            document.add(new Paragraph("Generated: " + today).setFontSize(10f));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Student: " + selectedStudentName).setFontSize(12f));
            document.add(new Paragraph("Student ID: " + selectedStudentId).setFontSize(12f));
            document.add(new Paragraph("Class: " + selectedClassName).setFontSize(12f));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Performance Metrics").setFontSize(14f).setBold());
            document.add(new Paragraph("Attendance:    " + currentAttendancePct + "%").setFontSize(12f));
            document.add(new Paragraph("Academic:      " + currentAcademicPct   + "%").setFontSize(12f));
            document.add(new Paragraph("Behavior:      " + currentBehaviorPct   + "%").setFontSize(12f));
            document.add(new Paragraph("Participation: " + currentParticipationPct + "%").setFontSize(12f));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Teacher Notes").setFontSize(14f).setBold());
            document.add(new Paragraph(notes.isEmpty() ? "No notes added." : notes).setFontSize(12f));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Observations").setFontSize(14f).setBold());
            document.add(new Paragraph(obs.isEmpty() ? "No observations added." : obs).setFontSize(12f));

            document.close();

            Toast.makeText(getContext(),
                    getString(R.string.pdf_saved, pdfFile.getAbsolutePath()),
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            android.util.Log.e("StudentProgress", "PDF export failed", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.pdf_failed), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    /** Sets all four metric fields atomically (EditText + ProgressBar). */
    private void updateAllUI(int attendance, int academic, int behavior, int participation,
                             String notes, String observations) {
        if (getContext() == null || !isAdded()) return;
        setMetric(etAttendanceVal,   pbAttendance,   attendance);
        setMetric(etAcademicVal,     pbAcademic,     academic);
        setMetric(etBehaviorVal,     pbBehavior,     behavior);
        setMetric(etParticipationVal, pbParticipation, participation);
        etNotes.setText(notes);
        etObservations.setText(observations);
    }

    /**
     * Sets an EditText to the given value. The MetricWatcher registered on the
     * EditText will automatically sync the ProgressBar and cached int.
     */
    private void setMetric(EditText et, ProgressBar pb, int value) {
        et.setText(String.valueOf(value));
        // Watcher fires asynchronously; set pb directly too for instant feedback.
        pb.setProgress(value);
    }

    private void resetMetricsUI() {
        updateAllUI(0, 0, 0, 0, "", "");
    }
}
