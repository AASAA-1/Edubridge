package com.example.edubridge.shared;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private EditText fullNameEdit, emailEdit, phoneEdit, dobEdit, notesEdit, passwordEdit;
    private TextView passwordLabel;
    private Spinner  roleSpinner;

    private Button saveButton, addStudentButton, deleteButton;

    private LinearLayout studentContainer, studentSection;

    private FirebaseFirestore db;
    private FirebaseAuth      auth;

    private String  uid               = null;
    private boolean isCurrentUserProfile = false;
    private String  currentUserType   = null;

    private static final String[] ROLES = {"parent", "admin", "teacher"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);
        TextSizeHelper.applyScaleRecursively(v);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        bindViews(view);
        setupRoleSpinner();
        setupListeners();
        determineMode();
    }

    private void bindViews(View view) {
        fullNameEdit   = view.findViewById(R.id.full_name_edit);
        emailEdit      = view.findViewById(R.id.email_edit);
        phoneEdit      = view.findViewById(R.id.phone_edit);
        dobEdit        = view.findViewById(R.id.dob_edit);
        notesEdit      = view.findViewById(R.id.notes_edit);
        passwordEdit   = view.findViewById(R.id.password_edit);
        passwordLabel  = view.findViewById(R.id.password_label);
        roleSpinner    = view.findViewById(R.id.role_spinner);
        saveButton     = view.findViewById(R.id.save_button);
        deleteButton   = view.findViewById(R.id.delete_button);
        studentSection = view.findViewById(R.id.student_section);
        studentContainer = view.findViewById(R.id.student_container);
        addStudentButton = view.findViewById(R.id.add_student_button);
    }

    private void setupRoleSpinner() {
        // Translate role names for display
        String[] translatedRoles = {
                getString(R.string.parent_role),
                getString(R.string.admin_role),
                getString(R.string.teacher_role)
        };

        roleSpinner.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                translatedRoles
        ));

        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                boolean isParent = ROLES[pos].equals("parent");
                studentSection.setVisibility(isParent ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupListeners() {
        addStudentButton.setOnClickListener(v -> addStudentRow());
        saveButton.setOnClickListener(v -> saveUser());

        dobEdit.setOnClickListener(v -> showDatePicker(dobEdit));
    }

    private void showDatePicker(EditText target) {
        Calendar cal = Calendar.getInstance();

        String existing = target.getText().toString().trim();
        if (existing.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] parts = existing.split("-");
            cal.set(Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]) - 1,
                    Integer.parseInt(parts[2]));
        }

        new DatePickerDialog(
                requireContext(),
                (view, year, month, day) ->
                        target.setText(String.format("%04d-%02d-%02d", year, month + 1, day)),
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void determineMode() {

        boolean createMode = getArguments() != null
                && getArguments().getBoolean("create_mode", false);

        if (createMode) {
            enterCreateMode();
            return;
        }

        FirebaseUser currentUser  = auth.getCurrentUser();
        String       currentUid   = currentUser != null ? currentUser.getUid() : null;
        String       viewingUid   = getArguments() != null
                ? getArguments().getString("uid", null)
                : null;

        uid = viewingUid != null ? viewingUid : currentUid;
        isCurrentUserProfile = uid != null && uid.equals(currentUid);

        passwordEdit.setVisibility(View.GONE);
        passwordLabel.setVisibility(View.GONE);

        if (uid == null) return;

        if (currentUid != null) {
            db.collection("users").document(currentUid).get()
                    .addOnSuccessListener(doc -> {
                        currentUserType = doc.getString("usertype");
                        if ("admin".equals(currentUserType) && !isCurrentUserProfile) {
                            deleteButton.setVisibility(View.VISIBLE);
                            deleteButton.setOnClickListener(v -> confirmDeleteUser());
                        }
                    });
        }

        loadUserData(uid);
    }

    private void enterCreateMode() {
        passwordEdit.setVisibility(View.VISIBLE);
        passwordLabel.setVisibility(View.VISIBLE);
        deleteButton.setVisibility(View.GONE);

        roleSpinner.setSelection(0);
        addStudentRow();
    }

    private void loadUserData(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    fullNameEdit.setText(doc.getString("fullname"));
                    emailEdit.setText(doc.getString("email"));
                    phoneEdit.setText(doc.getString("phone"));
                    dobEdit.setText(doc.getString("dob"));
                    notesEdit.setText(doc.getString("notes"));

                    String role = doc.getString("usertype");

                    if (role != null) {
                        // Find position by original role key (parent/admin/teacher)
                        for (int i = 0; i < ROLES.length; i++) {
                            if (ROLES[i].equals(role)) {
                                roleSpinner.setSelection(i);
                                break;
                            }
                        }
                    }

                    if ("parent".equals(role)) {
                        loadStudents(uid);
                    }
                });
    }

    private void loadStudents(String parentUid) {
        db.collection("users").document(parentUid)
                .collection("students").get()
                .addOnSuccessListener(query -> {
                    studentContainer.removeAllViews();
                    for (QueryDocumentSnapshot doc : query) {
                        StudentRow row = new StudentRow(requireContext());
                        row.setName(doc.getString("name"));
                        row.setDob(doc.getString("dob"));
                        row.setOnRemoveListener(() -> studentContainer.removeView(row));
                        studentContainer.addView(row);
                    }
                });
    }

    private void addStudentRow() {
        StudentRow row = new StudentRow(requireContext());
        row.setOnRemoveListener(() -> studentContainer.removeView(row));
        studentContainer.addView(row);
    }

    static class StudentRow extends LinearLayout {

        private final EditText nameEdit;
        private final EditText dobEdit;

        StudentRow(android.content.Context context) {
            super(context);
            setOrientation(HORIZONTAL);
            setPadding(0, 0, 0, 16);

            nameEdit = new EditText(context);
            nameEdit.setHint(context.getString(R.string.student_name_hint));
            nameEdit.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT, 2f));
            addView(nameEdit);

            dobEdit = new EditText(context);
            dobEdit.setHint(context.getString(R.string.student_dob_hint));
            dobEdit.setFocusable(false);
            dobEdit.setFocusableInTouchMode(false);
            dobEdit.setCursorVisible(false);
            dobEdit.setClickable(true);
            dobEdit.setOnClickListener(v -> showStudentDatePicker(context, dobEdit));
            LayoutParams dobParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 2f);
            dobParams.setMarginStart(8);
            dobEdit.setLayoutParams(dobParams);
            addView(dobEdit);

            Button removeBtn = new Button(context);
            removeBtn.setText("✕");
            LayoutParams btnParams = new LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            btnParams.setMarginStart(8);
            removeBtn.setLayoutParams(btnParams);
            removeBtn.setOnClickListener(v -> {
                if (removeListener != null) removeListener.run();
            });
            addView(removeBtn);
        }

        private static void showStudentDatePicker(android.content.Context context, EditText target) {
            Calendar cal = Calendar.getInstance();

            String existing = target.getText().toString().trim();
            if (existing.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String[] parts = existing.split("-");
                cal.set(Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]) - 1,
                        Integer.parseInt(parts[2]));
            }

            new DatePickerDialog(
                    context,
                    (view, year, month, day) ->
                            target.setText(String.format("%04d-%02d-%02d", year, month + 1, day)),
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        }

        private Runnable removeListener;

        void setOnRemoveListener(Runnable r) { this.removeListener = r; }
        void setName(String name)             { nameEdit.setText(name); }
        void setDob(String dob)               { dobEdit.setText(dob);   }
        String getName()                      { return nameEdit.getText().toString().trim(); }
        String getDob()                       { return dobEdit.getText().toString().trim(); }
    }

    private boolean validateStudents() {
        for (int i = 0; i < studentContainer.getChildCount(); i++) {
            StudentRow row = (StudentRow) studentContainer.getChildAt(i);
            if (TextUtils.isEmpty(row.getName())) {
                Toast.makeText(getContext(), getString(R.string.student_name_required), Toast.LENGTH_SHORT).show();
                return false;
            }
            if (TextUtils.isEmpty(row.getDob())) {
                Toast.makeText(getContext(), getString(R.string.student_dob_required), Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void saveUser() {
        String fullname  = fullNameEdit.getText().toString().trim();
        String email     = emailEdit.getText().toString().trim();
        String password  = passwordEdit.getText().toString().trim();

        // Get the actual role key from the translated display
        int selectedPos = roleSpinner.getSelectedItemPosition();
        String role = ROLES[selectedPos];

        if (TextUtils.isEmpty(fullname) || TextUtils.isEmpty(email)) {
            Toast.makeText(getContext(), getString(R.string.name_email_required), Toast.LENGTH_SHORT).show();
            return;
        }

        if ("parent".equals(role) && studentSection.getVisibility() == View.VISIBLE) {
            if (!validateStudents()) return;
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("fullname", fullname);
        userMap.put("email",    email);
        userMap.put("phone",    phoneEdit.getText().toString().trim());
        userMap.put("dob",      dobEdit.getText().toString().trim());
        userMap.put("notes",    notesEdit.getText().toString().trim());
        userMap.put("usertype", role);

        if (uid == null) {
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(getContext(), getString(R.string.password_required), Toast.LENGTH_SHORT).show();
                return;
            }
            createNewUser(email, password, role, userMap);

        } else {
            db.collection("users").document(uid)
                    .update(userMap)
                    .addOnSuccessListener(unused -> {
                        if ("parent".equals(role)) {
                            replaceStudents(uid);
                        } else {
                            deleteAllStudents(uid, () -> {});
                            showSuccessAndPop();
                        }
                    });
        }
    }

    private void createNewUser(String email, String password, String role,
                               Map<String, Object> userMap) {
        FirebaseApp secondaryApp;
        try {
            secondaryApp = FirebaseApp.getInstance("Secondary");
        } catch (IllegalStateException e) {
            secondaryApp = FirebaseApp.initializeApp(
                    requireContext(),
                    FirebaseApp.getInstance().getOptions(),
                    "Secondary");
        }

        FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
        secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) return;

                    String newUid = firebaseUser.getUid();
                    generateUserId(role, userId -> {
                        userMap.put("userId", userId);
                        db.collection("users").document(newUid)
                                .set(userMap)
                                .addOnSuccessListener(unused -> {
                                    if ("parent".equals(role)) {
                                        saveStudents(newUid, () -> {});
                                    }
                                    secondaryAuth.signOut();
                                    showSuccessAndPop();
                                });
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                getString(R.string.failed_create_account, e.getMessage()),
                                Toast.LENGTH_LONG).show());
    }

    private void replaceStudents(String parentUid) {
        deleteAllStudents(parentUid, () -> saveStudents(parentUid, this::showSuccessAndPop));
    }

    private void deleteAllStudents(String parentUid, Runnable onDone) {
        db.collection("users").document(parentUid)
                .collection("students").get()
                .addOnSuccessListener(query -> {
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : query) batch.delete(doc.getReference());
                    batch.commit().addOnSuccessListener(v -> onDone.run());
                });
    }

    private void saveStudents(String parentUid, Runnable onDone) {
        List<Map<String, Object>> students = collectStudentRows();

        if (students.isEmpty()) {
            onDone.run();
            return;
        }

        saveNextStudent(parentUid, students, 0, onDone);
    }

    private void saveNextStudent(String parentUid, List<Map<String, Object>> students,
                                 int index, Runnable onDone) {
        if (index >= students.size()) {
            onDone.run();
            return;
        }

        Map<String, Object> student = students.get(index);
        generateUserId("student", studentId -> {
            student.put("studentId", studentId);
            db.collection("users").document(parentUid)
                    .collection("students")
                    .add(student)
                    .addOnSuccessListener(ref ->
                            saveNextStudent(parentUid, students, index + 1, onDone));
        });
    }

    private List<Map<String, Object>> collectStudentRows() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < studentContainer.getChildCount(); i++) {
            StudentRow row  = (StudentRow) studentContainer.getChildAt(i);
            String     name = row.getName();
            String     dob  = row.getDob();
            if (TextUtils.isEmpty(name) && TextUtils.isEmpty(dob)) continue;

            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("dob",  dob);
            map.put("class",   "");
            map.put("classId", "");
            list.add(map);
        }
        return list;
    }

    interface IdCallback { void onId(String id); }

    private void generateUserId(String role, IdCallback callback) {
        String counterDoc;
        String prefix;

        switch (role) {
            case "parent":  counterDoc = "parents";  prefix = "P"; break;
            case "teacher": counterDoc = "teachers"; prefix = "T"; break;
            case "admin":   counterDoc = "admins";   prefix = "A"; break;
            case "student": counterDoc = "students"; prefix = "S"; break;
            default:        counterDoc = "users";    prefix = "U"; break;
        }

        db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(
                    db.collection("counters").document(counterDoc));
            long lastId = snap.exists() && snap.getLong("lastId") != null
                    ? snap.getLong("lastId") : 0;
            long newId = lastId + 1;
            transaction.update(
                    db.collection("counters").document(counterDoc),
                    "lastId", newId);
            return String.format("%s%04d", prefix, newId);
        }).addOnSuccessListener(id -> callback.onId((String) id));
    }

    private void confirmDeleteUser() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.delete_user_title))
                .setMessage(getString(R.string.delete_user_message))
                .setPositiveButton(getString(R.string.delete_confirm), (d, w) -> deleteUser())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void deleteUser() {
        if (uid == null) return;

        db.collection("users").document(uid)
                .collection("students").get()
                .addOnSuccessListener(query -> {
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : query) batch.delete(doc.getReference());
                    batch.delete(db.collection("users").document(uid));
                    batch.commit()
                            .addOnSuccessListener(v -> {
                                Toast.makeText(getContext(),
                                        getString(R.string.user_deleted), Toast.LENGTH_SHORT).show();
                                getParentFragmentManager().popBackStack();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            getString(R.string.delete_failed_message, e.getMessage()),
                                            Toast.LENGTH_SHORT).show());
                });
    }

    private void showSuccessAndPop() {
        if (getContext() == null) return;
        Toast.makeText(getContext(), getString(R.string.saved_successfully), Toast.LENGTH_SHORT).show();
        getParentFragmentManager().popBackStack();
    }
}