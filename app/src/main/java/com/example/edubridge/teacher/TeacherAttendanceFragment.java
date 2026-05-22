package com.example.edubridge.teacher;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TeacherAttendanceFragment extends Fragment {

    private RecyclerView studentsRecyclerView;
    private StudentAttendanceAdapter adapter;
    private List<StudentAttendance> studentList;
    private Spinner classSpinner, dateSpinner;
    private MaterialButton btnSaveAttendance, btnViewHistory;
    private TextView tvSelectedDate, tvClassStatus;
    private FirebaseFirestore db;
    private String currentTeacherId;
    private String selectedClassId = null;
    private String selectedClassName = null;
    private String selectedDate;

    private final List<String> classNames = new ArrayList<>();
    private final List<String> classIds = new ArrayList<>();

    private static final String TAG = "TeacherAttendance";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_teacher_attendance, container, false);
        TextSizeHelper.applyScaleRecursively(v);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        currentTeacherId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        studentsRecyclerView = view.findViewById(R.id.students_recycler_view);
        classSpinner = view.findViewById(R.id.class_spinner);
        dateSpinner = view.findViewById(R.id.date_spinner);
        btnSaveAttendance = view.findViewById(R.id.btn_save_attendance);
        btnViewHistory = view.findViewById(R.id.btn_view_history);
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        tvClassStatus = view.findViewById(R.id.tv_class_status);

        studentList = new ArrayList<>();
        adapter = new StudentAttendanceAdapter(studentList);
        studentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        studentsRecyclerView.setAdapter(adapter);

        setupDateSpinner();
        setupCurrentDate();
        fetchAssignedClasses();

        btnSaveAttendance.setOnClickListener(v -> saveAttendance());
        btnViewHistory.setOnClickListener(v -> viewAttendanceHistory());
    }

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
                        tvClassStatus.setVisibility(View.VISIBLE);
                        tvClassStatus.setText(getString(R.string.no_classes_assigned));
                        classSpinner.setVisibility(View.GONE);
                    } else {
                        tvClassStatus.setVisibility(View.GONE);
                        classSpinner.setVisibility(View.VISIBLE);
                        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_spinner_dropdown_item,
                                classNames);
                        classSpinner.setAdapter(classAdapter);
                        classSpinner.setEnabled(true);
                        setupClassSpinnerListener();

                        // Auto-select first class
                        if (!classNames.isEmpty()) {
                            selectedClassName = classNames.get(0);
                            selectedClassId = classIds.get(0);
                            loadStudentsFromParentSubcollections();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null || !isAdded()) return;
                    tvClassStatus.setVisibility(View.VISIBLE);
                    tvClassStatus.setText(getString(R.string.classes_load_failed));
                    classSpinner.setVisibility(View.GONE);
                });
    }

    private void setupClassSpinnerListener() {
        classSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= classIds.size()) return;
                selectedClassName = classNames.get(position);
                selectedClassId = classIds.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupDateSpinner() {
        String[] dates = {"Today", "Yesterday", "Custom"};
        ArrayAdapter<String> dateAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, dates);
        dateSpinner.setAdapter(dateAdapter);
    }

    private void setupCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault());
        selectedDate = sdf.format(new Date());
        tvSelectedDate.setText(selectedDate);
    }

    /**
     * NEW METHOD: Load students by searching through parents' students subcollections
     * This matches how the parent dashboard finds linked students
     */
    private void loadStudentsFromParentSubcollections() {
        if (selectedClassName == null || selectedClassId == null) return;

        Log.d(TAG, "Loading students for class: " + selectedClassName + " (ID: " + selectedClassId + ")");

        // First, get all parent users
        db.collection("users")
                .whereEqualTo("usertype", "parent")
                .get()
                .addOnSuccessListener(parentSnapshots -> {
                    studentList.clear();
                    final int[] totalParents = {parentSnapshots.size()};
                    final int[] processedParents = {0};

                    Log.d(TAG, "Found " + totalParents[0] + " parents to check");

                    if (totalParents[0] == 0) {
                        checkStudentList();
                        return;
                    }

                    for (DocumentSnapshot parentDoc : parentSnapshots) {
                        // Get students subcollection for each parent
                        parentDoc.getReference().collection("students")
                                .get()
                                .addOnSuccessListener(studentSnapshots -> {
                                    Log.d(TAG, "Parent " + parentDoc.getId() + " has " + studentSnapshots.size() + " students");

                                    for (DocumentSnapshot studentDoc : studentSnapshots) {
                                        String studentClass = studentDoc.getString("class");
                                        String studentClassId = studentDoc.getString("classId");

                                        Log.d(TAG, "Student: " + studentDoc.getString("name") +
                                                " - Class: " + studentClass +
                                                " - ClassID: " + studentClassId);

                                        // Check if student belongs to selected class
                                        // Match by classId first (more reliable), then by class name
                                        if ((studentClassId != null && studentClassId.equals(selectedClassId)) ||
                                                (studentClass != null && studentClass.equalsIgnoreCase(selectedClassName))) {

                                            StudentAttendance student = new StudentAttendance();
                                            student.setId(studentDoc.getString("studentId")); // Use studentId field
                                            student.setName(studentDoc.getString("name"));
                                            student.setRollNumber(studentDoc.getString("studentId")); // Use studentId as roll number
                                            student.setPresent(true);
                                            studentList.add(student);

                                            Log.d(TAG, "Added student: " + student.getName() + " (ID: " + student.getId() + ")");
                                        }
                                    }

                                    processedParents[0]++;

                                    // When all parents are processed, update UI
                                    if (processedParents[0] >= totalParents[0]) {
                                        checkStudentList();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error getting students for parent: " + e.getMessage());
                                    processedParents[0]++;

                                    if (processedParents[0] >= totalParents[0]) {
                                        checkStudentList();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting parents: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to load students: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void checkStudentList() {
        requireActivity().runOnUiThread(() -> {
            adapter.notifyDataSetChanged();

            if (studentList.isEmpty()) {
                Toast.makeText(getContext(),
                        "No students found in " + selectedClassName,
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(),
                        "Loaded " + studentList.size() + " students from " + selectedClassName,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAttendance() {
        if (selectedClassName == null) {
            Toast.makeText(getContext(), "Please select a class first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (studentList.isEmpty()) {
            Toast.makeText(getContext(), "No students to save attendance for", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> attendanceData = new HashMap<>();
        attendanceData.put("teacherId", currentTeacherId);
        attendanceData.put("class", selectedClassName);
        attendanceData.put("subject", selectedClassName);
        attendanceData.put("classId", selectedClassId);
        attendanceData.put("date", selectedDate);
        attendanceData.put("timestamp", new Date());

        List<Map<String, Object>> studentAttendance = new ArrayList<>();
        for (StudentAttendance student : studentList) {
            Map<String, Object> studentRecord = new HashMap<>();
            studentRecord.put("studentId", student.getId());
            studentRecord.put("name", student.getName());
            studentRecord.put("rollNumber", student.getRollNumber());
            studentRecord.put("status", student.isPresent() ? "present" : "absent");
            studentRecord.put("present", student.isPresent());
            studentAttendance.add(studentRecord);
        }
        attendanceData.put("students", studentAttendance);

        db.collection("attendance")
                .add(attendanceData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), getString(R.string.attendance_saved),
                            Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Attendance saved successfully for class: " + selectedClassName);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), getString(R.string.attendance_save_error, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to save attendance: " + e.getMessage());
                });
    }

    private void viewAttendanceHistory() {
        if (selectedClassName == null) {
            Toast.makeText(getContext(), "Please select a class first", Toast.LENGTH_SHORT).show();
            return;
        }

        AttendanceHistoryFragment fragment = new AttendanceHistoryFragment();
        Bundle args = new Bundle();
        args.putString("class", selectedClassName);
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // StudentAttendance model class
    public static class StudentAttendance {
        private String id;
        private String name;
        private String rollNumber;
        private boolean isPresent;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRollNumber() { return rollNumber; }
        public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
        public boolean isPresent() { return isPresent; }
        public void setPresent(boolean present) { isPresent = present; }
    }

    // Adapter class
    private class StudentAttendanceAdapter extends RecyclerView.Adapter<StudentAttendanceAdapter.ViewHolder> {
        private List<StudentAttendance> students;

        public StudentAttendanceAdapter(List<StudentAttendance> students) {
            this.students = students;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_student_attendance, parent, false);
            TextSizeHelper.applyScaleRecursively(view);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StudentAttendance student = students.get(position);
            holder.tvStudentName.setText(student.getName());
            holder.tvRollNumber.setText("Roll: " + (student.getRollNumber() != null ? student.getRollNumber() : "N/A"));

            holder.cardPresent.setCardBackgroundColor(student.isPresent() ?
                    getResources().getColor(R.color.present_green) :
                    getResources().getColor(R.color.absent_red));

            holder.cardPresent.setOnClickListener(v -> {
                student.setPresent(!student.isPresent());
                notifyItemChanged(position);
            });
        }

        @Override
        public int getItemCount() {
            return students.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvStudentName, tvRollNumber;
            MaterialCardView cardPresent;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvStudentName = itemView.findViewById(R.id.tv_student_name);
                tvRollNumber = itemView.findViewById(R.id.tv_roll_number);
                cardPresent = itemView.findViewById(R.id.card_present);
            }
        }
    }
}