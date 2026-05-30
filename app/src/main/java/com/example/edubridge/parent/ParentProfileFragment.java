package com.example.edubridge.parent;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class ParentProfileFragment extends Fragment {

    private EditText fullNameEdit, phoneEdit, dobEdit, notesEdit, passwordEdit;
    private TextView emailText;
    private LinearLayout studentContainer;
    private Button saveButton;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String userId;

    private final Calendar selectedDob = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_parent_profile, container, false);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getContext(), getString(R.string.not_logged_in), Toast.LENGTH_SHORT).show();
            return v;
        }

        userId = currentUser.getUid();

        bindViews(v);
        setupToolbar(v);
        setupDatePicker();
        TextSizeHelper.applyScaleRecursively(v);
        setupListeners();
        loadData();

        return v;
    }

    private void bindViews(View v) {
        fullNameEdit = v.findViewById(R.id.full_name_edit);
        emailText = v.findViewById(R.id.email_text);
        phoneEdit = v.findViewById(R.id.phone_edit);
        dobEdit = v.findViewById(R.id.dob_edit);
        notesEdit = v.findViewById(R.id.notes_edit);
        passwordEdit = v.findViewById(R.id.password_edit);
        saveButton = v.findViewById(R.id.save_button);
        studentContainer = v.findViewById(R.id.student_container);
    }

    private void setupToolbar(View v) {
        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(view ->
                    requireActivity().getSupportFragmentManager().popBackStack());
        }
    }

    private void setupDatePicker() {
        dobEdit.setFocusable(false);
        dobEdit.setClickable(true);
        dobEdit.setOnClickListener(v -> {
            new DatePickerDialog(
                    requireContext(),
                    (view, year, month, day) -> {
                        selectedDob.set(year, month, day);
                        dobEdit.setText(String.format("%04d-%02d-%02d", year, month + 1, day));
                    },
                    selectedDob.get(Calendar.YEAR),
                    selectedDob.get(Calendar.MONTH),
                    selectedDob.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void setupListeners() {
        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void loadData() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) populateFields(doc);
                    loadStudents();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Failed to load profile: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void populateFields(DocumentSnapshot doc) {
        String fullName = doc.getString("fullName");
        if (fullName == null) fullName = doc.getString("fullname");
        fullNameEdit.setText(fullName);

        String email = doc.getString("email");
        if (email == null && currentUser != null) email = currentUser.getEmail();
        emailText.setText(email);

        phoneEdit.setText(doc.getString("phone"));

        String dob = doc.getString("dateOfBirth");
        if (dob == null) dob = doc.getString("dob");
        dobEdit.setText(dob);

        notesEdit.setText(doc.getString("notes"));
    }

    private void loadStudents() {
        db.collection("users").document(userId)
                .collection("students").get()
                .addOnSuccessListener(query -> {
                    studentContainer.removeAllViews();

                    if (query.isEmpty()) {
                        TextView empty = new TextView(requireContext());
                        empty.setText(getString(R.string.no_linked_students));
                        empty.setPadding(0, 8, 0, 8);
                        studentContainer.addView(empty);
                        return;
                    }

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        studentContainer.addView(buildStudentCard(doc));
                    }
                });
    }

    private View buildStudentCard(DocumentSnapshot doc) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);

        card.setBackgroundResource(R.drawable.shared_bg_input_rounded);
        int p = dpToPx(12);
        card.setPadding(p, p, p, p);

        String name = doc.getString("name");
        String studentId = doc.getString("studentId");
        String className = doc.getString("class");

        addLabeledRow(card, "Name", name);
        addLabeledRow(card, "Student ID", studentId);
        addLabeledRow(card, "Class", TextUtils.isEmpty(className) ? "—" : className);

        return card;
    }

    private void addLabeledRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        rowParams.setMargins(0, 0, 0, 4);
        row.setLayoutParams(rowParams);

        TextView labelView = new TextView(requireContext());
        labelView.setText(label + ": ");
        labelView.setTypeface(labelView.getTypeface(), android.graphics.Typeface.BOLD);

        labelView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView valueView = new TextView(requireContext());
        valueView.setText(value != null ? value : "—");

        valueView.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f));

        row.addView(labelView);
        row.addView(valueView);
        parent.addView(row);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void saveProfile() {
        String fullName = fullNameEdit.getText().toString().trim();
        String phone = phoneEdit.getText().toString().trim();
        String dob = dobEdit.getText().toString().trim();
        String notes = notesEdit.getText().toString().trim();
        String newPassword = passwordEdit.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            Toast.makeText(getContext(), getString(R.string.full_name_required), Toast.LENGTH_SHORT).show();
            return;
        }

        saveButton.setEnabled(false);
        saveButton.setText(getString(R.string.saving));

        if (!TextUtils.isEmpty(newPassword)) {

            if (newPassword.length() < 6) {
                restoreSaveButton();

                Toast.makeText(
                        getContext(),
                        getString(R.string.password_min_length),
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            currentUser.updatePassword(newPassword)
                    .addOnSuccessListener(v -> saveUserData(fullName, phone, dob, notes))
                    .addOnFailureListener(e -> {
                        restoreSaveButton();

                        Toast.makeText(
                                getContext(),
                                getString(R.string.password_update_failed, e.getMessage()),
                                Toast.LENGTH_SHORT
                        ).show();
                    });

        } else {
            saveUserData(fullName, phone, dob, notes);
        }
    }

    private void saveUserData(String fullName, String phone, String dob, String notes) {

        Map<String, Object> data = new HashMap<>();

        data.put("fullName", fullName);
        data.put("fullname", fullName);
        data.put("phone", phone);
        data.put("dateOfBirth", dob);
        data.put("dob", dob);
        data.put("notes", notes);
        data.put("email", currentUser.getEmail());
        data.put("usertype", "parent");
        data.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("users").document(userId)
                .update(data)
                .addOnSuccessListener(v -> {
                    restoreSaveButton();
                    passwordEdit.setText("");

                    Toast.makeText(
                            getContext(),
                            getString(R.string.profile_saved),
                            Toast.LENGTH_SHORT
                    ).show();
                })

                .addOnFailureListener(e -> {
                    restoreSaveButton();

                    Toast.makeText(
                            getContext(),
                            getString(R.string.save_failed, e.getMessage()),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void restoreSaveButton() {
        saveButton.setEnabled(true);
        saveButton.setText(getString(R.string.save_button));
    }
}