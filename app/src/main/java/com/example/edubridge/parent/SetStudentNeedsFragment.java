package com.example.edubridge.parent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetStudentNeedsFragment extends Fragment {

    private Spinner spinnerStudent;
    private TextInputEditText etDietary, etSleep, etBathroom;
    private TextInputEditText etDietaryNotes, etSleepNotes, etBathroomNotes;
    private MaterialButton btnSave;
    private FirebaseFirestore db;
    private String parentId;
    private List<ParentStudentReportFragment.StudentInfo> studentList = new ArrayList<>();
    private String selectedStudentId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_set_student_needs, container, false);
        TextSizeHelper.applyScaleRecursively(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        parentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        spinnerStudent = view.findViewById(R.id.spinner_student);
        etDietary = view.findViewById(R.id.et_dietary_needs);
        etSleep = view.findViewById(R.id.et_sleep_needs);
        etBathroom = view.findViewById(R.id.et_bathroom_needs);
        
        etDietaryNotes = view.findViewById(R.id.et_dietary_notes);
        etSleepNotes = view.findViewById(R.id.et_sleep_notes);
        etBathroomNotes = view.findViewById(R.id.et_bathroom_notes);
        
        btnSave = view.findViewById(R.id.btn_save_needs);

        loadStudents();

        btnSave.setOnClickListener(v -> saveNeeds());
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
                        ParentStudentReportFragment.StudentInfo student = new ParentStudentReportFragment.StudentInfo();
                        String id = doc.getString("studentId");
                        if (id == null) id = doc.getId();
                        student.setId(id);
                        student.setName(doc.getString("name"));
                        studentList.add(student);
                        studentNames.add(student.getName());
                    }

                    if (!studentList.isEmpty()) {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_spinner_dropdown_item, studentNames);
                        spinnerStudent.setAdapter(adapter);

                        spinnerStudent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                selectedStudentId = studentList.get(position).getId();
                                loadCurrentNeeds();
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                    }
                });
    }

    private void loadCurrentNeeds() {
        if (selectedStudentId == null) return;
        db.collection("studentNeeds").document(selectedStudentId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        etDietary.setText(doc.getString("dietary"));
                        etSleep.setText(doc.getString("sleep"));
                        etBathroom.setText(doc.getString("bathroom"));
                        
                        etDietaryNotes.setText(doc.getString("dietaryNotes"));
                        etSleepNotes.setText(doc.getString("sleepNotes"));
                        etBathroomNotes.setText(doc.getString("bathroomNotes"));
                    } else {
                        etDietary.setText("");
                        etSleep.setText("");
                        etBathroom.setText("");
                        etDietaryNotes.setText("");
                        etSleepNotes.setText("");
                        etBathroomNotes.setText("");
                    }
                });
    }

    private void saveNeeds() {
        if (selectedStudentId == null) {
            Toast.makeText(getContext(), "Please select a student", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> needs = new HashMap<>();
        needs.put("dietary", etDietary.getText().toString());
        needs.put("sleep", etSleep.getText().toString());
        needs.put("bathroom", etBathroom.getText().toString());
        
        needs.put("dietaryNotes", etDietaryNotes.getText().toString());
        needs.put("sleepNotes", etSleepNotes.getText().toString());
        needs.put("bathroomNotes", etBathroomNotes.getText().toString());

        needs.put("lastUpdated", com.google.firebase.Timestamp.now());

        db.collection("studentNeeds").document(selectedStudentId)
                .set(needs)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), R.string.needs_saved, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(getContext(), getString(R.string.needs_save_failed, e.getMessage()), Toast.LENGTH_SHORT).show());
    }
}
