package com.example.edubridge.parent;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ParentStudentReportFragment extends Fragment {

    private Spinner spinnerStudent, spinnerTerm;
    private TabLayout tabLayout;
    private TextView tvStudentName, tvStudentClass, tvStudentRoll, tvAttendancePercentage;
    private RecyclerView subjectsRecyclerView, attendanceRecyclerView, weeklyReportsRecyclerView;
    private MaterialCardView cardAttendance, cardPerformance, cardWeeklyReports;
    private MaterialButton btnExportPdf;

    private FirebaseFirestore db;
    private String parentId;
    private List<StudentInfo> studentList;
    private String selectedStudentId;
    private String selectedStudentName;
    private String selectedTerm = "Term 1";
    private List<SubjectGrade> currentGrades = new ArrayList<>();
    private List<AttendanceRecord> currentAttendance = new ArrayList<>();
    private List<WeeklyReportItem> currentWeeklyReports = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parent_student_report, container, false);
        TextSizeHelper.applyScaleRecursively(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextSizeHelper.applyScaleRecursively(view);

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), getString(R.string.please_login), Toast.LENGTH_SHORT).show();
            return;
        }

        parentId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        studentList = new ArrayList<>();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        initializeViews(view);
        setupSpinners();
        setupTabs();
        loadStudents();

        btnExportPdf.setOnClickListener(v -> exportReportAsPdf());
    }

    private void initializeViews(View view) {
        spinnerStudent = view.findViewById(R.id.spinner_student);
        spinnerTerm = view.findViewById(R.id.spinner_term);
        tabLayout = view.findViewById(R.id.tab_layout);
        tvStudentName = view.findViewById(R.id.tv_student_name);
        tvStudentClass = view.findViewById(R.id.tv_student_class);
        tvStudentRoll = view.findViewById(R.id.tv_student_roll);
        tvAttendancePercentage = view.findViewById(R.id.tv_attendance_percentage);
        subjectsRecyclerView = view.findViewById(R.id.subjects_recycler_view);
        attendanceRecyclerView = view.findViewById(R.id.attendance_recycler_view);
        weeklyReportsRecyclerView = view.findViewById(R.id.weekly_reports_recycler_view);
        cardAttendance = view.findViewById(R.id.card_attendance);
        cardPerformance = view.findViewById(R.id.card_performance);
        cardWeeklyReports = view.findViewById(R.id.card_weekly_reports);
        btnExportPdf = view.findViewById(R.id.btn_export_pdf);
    }

    private void setupSpinners() {
        String[] terms = {"Term 1", "Term 2", "Term 3", "Annual"};

        ArrayAdapter<String> termAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                terms
        );

        spinnerTerm.setAdapter(termAdapter);

        spinnerTerm.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTerm = terms[position];

                if (selectedStudentId != null) {
                    loadStudentReport();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Attendance"));
        tabLayout.addTab(tabLayout.newTab().setText("Performance"));
        tabLayout.addTab(tabLayout.newTab().setText("Weekly Reports"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        cardAttendance.setVisibility(View.VISIBLE);
                        cardPerformance.setVisibility(View.GONE);
                        cardWeeklyReports.setVisibility(View.GONE);
                        break;

                    case 1:
                        cardAttendance.setVisibility(View.GONE);
                        cardPerformance.setVisibility(View.VISIBLE);
                        cardWeeklyReports.setVisibility(View.GONE);
                        loadPerformanceData();
                        break;

                    case 2:
                        cardAttendance.setVisibility(View.GONE);
                        cardPerformance.setVisibility(View.GONE);
                        cardWeeklyReports.setVisibility(View.VISIBLE);
                        loadWeeklyReports();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void loadStudents() {
        db.collection("users")
                .document(parentId)
                .collection("students")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentList.clear();
                    List<String> studentNames = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        StudentInfo student = new StudentInfo();

                        String studentId = doc.getString("studentId");
                        if (studentId == null || studentId.trim().isEmpty()) {
                            studentId = doc.getId();
                        }

                        student.setId(studentId);
                        student.setName(doc.getString("name"));
                        student.setClassName(doc.getString("class"));
                        student.setClassId(doc.getString("classId"));
                        student.setRollNumber(studentId);

                        if (student.getName() == null) student.setName("Unknown");
                        if (student.getClassName() == null) student.setClassName("N/A");
                        if (student.getRollNumber() == null) student.setRollNumber("N/A");

                        studentList.add(student);
                        studentNames.add(student.getName());
                    }

                    if (studentList.isEmpty()) {
                        Toast.makeText(getContext(), getString(R.string.no_students_linked), Toast.LENGTH_LONG).show();
                        return;
                    }

                    ArrayAdapter<String> studentAdapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_dropdown_item,
                            studentNames
                    );

                    spinnerStudent.setAdapter(studentAdapter);

                    spinnerStudent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            StudentInfo selected = studentList.get(position);
                            selectedStudentId = selected.getId();
                            selectedStudentName = selected.getName();
                            displayStudentInfo(selected);
                            loadStudentReport();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });

                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), getString(R.string.error_loading_students, e.getMessage()), Toast.LENGTH_SHORT).show()
                );
    }

    private void displayStudentInfo(StudentInfo student) {
        tvStudentName.setText(student.getName());
        tvStudentClass.setText("Class: " + (student.getClassName() != null ? student.getClassName() : "N/A"));
        tvStudentRoll.setText("Roll: " + (student.getRollNumber() != null ? student.getRollNumber() : "N/A"));
    }

    private void loadStudentReport() {
        loadAttendanceData();
    }

    private void loadAttendanceData() {
        db.collection("attendance")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    currentAttendance.clear();
                    int presentCount = 0;
                    int totalCount = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> students = (List<Map<String, Object>>) doc.get("students");
                        if (students != null) {
                            for (Map<String, Object> student : students) {
                                String sid = String.valueOf(student.get("studentId"));
                                if (selectedStudentId.equals(sid)) {
                                    Boolean present = (Boolean) student.get("present");
                                    if (present == null) {
                                        String status = (String) student.get("status");
                                        present = "present".equalsIgnoreCase(status);
                                    }

                                    String date = doc.getString("date");
                                    Date timestamp = doc.getDate("timestamp");

                                    AttendanceRecord record = new AttendanceRecord();
                                    record.setDate(date != null ? date : "Unknown");
                                    record.setTimestamp(timestamp);
                                    record.setPresent(present != null && present);

                                    currentAttendance.add(record);
                                    if (present != null && present) presentCount++;
                                    totalCount++;
                                    break;
                                }
                            }
                        }
                    }

                    Collections.sort(currentAttendance, (r1, r2) -> {
                        if (r1.getTimestamp() == null && r2.getTimestamp() == null) return 0;
                        if (r1.getTimestamp() == null) return 1;
                        if (r2.getTimestamp() == null) return -1;
                        return r2.getTimestamp().compareTo(r1.getTimestamp());
                    });

                    if (totalCount > 0) {
                        int percentage = (presentCount * 100) / totalCount;
                        tvAttendancePercentage.setText(percentage + "%");
                    } else {
                        tvAttendancePercentage.setText("N/A");
                    }

                    attendanceRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    attendanceRecyclerView.setAdapter(new AttendanceAdapter(currentAttendance));
                })
                .addOnFailureListener(e -> {
                    currentAttendance.clear();
                    tvAttendancePercentage.setText("N/A");
                    attendanceRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    attendanceRecyclerView.setAdapter(new AttendanceAdapter(currentAttendance));
                });
    }

    private void loadPerformanceData() {
        db.collection("grades")
                .whereEqualTo("studentId", selectedStudentId)
                .whereEqualTo("term", selectedTerm)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    currentGrades.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        SubjectGrade grade = new SubjectGrade();
                        grade.setSubject(doc.getString("subject"));
                        grade.setGrade(doc.getString("grade"));
                        grade.setScore(doc.getDouble("score"));
                        grade.setRemarks(doc.getString("remarks"));
                        if (grade.getSubject() == null) grade.setSubject("Unknown");
                        if (grade.getGrade() == null) grade.setGrade("N/A");
                        if (grade.getScore() == null) grade.setScore(0.0);
                        if (grade.getRemarks() == null) grade.setRemarks("");
                        currentGrades.add(grade);
                    }
                    subjectsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    subjectsRecyclerView.setAdapter(new PerformanceAdapter(currentGrades));
                })
                .addOnFailureListener(e -> {
                    currentGrades.clear();
                    subjectsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    subjectsRecyclerView.setAdapter(new PerformanceAdapter(currentGrades));
                });
    }

    private void loadWeeklyReports() {
        if (selectedStudentId == null) return;

        db.collection("weeklyProgress")
                .whereEqualTo("studentId", selectedStudentId)
                .get()
                .addOnSuccessListener(snap -> {
                    currentWeeklyReports.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        WeeklyReportItem item = new WeeklyReportItem();
                        item.setWeekLabel(doc.getString("weekLabel"));
                        item.setWeekKey(doc.getString("weekKey"));
                        item.setAttendance(doc.getLong("attendance") != null ? doc.getLong("attendance").intValue() : 0);
                        item.setAcademic(doc.getLong("academic") != null ? doc.getLong("academic").intValue() : 0);
                        item.setBehavior(doc.getLong("behavior") != null ? doc.getLong("behavior").intValue() : 0);
                        item.setParticipation(doc.getLong("participation") != null ? doc.getLong("participation").intValue() : 0);
                        item.setNotes(doc.getString("notes"));
                        item.setObservations(doc.getString("observations"));
                        item.setTeacherId(doc.getString("teacherId"));
                        currentWeeklyReports.add(item);
                    }

                    Collections.sort(currentWeeklyReports, (r1, r2) -> {
                        if (r1.getWeekKey() == null && r2.getWeekKey() == null) return 0;
                        if (r1.getWeekKey() == null) return 1;
                        if (r2.getWeekKey() == null) return -1;
                        return r2.getWeekKey().compareTo(r1.getWeekKey());
                    });

                    weeklyReportsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    weeklyReportsRecyclerView.setAdapter(new WeeklyReportsAdapter(currentWeeklyReports));
                })
                .addOnFailureListener(e -> {
                    currentWeeklyReports.clear();
                    weeklyReportsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    weeklyReportsRecyclerView.setAdapter(new WeeklyReportsAdapter(currentWeeklyReports));
                });
    }

    private void exportReportAsPdf() {
        if (studentList.isEmpty() || selectedStudentId == null) {
            Toast.makeText(getContext(), getString(R.string.no_student_data), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String fileName = "Student_Report_" + tvStudentName.getText().toString().replace(" ", "_") + "_" + sdf.format(new Date()) + ".pdf";

            File pdfFile;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                pdfFile = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                pdfFile = new File(downloadsDir, fileName);
            }

            PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            addPdfContent(document);

            document.close();

            Toast.makeText(getContext(), "PDF saved: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            sharePdf(pdfFile);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error creating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addPdfContent(Document document) throws Exception {
        PdfFont boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont normalFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        Paragraph title = new Paragraph("Student Academic Report")
                .setFont(boldFont).setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(20);
        document.add(title);

        Paragraph school = new Paragraph("EduBridge International School")
                .setFont(normalFont).setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(30);
        document.add(school);

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20);
        addInfoRow(infoTable, "Student Name:", tvStudentName.getText().toString(), boldFont, normalFont);
        addInfoRow(infoTable, "Class:", tvStudentClass.getText().toString(), boldFont, normalFont);
        addInfoRow(infoTable, "Roll Number:", tvStudentRoll.getText().toString(), boldFont, normalFont);
        addInfoRow(infoTable, "Term:", selectedTerm, boldFont, normalFont);
        addInfoRow(infoTable, "Report Date:", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()), boldFont, normalFont);
        document.add(infoTable);

        Paragraph attTitle = new Paragraph("Attendance Summary")
                .setFont(boldFont).setFontSize(16).setMarginTop(20).setMarginBottom(10);
        document.add(attTitle);

        int totalDays = currentAttendance.size();
        int presentDays = 0;
        for (AttendanceRecord r : currentAttendance) {
            if (r.isPresent()) presentDays++;
        }
        double attPct = totalDays > 0 ? (presentDays * 100.0 / totalDays) : 0;

        document.add(new Paragraph(String.format(Locale.getDefault(),
                "Total Days: %d | Present: %d | Absent: %d | Percentage: %.1f%%",
                totalDays, presentDays, totalDays - presentDays, attPct))
                .setFont(normalFont).setFontSize(12).setMarginBottom(10));

        Table attTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(20);
        addTableHeader(attTable, boldFont, "Date", "Status");
        for (AttendanceRecord r : currentAttendance) {
            attTable.addCell(new Cell().add(new Paragraph(r.getDate() != null ? r.getDate() : "Unknown").setFont(normalFont).setFontSize(10)));
            String status = r.isPresent() ? "Present" : "Absent";
            Cell statusCell = new Cell().add(new Paragraph(status).setFont(normalFont).setFontSize(10));
            statusCell.setFontColor(r.isPresent() ? new DeviceRgb(0, 128, 0) : new DeviceRgb(255, 0, 0));
            attTable.addCell(statusCell);
        }
        document.add(attTable);

        Paragraph perfTitle = new Paragraph("Academic Performance - " + selectedTerm)
                .setFont(boldFont).setFontSize(16).setMarginTop(20).setMarginBottom(10);
        document.add(perfTitle);

        Table perfTable = new Table(UnitValue.createPercentArray(new float[]{40, 15, 20, 25}))
                .setWidth(UnitValue.createPercentValue(100));
        addTableHeader(perfTable, boldFont, "Subject", "Grade", "Score (%)", "Remarks");

        double totalScore = 0;
        for (SubjectGrade g : currentGrades) {
            perfTable.addCell(new Cell().add(new Paragraph(g.getSubject() != null ? g.getSubject() : "Unknown").setFont(normalFont).setFontSize(10)));
            perfTable.addCell(new Cell().add(new Paragraph(g.getGrade() != null ? g.getGrade() : "N/A").setFont(normalFont).setFontSize(10)).setTextAlignment(TextAlignment.CENTER));
            perfTable.addCell(new Cell().add(new Paragraph(String.format(Locale.getDefault(), "%.1f", g.getScore() != null ? g.getScore() : 0.0)).setFont(normalFont).setFontSize(10)).setTextAlignment(TextAlignment.CENTER));
            perfTable.addCell(new Cell().add(new Paragraph(g.getRemarks() != null ? g.getRemarks() : "").setFont(normalFont).setFontSize(10)));
            totalScore += g.getScore() != null ? g.getScore() : 0;
        }

        double average = currentGrades.size() > 0 ? totalScore / currentGrades.size() : 0;
        perfTable.addCell(new Cell(1, 4).add(new Paragraph(String.format(Locale.getDefault(), "Average: %.1f%%", average))
                .setFont(boldFont).setFontSize(11).setTextAlignment(TextAlignment.RIGHT)).setBorderTop(new SolidBorder(1)));
        document.add(perfTable);

        if (!currentWeeklyReports.isEmpty()) {
            Paragraph weeklyTitle = new Paragraph("Weekly Progress Reports (Teacher Feedback)")
                    .setFont(boldFont).setFontSize(16).setMarginTop(20).setMarginBottom(10);
            document.add(weeklyTitle);

            for (WeeklyReportItem wr : currentWeeklyReports) {
                document.add(new Paragraph("Week: " + (wr.getWeekLabel() != null ? wr.getWeekLabel() : "Unknown"))
                        .setFont(boldFont).setFontSize(12).setMarginBottom(5));
                document.add(new Paragraph(String.format(Locale.getDefault(),
                        "Attendance: %d%% | Academic: %d%% | Behavior: %d%% | Participation: %d%%",
                        wr.getAttendance(), wr.getAcademic(), wr.getBehavior(), wr.getParticipation()))
                        .setFont(normalFont).setFontSize(10).setMarginBottom(3));
                if (wr.getNotes() != null && !wr.getNotes().isEmpty()) {
                    document.add(new Paragraph("Notes: " + wr.getNotes()).setFont(normalFont).setFontSize(10).setMarginBottom(3));
                }
                if (wr.getObservations() != null && !wr.getObservations().isEmpty()) {
                    document.add(new Paragraph("Observations: " + wr.getObservations()).setFont(normalFont).setFontSize(10).setMarginBottom(3));
                }
                document.add(new Paragraph(" ").setFontSize(6));
            }
        }

        Paragraph footer = new Paragraph("Generated by EduBridge System")
                .setFont(normalFont).setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER).setMarginTop(30);
        document.add(footer);
    }

    private void addInfoRow(Table table, String label, String value, PdfFont boldFont, PdfFont normalFont) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(boldFont).setFontSize(11)).setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(value != null ? value : "N/A").setFont(normalFont).setFontSize(11)).setBorder(Border.NO_BORDER));
    }

    private void addTableHeader(Table table, PdfFont boldFont, String... headers) {
        for (String header : headers) {
            Cell cell = new Cell().add(new Paragraph(header).setFont(boldFont).setFontSize(11).setTextAlignment(TextAlignment.CENTER));
            cell.setBackgroundColor(new DeviceRgb(200, 200, 200));
            table.addCell(cell);
        }
    }

    private void sharePdf(File pdfFile) {
        Uri fileUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", pdfFile);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Student Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Student Academic Report for " + tvStudentName.getText().toString());
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share PDF Report"));
    }

    public static class StudentInfo {
        private String id, name, className, classId, rollNumber;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public String getClassId() { return classId; }
        public void setClassId(String classId) { this.classId = classId; }
        public String getRollNumber() { return rollNumber; }
        public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    }

    public static class AttendanceRecord {
        private String date;
        private Date timestamp;
        private boolean present;
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
        public boolean isPresent() { return present; }
        public void setPresent(boolean present) { this.present = present; }
    }

    public static class SubjectGrade {
        private String subject, grade, remarks;
        private Double score;
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }

    public static class WeeklyReportItem {
        private String weekLabel, weekKey, notes, observations, teacherId;
        private int attendance, academic, behavior, participation;
        public String getWeekLabel() { return weekLabel; }
        public void setWeekLabel(String weekLabel) { this.weekLabel = weekLabel; }
        public String getWeekKey() { return weekKey; }
        public void setWeekKey(String weekKey) { this.weekKey = weekKey; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getObservations() { return observations; }
        public void setObservations(String observations) { this.observations = observations; }
        public String getTeacherId() { return teacherId; }
        public void setTeacherId(String teacherId) { this.teacherId = teacherId; }
        public int getAttendance() { return attendance; }
        public void setAttendance(int attendance) { this.attendance = attendance; }
        public int getAcademic() { return academic; }
        public void setAcademic(int academic) { this.academic = academic; }
        public int getBehavior() { return behavior; }
        public void setBehavior(int behavior) { this.behavior = behavior; }
        public int getParticipation() { return participation; }
        public void setParticipation(int participation) { this.participation = participation; }
    }

    private class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.VH> {
        private List<AttendanceRecord> records;
        AttendanceAdapter(List<AttendanceRecord> records) { this.records = records; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_record, parent, false);
            TextSizeHelper.applyScaleRecursively(v);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            AttendanceRecord r = records.get(position);
            h.tvDate.setText(r.getDate() != null ? r.getDate() : "Unknown");
            h.tvStatus.setText(r.isPresent() ? "Present" : "Absent");
            h.tvStatus.setTextColor(r.isPresent() ?
                    getResources().getColor(R.color.present_green) :
                    getResources().getColor(R.color.absent_red));
        }

        @Override
        public int getItemCount() { return records.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvDate, tvStatus;
            VH(@NonNull View v) {
                super(v);
                tvDate = v.findViewById(R.id.tv_date);
                tvStatus = v.findViewById(R.id.tv_status);
            }
        }
    }

    private class PerformanceAdapter extends RecyclerView.Adapter<PerformanceAdapter.VH> {
        private List<SubjectGrade> grades;
        PerformanceAdapter(List<SubjectGrade> grades) { this.grades = grades; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject_grade, parent, false);
            TextSizeHelper.applyScaleRecursively(v);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            SubjectGrade g = grades.get(position);
            h.tvSubject.setText(g.getSubject() != null ? g.getSubject() : "Unknown");
            h.tvGrade.setText(g.getGrade() != null ? g.getGrade() : "N/A");
            h.tvScore.setText(String.format(Locale.getDefault(), "%.1f%%", g.getScore() != null ? g.getScore() : 0.0));
            h.tvRemarks.setText(g.getRemarks() != null ? g.getRemarks() : "");
        }

        @Override
        public int getItemCount() { return grades.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvSubject, tvGrade, tvScore, tvRemarks;
            VH(@NonNull View v) {
                super(v);
                tvSubject = v.findViewById(R.id.tv_subject);
                tvGrade = v.findViewById(R.id.tv_grade);
                tvScore = v.findViewById(R.id.tv_score);
                tvRemarks = v.findViewById(R.id.tv_remarks);
            }
        }
    }

    private class WeeklyReportsAdapter extends RecyclerView.Adapter<WeeklyReportsAdapter.VH> {
        private List<WeeklyReportItem> reports;
        WeeklyReportsAdapter(List<WeeklyReportItem> reports) { this.reports = reports; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weekly_report_parent, parent, false);
            TextSizeHelper.applyScaleRecursively(v);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            WeeklyReportItem r = reports.get(position);
            h.tvWeek.setText(r.getWeekLabel() != null ? r.getWeekLabel() : "Unknown week");
            h.tvStats.setText(String.format(Locale.getDefault(),
                    "Attendance: %d%% | Academic: %d%%\nBehavior: %d%% | Participation: %d%%",
                    r.getAttendance(), r.getAcademic(), r.getBehavior(), r.getParticipation()));
            h.tvNotes.setText(r.getNotes() != null && !r.getNotes().isEmpty() ? "Notes: " + r.getNotes() : "");
            h.tvObservations.setText(r.getObservations() != null && !r.getObservations().isEmpty() ? "Observations: " + r.getObservations() : "");
        }

        @Override
        public int getItemCount() { return reports.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvWeek, tvStats, tvNotes, tvObservations;
            VH(@NonNull View v) {
                super(v);
                tvWeek = v.findViewById(R.id.tv_week);
                tvStats = v.findViewById(R.id.tv_stats);
                tvNotes = v.findViewById(R.id.tv_notes);
                tvObservations = v.findViewById(R.id.tv_observations);
            }
        }
    }
}