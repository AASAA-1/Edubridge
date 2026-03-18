package com.example.edubridge.admin;

import android.os.Bundle;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminClassModificationFragment extends Fragment {

    private EditText classNameEdit;
    private Spinner gradeSpinner, teacherSpinner;
    private Button saveButton, deleteButton, manageStudentsButton;
    private FirebaseFirestore db;
    private String classId;
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

        if (getArguments() != null) {
            classId = getArguments().getString("classId");
        }

        setupGradeSpinner();
        loadTeachers();

        if (classId != null) {
            loadClassData();
            deleteButton.setVisibility(View.VISIBLE);
            manageStudentsButton.setVisibility(View.VISIBLE);
            TextView title = view.findViewById(R.id.modification_title);
            if (title != null) title.setText("Edit Class");
        }

        saveButton.setOnClickListener(v -> saveClass());
        deleteButton.setOnClickListener(v -> deleteClass());
        manageStudentsButton.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, AdminClassStudentManagementFragment.newInstance(classId))
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

        if (classId == null) {
            db.collection("classes").add(classData)
                    .addOnSuccessListener(documentReference -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Class created", Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().popBackStack();
                        }
                    });
        } else {
            db.collection("classes").document(classId).set(classData)
                    .addOnSuccessListener(aVoid -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Class updated", Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().popBackStack();
                        }
                    });
        }
    }

    private void deleteClass() {
        if (classId != null) {
            db.collection("classes").document(classId).delete()
                    .addOnSuccessListener(aVoid -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Class deleted", Toast.LENGTH_SHORT).show();
                            getParentFragmentManager().popBackStack();
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
