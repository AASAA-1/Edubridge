package com.example.edubridge.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminCurriculumModificationFragment extends Fragment {

    private Spinner gradeSpinner;
    private EditText nameEdit;
    private EditText booksEdit;
    private EditText linkEdit;

    private Button saveButton;

    private FirebaseFirestore db;

    public AdminCurriculumModificationFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_curriculum_modification, container, false);

        gradeSpinner = view.findViewById(R.id.grade_spinner);
        nameEdit = view.findViewById(R.id.curriculum_name_edit);
        booksEdit = view.findViewById(R.id.book_references_edit);
        linkEdit = view.findViewById(R.id.link_edit);

        saveButton = view.findViewById(R.id.save_curriculum_button);

        db = FirebaseFirestore.getInstance();

        setupGradeSpinner();

        saveButton.setOnClickListener(v -> saveCurriculum());

        return view;
    }

    private void setupGradeSpinner() {

        List<String> grades = new ArrayList<>();

        for (int i = 1; i <= 12; i++) {
            grades.add(String.valueOf(i));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                grades
        );

        gradeSpinner.setAdapter(adapter);
    }

    private void saveCurriculum() {

        String grade = gradeSpinner.getSelectedItem().toString();
        String name = nameEdit.getText().toString().trim();
        String books = booksEdit.getText().toString().trim();
        String link = linkEdit.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getContext(), "Name required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();

        data.put("grade", grade);
        data.put("name", name);
        data.put("books", books);
        data.put("link", link);

        db.collection("curriculum")
                .add(data)
                .addOnSuccessListener(doc -> {

                    Toast.makeText(getContext(), "Curriculum saved", Toast.LENGTH_SHORT).show();

                    requireActivity()
                            .getSupportFragmentManager()
                            .popBackStack();
                });
    }
}