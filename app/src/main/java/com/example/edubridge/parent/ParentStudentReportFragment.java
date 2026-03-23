package com.example.edubridge.parent;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ParentStudentReportFragment extends Fragment {

    private Spinner spinnerStudent, spinnerTerm;
    private TabLayout tabLayout;
    private TextView tvStudentName, tvStudentClass, tvStudentRoll, tvAttendancePercentage;
    private RecyclerView subjectsRecyclerView, attendanceRecyclerView;
    private MaterialCardView cardAttendance, cardPerformance;
    private MaterialButton btnExportPdf;

    private FirebaseFirestore db;
    private String parentId;
    private List<StudentInfo> studentList;
    private String selectedStudentId;
    private String selectedTerm = "Term 1";
    private List<SubjectGrade> currentGrades = new ArrayList<>();
    private List<AttendanceRecord> currentAttendance = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parent_student_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please login first", Toast.LENGTH_SHORT).show();
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
        cardAttendance = view.findViewById(R.id.card_attendance);
        cardPerformance = view.findViewById(R.id.card_performance);

        // الحصول على مرجع زر التصدير من XML
        btnExportPdf = view.findViewById(R.id.btn_export_pdf);
    }

    private void setupSpinners() {
        String[] terms = {"Term 1", "Term 2", "Term 3", "Annual"};
        ArrayAdapter<String> termAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, terms);
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
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Attendance"));
        tabLayout.addTab(tabLayout.newTab().setText("Performance"));
        tabLayout.addTab(tabLayout.newTab().setText("Reports"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        cardAttendance.setVisibility(View.VISIBLE);
                        cardPerformance.setVisibility(View.GONE);
                        break;
                    case 1:
                        cardAttendance.setVisibility(View.GONE);
                        cardPerformance.setVisibility(View.VISIBLE);
                        loadPerformanceData();
                        break;
                    case 2:
                        Toast.makeText(getContext(), "Reports feature coming soon", Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadStudents() {
        db.collection("users")
                .whereEqualTo("usertype", "student")
                .whereEqualTo("parentId", parentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentList.clear();
                    List<String> studentNames = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        StudentInfo student = new StudentInfo();
                        student.setId(doc.getId());
                        student.setName(doc.getString("fullname"));
                        student.setClassName(doc.getString("class"));
                        student.setRollNumber(doc.getString("rollNumber"));

                        if (student.getName() == null) student.setName("Unknown");
                        if (student.getClassName() == null) student.setClassName("N/A");
                        if (student.getRollNumber() == null) student.setRollNumber("N/A");

                        studentList.add(student);
                        studentNames.add(student.getName());
                    }

                    if (studentList.isEmpty()) {
                        Toast.makeText(getContext(), "No students found", Toast.LENGTH_SHORT).show();
                        addDummyData();
                    } else {
                        ArrayAdapter<String> studentAdapter = new ArrayAdapter<>(
                                requireContext(), android.R.layout.simple_spinner_dropdown_item, studentNames);
                        spinnerStudent.setAdapter(studentAdapter);
                        spinnerStudent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                selectedStudentId = studentList.get(position).getId();
                                displayStudentInfo(studentList.get(position));
                                loadStudentReport();
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading students: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    addDummyData();
                });
    }

    private void addDummyData() {
        // إضافة بيانات تجريبية
        StudentInfo dummyStudent = new StudentInfo();
        dummyStudent.setId("dummy_id");
        dummyStudent.setName("Ahmed Mohamed");
        dummyStudent.setClassName("5A");
        dummyStudent.setRollNumber("12345");
        studentList.add(dummyStudent);

        selectedStudentId = dummyStudent.getId();
        displayStudentInfo(dummyStudent);

        // إضافة بيانات حضور تجريبية
        currentAttendance.clear();
        for (int i = 0; i < 10; i++) {
            AttendanceRecord record = new AttendanceRecord();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -i);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            record.setDate(sdf.format(cal.getTime()));
            record.setPresent(i < 7);
            currentAttendance.add(record);
        }

        int presentCount = 0;
        for (AttendanceRecord r : currentAttendance) {
            if (r.isPresent()) presentCount++;
        }
        int percentage = (presentCount * 100) / currentAttendance.size();
        tvAttendancePercentage.setText(percentage + "%");

        AttendanceAdapter attendanceAdapter = new AttendanceAdapter(currentAttendance);
        attendanceRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        attendanceRecyclerView.setAdapter(attendanceAdapter);

        // إضافة بيانات أداء تجريبية
        currentGrades.clear();
        currentGrades.add(createSubjectGrade("Mathematics", "A", 92.5, "Excellent"));
        currentGrades.add(createSubjectGrade("English", "B+", 88.0, "Good"));
        currentGrades.add(createSubjectGrade("Science", "A-", 90.0, "Very Good"));
        currentGrades.add(createSubjectGrade("Arabic", "B", 85.5, "Satisfactory"));

        PerformanceAdapter performanceAdapter = new PerformanceAdapter(currentGrades);
        subjectsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        subjectsRecyclerView.setAdapter(performanceAdapter);
    }

    private void displayStudentInfo(StudentInfo student) {
        tvStudentName.setText(student.getName());
        tvStudentClass.setText("Class: " + (student.getClassName() != null ? student.getClassName() : "N/A"));
        tvStudentRoll.setText("Roll: " + (student.getRollNumber() != null ? student.getRollNumber() : "N/A"));
    }

    private void loadStudentReport() {
        loadAttendanceData();
        loadPerformanceData();
    }

    private void loadAttendanceData() {
        String className = tvStudentClass.getText().toString().replace("Class: ", "");

        db.collection("attendance")
                .whereEqualTo("class", className)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    currentAttendance.clear();
                    int presentCount = 0;
                    int totalCount = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        List<Map<String, Object>> students = (List<Map<String, Object>>) doc.get("students");
                        if (students != null) {
                            for (Map<String, Object> student : students) {
                                if (selectedStudentId.equals(student.get("studentId"))) {
                                    Boolean present = (Boolean) student.get("present");
                                    String date = doc.getString("date");

                                    AttendanceRecord record = new AttendanceRecord();
                                    record.setDate(date != null ? date : "Unknown");
                                    record.setPresent(present != null && present);
                                    currentAttendance.add(record);

                                    if (present != null && present) presentCount++;
                                    totalCount++;
                                    break;
                                }
                            }
                        }
                    }

                    if (currentAttendance.isEmpty()) {
                        // إضافة بيانات تجريبية إذا لم توجد بيانات
                        for (int i = 0; i < 10; i++) {
                            AttendanceRecord record = new AttendanceRecord();
                            Calendar cal = Calendar.getInstance();
                            cal.add(Calendar.DAY_OF_YEAR, -i);
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            record.setDate(sdf.format(cal.getTime()));
                            record.setPresent(i < 7);
                            currentAttendance.add(record);
                            if (i < 7) presentCount++;
                            totalCount++;
                        }
                    }

                    if (totalCount > 0) {
                        int percentage = (presentCount * 100) / totalCount;
                        tvAttendancePercentage.setText(percentage + "%");
                    } else {
                        tvAttendancePercentage.setText("N/A");
                    }

                    AttendanceAdapter adapter = new AttendanceAdapter(currentAttendance);
                    attendanceRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    attendanceRecyclerView.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading attendance: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    // إضافة بيانات تجريبية في حالة الفشل
                    currentAttendance.clear();
                    for (int i = 0; i < 10; i++) {
                        AttendanceRecord record = new AttendanceRecord();
                        Calendar cal = Calendar.getInstance();
                        cal.add(Calendar.DAY_OF_YEAR, -i);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        record.setDate(sdf.format(cal.getTime()));
                        record.setPresent(i < 7);
                        currentAttendance.add(record);
                    }

                    tvAttendancePercentage.setText("70%");
                    AttendanceAdapter adapter = new AttendanceAdapter(currentAttendance);
                    attendanceRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    attendanceRecyclerView.setAdapter(adapter);
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

                    if (currentGrades.isEmpty()) {
                        // إضافة بيانات تجريبية
                        currentGrades.add(createSubjectGrade("Mathematics", "A", 92.5, "Excellent"));
                        currentGrades.add(createSubjectGrade("English", "B+", 88.0, "Good"));
                        currentGrades.add(createSubjectGrade("Science", "A-", 90.0, "Very Good"));
                        currentGrades.add(createSubjectGrade("Arabic", "B", 85.5, "Satisfactory"));
                    }

                    PerformanceAdapter adapter = new PerformanceAdapter(currentGrades);
                    subjectsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    subjectsRecyclerView.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading performance: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    // إضافة بيانات تجريبية في حالة الفشل
                    currentGrades.clear();
                    currentGrades.add(createSubjectGrade("Mathematics", "A", 92.5, "Excellent"));
                    currentGrades.add(createSubjectGrade("English", "B+", 88.0, "Good"));
                    currentGrades.add(createSubjectGrade("Science", "A-", 90.0, "Very Good"));
                    currentGrades.add(createSubjectGrade("Arabic", "B", 85.5, "Satisfactory"));

                    PerformanceAdapter adapter = new PerformanceAdapter(currentGrades);
                    subjectsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                    subjectsRecyclerView.setAdapter(adapter);
                });
    }

    private SubjectGrade createSubjectGrade(String subject, String grade, double score, String remarks) {
        SubjectGrade sg = new SubjectGrade();
        sg.setSubject(subject);
        sg.setGrade(grade);
        sg.setScore(score);
        sg.setRemarks(remarks);
        return sg;
    }

    private void exportReportAsPdf() {
        if (studentList.isEmpty() || selectedStudentId == null) {
            Toast.makeText(getContext(), "No student data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // إنشاء اسم الملف
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String fileName = "Student_Report_" + tvStudentName.getText().toString().replace(" ", "_")
                    + "_" + sdf.format(new Date()) + ".pdf";

            File pdfFile;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // لأندرويد 10 وأعلى
                pdfFile = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
            } else {
                // للإصدارات الأقدم
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                pdfFile = new File(downloadsDir, fileName);
            }

            // إنشاء ملف PDF
            PdfWriter writer = new PdfWriter(new FileOutputStream(pdfFile));
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // إضافة المحتوى
            addPdfContent(document);

            document.close();

            Toast.makeText(getContext(), "PDF saved: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // مشاركة الملف
            sharePdf(pdfFile);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error creating PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addPdfContent(Document document) throws Exception {
        // إنشاء الخطوط
        PdfFont boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont normalFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        // العنوان الرئيسي
        Paragraph title = new Paragraph("Student Academic Report")
                .setFont(boldFont)
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        // اسم المدرسة
        Paragraph school = new Paragraph("EduBridge International School")
                .setFont(normalFont)
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(30);
        document.add(school);

        // جدول معلومات الطالب
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        addInfoRow(infoTable, "Student Name:", tvStudentName.getText().toString(), boldFont, normalFont);
        addInfoRow(infoTable, "Class:", tvStudentClass.getText().toString(), boldFont, normalFont);
        addInfoRow(infoTable, "Roll Number:", tvStudentRoll.getText().toString(), boldFont, normalFont);
        addInfoRow(infoTable, "Term:", selectedTerm, boldFont, normalFont);
        addInfoRow(infoTable, "Report Date:", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()), boldFont, normalFont);

        document.add(infoTable);

        // ملخص الحضور
        Paragraph attendanceTitle = new Paragraph("Attendance Summary")
                .setFont(boldFont)
                .setFontSize(16)
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(attendanceTitle);

        // إحصائيات الحضور
        int totalDays = currentAttendance.size();
        int presentDays = 0;
        for (AttendanceRecord record : currentAttendance) {
            if (record.isPresent()) presentDays++;
        }
        double attendancePercentage = totalDays > 0 ? (presentDays * 100.0 / totalDays) : 0;

        Paragraph attendanceStats = new Paragraph(String.format(Locale.getDefault(),
                "Total Days: %d | Present: %d | Absent: %d | Percentage: %.1f%%",
                totalDays, presentDays, totalDays - presentDays, attendancePercentage))
                .setFont(normalFont)
                .setFontSize(12)
                .setMarginBottom(10);
        document.add(attendanceStats);

        // جدول تفاصيل الحضور
        Table attendanceTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        // رؤوس الجدول
        addTableHeader(attendanceTable, boldFont, "Date", "Status");

        // إضافة سجلات الحضور
        for (AttendanceRecord record : currentAttendance) {
            attendanceTable.addCell(new Cell().add(new Paragraph(record.getDate() != null ? record.getDate() : "Unknown")
                    .setFont(normalFont).setFontSize(10)));

            String status = record.isPresent() ? "Present" : "Absent";
            Cell statusCell = new Cell().add(new Paragraph(status).setFont(normalFont).setFontSize(10));
            if (record.isPresent()) {
                statusCell.setFontColor(new DeviceRgb(0, 128, 0));
            } else {
                statusCell.setFontColor(new DeviceRgb(255, 0, 0));
            }
            attendanceTable.addCell(statusCell);
        }
        document.add(attendanceTable);

        // الأداء الأكاديمي
        Paragraph performanceTitle = new Paragraph("Academic Performance - " + selectedTerm)
                .setFont(boldFont)
                .setFontSize(16)
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(performanceTitle);

        // جدول الأداء
        Table performanceTable = new Table(UnitValue.createPercentArray(new float[]{40, 15, 20, 25}))
                .setWidth(UnitValue.createPercentValue(100));

        // رؤوس الجدول
        addTableHeader(performanceTable, boldFont, "Subject", "Grade", "Score (%)", "Remarks");
        // إضافة المواد
        double totalScore = 0;
        for (SubjectGrade grade : currentGrades) {
            performanceTable.addCell(new Cell().add(new Paragraph(grade.getSubject() != null ? grade.getSubject() : "Unknown")
                    .setFont(normalFont).setFontSize(10)));

            performanceTable.addCell(new Cell().add(new Paragraph(grade.getGrade() != null ? grade.getGrade() : "N/A")
                            .setFont(normalFont).setFontSize(10))
                    .setTextAlignment(TextAlignment.CENTER));

            performanceTable.addCell(new Cell().add(new Paragraph(String.format(Locale.getDefault(), "%.1f",
                            grade.getScore() != null ? grade.getScore() : 0.0))
                            .setFont(normalFont).setFontSize(10))
                    .setTextAlignment(TextAlignment.CENTER));

            performanceTable.addCell(new Cell().add(new Paragraph(grade.getRemarks() != null ? grade.getRemarks() : "")
                    .setFont(normalFont).setFontSize(10)));

            totalScore += grade.getScore() != null ? grade.getScore() : 0;
        }

        // إضافة صف المعدل
        double average = currentGrades.size() > 0 ? totalScore / currentGrades.size() : 0;
        performanceTable.addCell(new Cell(1, 4)
                .add(new Paragraph(String.format(Locale.getDefault(), "Average: %.1f%%", average))
                        .setFont(boldFont).setFontSize(11).setTextAlignment(TextAlignment.RIGHT))
                .setBorderTop(new SolidBorder(1)));

        document.add(performanceTable);

        // تذييل
        Paragraph footer = new Paragraph("Generated by EduBridge System")
                .setFont(normalFont)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30);
        document.add(footer);
    }

    private void addInfoRow(Table table, String label, String value, PdfFont boldFont, PdfFont normalFont) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(boldFont).setFontSize(11))
                .setBorder(Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(value != null ? value : "N/A").setFont(normalFont).setFontSize(11))
                .setBorder(Border.NO_BORDER));
    }

    private void addTableHeader(Table table, PdfFont boldFont, String... headers) {
        for (String header : headers) {
            Cell cell = new Cell().add(new Paragraph(header)
                    .setFont(boldFont)
                    .setFontSize(11)
                    .setTextAlignment(TextAlignment.CENTER));
            cell.setBackgroundColor(new DeviceRgb(200, 200, 200));
            table.addCell(cell);
        }
    }

    private void sharePdf(File pdfFile) {
        Uri fileUri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".provider", pdfFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Student Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Student Academic Report for " + tvStudentName.getText().toString());
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share PDF Report"));
    }

    // Model Classes
    public static class StudentInfo {
        private String id;
        private String name;
        private String className;
        private String rollNumber;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public String getRollNumber() { return rollNumber; }
        public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    }

    public static class AttendanceRecord {
        private String date;
        private boolean present;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public boolean isPresent() { return present; }
        public void setPresent(boolean present) { this.present = present; }
    }

    public static class SubjectGrade {
        private String subject;
        private String grade;
        private Double score;
        private String remarks;

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }

    // Adapters
    private class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {
        private List<AttendanceRecord> records;

        public AttendanceAdapter(List<AttendanceRecord> records) {
            this.records = records;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_attendance_record, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AttendanceRecord record = records.get(position);
            holder.tvDate.setText(record.getDate() != null ? record.getDate() : "Unknown");
            holder.tvStatus.setText(record.isPresent() ? "Present" : "Absent");
            holder.tvStatus.setTextColor(record.isPresent() ?
                    getResources().getColor(R.color.present_green) :
                    getResources().getColor(R.color.absent_red));
        }

        @Override
        public int getItemCount() {
            return records.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDate, tvStatus;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDate = itemView.findViewById(R.id.tv_date);
                tvStatus = itemView.findViewById(R.id.tv_status);
            }
        }
    }

    private class PerformanceAdapter extends RecyclerView.Adapter<PerformanceAdapter.ViewHolder> {
        private List<SubjectGrade> grades;

        public PerformanceAdapter(List<SubjectGrade> grades) {
            this.grades = grades;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_subject_grade, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            SubjectGrade grade = grades.get(position);
            holder.tvSubject.setText(grade.getSubject() != null ? grade.getSubject() : "Unknown");
            holder.tvGrade.setText(grade.getGrade() != null ? grade.getGrade() : "N/A");
            holder.tvScore.setText(String.format(Locale.getDefault(), "%.1f%%",
                    grade.getScore() != null ? grade.getScore() : 0.0));
            holder.tvRemarks.setText(grade.getRemarks() != null ? grade.getRemarks() : "");
        }

        @Override
        public int getItemCount() {
            return grades.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvSubject, tvGrade, tvScore, tvRemarks;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSubject = itemView.findViewById(R.id.tv_subject);
                tvGrade = itemView.findViewById(R.id.tv_grade);
                tvScore = itemView.findViewById(R.id.tv_score);
                tvRemarks = itemView.findViewById(R.id.tv_remarks);
            }
        }
    }
}