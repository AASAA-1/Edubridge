package com.example.edubridge.teacher;

import android.os.Bundle;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
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
    private TextView tvSelectedDate;
    private FirebaseFirestore db;
    private String currentTeacherId;
    private String selectedClass = "Class 5A";
    private String selectedDate;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_attendance, container, false);
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

        studentList = new ArrayList<>();
        adapter = new StudentAttendanceAdapter(studentList);
        studentsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        studentsRecyclerView.setAdapter(adapter);

        setupSpinners();
        setupCurrentDate();
        loadStudents();

        btnSaveAttendance.setOnClickListener(v -> saveAttendance());
        btnViewHistory.setOnClickListener(v -> viewAttendanceHistory());
    }

    private void setupSpinners() {
        String[] classes = {"Class 5A", "Class 5B", "Class 6A", "Class 6B", "Class 7A"};
        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, classes);
        classSpinner.setAdapter(classAdapter);
        classSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedClass = classes[position];
                loadStudents();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

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

    private void loadStudents() {
        db.collection("users")
                .whereEqualTo("usertype", "student")
                .whereEqualTo("class", selectedClass)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        StudentAttendance student = new StudentAttendance();
                        student.setId(doc.getId());
                        student.setName(doc.getString("fullname"));
                        student.setRollNumber(doc.getString("rollNumber"));
                        student.setPresent(true); // Default present
                        studentList.add(student);
                    }
                    adapter.notifyDataSetChanged();

                    if (studentList.isEmpty()) {
                        Toast.makeText(getContext(), "No students found in " + selectedClass,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error loading students: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void saveAttendance() {
        Map<String, Object> attendanceData = new HashMap<>();
        attendanceData.put("teacherId", currentTeacherId);
        attendanceData.put("class", selectedClass);
        attendanceData.put("date", selectedDate);
        attendanceData.put("timestamp", new Date());

        List<Map<String, Object>> studentAttendance = new ArrayList<>();
        for (StudentAttendance student : studentList) {
            Map<String, Object> studentRecord = new HashMap<>();
            studentRecord.put("studentId", student.getId());
            studentRecord.put("name", student.getName());
            studentRecord.put("present", student.isPresent());
            studentRecord.put("rollNumber", student.getRollNumber());
            studentAttendance.add(studentRecord);
        }
        attendanceData.put("students", studentAttendance);

        db.collection("attendance")
                .add(attendanceData)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(getContext(), "Attendance saved successfully",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error saving attendance: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void viewAttendanceHistory() {
        AttendanceHistoryFragment fragment = new AttendanceHistoryFragment();
        Bundle args = new Bundle();
        args.putString("class", selectedClass);
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // Student Attendance Model
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

    // Adapter
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