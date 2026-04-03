package com.example.edubridge.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.edubridge.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminClassModificationFragment extends Fragment {

    private static final String TAG = "AdminClassMod";
    private EditText classNameEdit;
    private Spinner gradeSpinner, teacherSpinner;
    private Button saveButton, deleteButton, manageStudentsButton, manageScheduleButton;
    private FirebaseFirestore db;
    private String classId;
    private boolean isNewClass = false;
    private List<TeacherData> teacherList = new ArrayList<>();
    private String[] grades = {"Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5", "Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"};
    private String pendingTeacherId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_class_modification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        classNameEdit = view.findViewById(R.id.class_name_edit);
        gradeSpinner = view.findViewById(R.id.modification_grade_spinner);
        teacherSpinner = view.findViewById(R.id.teacher_spinner);
        saveButton = view.findViewById(R.id.save_class_button);
        deleteButton = view.findViewById(R.id.delete_class_button);
        manageStudentsButton = view.findViewById(R.id.btn_manage_students);
        manageScheduleButton = view.findViewById(R.id.btn_manage_schedule);

        if (getArguments() != null) {
            classId = getArguments().getString("classId");
        }

        setupGradeSpinner();
        loadTeachers();

        TextView title = view.findViewById(R.id.modification_title);
        
        if (classId != null) {
            // Edit Mode
            loadClassData();
            deleteButton.setVisibility(View.VISIBLE);
            if (title != null) title.setText("Edit Class");
        } else {
            // Add Mode
            isNewClass = true;
            // Pre-generate ID so we can manage students and schedule before the first save
            classId = db.collection("classes").document().getId();
            deleteButton.setVisibility(View.GONE);
            if (title != null) title.setText("Add Class");
        }

        // Always show manage buttons
        manageStudentsButton.setVisibility(View.VISIBLE);
        manageScheduleButton.setVisibility(View.VISIBLE);

        saveButton.setOnClickListener(v -> saveClass());
        deleteButton.setOnClickListener(v -> deleteClass());
        
        manageStudentsButton.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, AdminClassStudentManagementFragment.newInstance(classId))
                    .addToBackStack(null)
                    .commit();
        });

        manageScheduleButton.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, AdminClassScheduleManagementFragment.newInstance(classId))
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupGradeSpinner() {
        gradeSpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, grades));
    }

    private void loadTeachers() {
        db.collection("users").whereEqualTo("usertype", "teacher").get()
                .addOnSuccessListener(querySnapshot -> {
                    teacherList.clear();
                    List<String> teacherNames = new ArrayList<>();
                    
                    teacherList.add(new TeacherData(null, "No teacher assigned"));
                    teacherNames.add("No teacher assigned");

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String name = doc.getString("fullname");
                        String id = doc.getId();
                        teacherList.add(new TeacherData(id, name));
                        teacherNames.add(name != null ? name : "Unknown");
                    }

                    if (isAdded()) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, teacherNames);
                        teacherSpinner.setAdapter(adapter);

                        if (pendingTeacherId != null) {
                            setTeacherSelection(pendingTeacherId);
                        }
                    }
                });
    }

    private void loadClassData() {
        db.collection("classes").document(classId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        classNameEdit.setText(doc.getString("name"));
                        String grade = doc.getString("grade");
                        if (grade != null) {
                            for (int i = 0; i < grades.length; i++) {
                                if (grades[i].equals(grade)) {
                                    gradeSpinner.setSelection(i);
                                    break;
                                }
                            }
                        }
                        
                        String teacherId = doc.getString("teacherId");
                        if (teacherId != null) {
                            if (teacherList.size() > 1) {
                                setTeacherSelection(teacherId);
                            } else {
                                pendingTeacherId = teacherId;
                            }
                        }
                    }
                });
    }

    private void setTeacherSelection(String teacherId) {
        for (int i = 0; i < teacherList.size(); i++) {
            if (teacherId.equals(teacherList.get(i).id)) {
                teacherSpinner.setSelection(i);
                break;
            }
        }
    }

    private void saveClass() {
        String name = classNameEdit.getText().toString().trim();
        if (name.isEmpty()) {
            classNameEdit.setError("Name required");
            return;
        }

        String grade = gradeSpinner.getSelectedItem().toString();
        int teacherPos = teacherSpinner.getSelectedItemPosition();
        TeacherData selectedTeacher = teacherList.get(teacherPos);

        Map<String, Object> classData = new HashMap<>();
        classData.put("name", name);
        classData.put("grade", grade);
        classData.put("teacherId", selectedTeacher.id);
        classData.put("teacherName", selectedTeacher.name);

        // Use SetOptions.merge() to avoid overwriting fields like 'schedule' or 'students'
        db.collection("classes").document(classId).set(classData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        String message = isNewClass ? "Class created" : "Class updated";
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, message + " ID: " + classId);
                        getParentFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save class", e);
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteClass() {
        if (classId != null) {
            Log.d(TAG, "Attempting to delete class: " + classId);
            db.collectionGroup("students")
                    .whereEqualTo("classId", classId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        Log.d(TAG, "Found " + querySnapshot.size() + " students to unlink");
                        WriteBatch batch = db.batch();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            batch.update(doc.getReference(), "classId", "");
                        }

                        batch.delete(db.collection("classes").document(classId));

                        batch.commit().addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Class and student unlinking successful");
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Class deleted", Toast.LENGTH_SHORT).show();
                                getParentFragmentManager().popBackStack();
                            }
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Batch commit failed", e);
                            if (isAdded()) {
                                Toast.makeText(getContext(), "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error querying students for deletion", e);
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private static class TeacherData {
        String id;
        String name;
        TeacherData(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
