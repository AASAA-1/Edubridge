package com.example.edubridge.shared;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private EditText fullNameEdit, emailEdit, phoneEdit, dobEdit, notesEdit, passwordEdit;
    private TextView passwordLabel;

    private Spinner roleSpinner;
    private Button saveButton, addStudentButton;

    private LinearLayout studentContainer, studentSection;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String uid = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        fullNameEdit = view.findViewById(R.id.full_name_edit);
        emailEdit = view.findViewById(R.id.email_edit);
        phoneEdit = view.findViewById(R.id.phone_edit);
        dobEdit = view.findViewById(R.id.dob_edit);
        notesEdit = view.findViewById(R.id.notes_edit);
        passwordEdit = view.findViewById(R.id.password_edit);
        passwordLabel = view.findViewById(R.id.password_label);

        roleSpinner = view.findViewById(R.id.role_spinner);
        saveButton = view.findViewById(R.id.save_button);

        studentContainer = view.findViewById(R.id.student_container);
        studentSection = view.findViewById(R.id.student_section);
        addStudentButton = view.findViewById(R.id.add_student_button);

        String[] roles = {"parent", "admin", "teacher"};

        roleSpinner.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                roles
        ));

        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String role = roleSpinner.getSelectedItem().toString();

                if (role.equals("parent")) {
                    studentSection.setVisibility(View.VISIBLE);
                } else {
                    studentSection.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        addStudentButton.setOnClickListener(v -> addStudentView());
        saveButton.setOnClickListener(v -> saveUser());

        determineUserMode();
    }

    private void determineUserMode() {

        boolean createMode = false;

        if (getArguments() != null) {
            createMode = getArguments().getBoolean("create_mode", false);
        }

        if (createMode) {

            passwordEdit.setVisibility(View.VISIBLE);
            passwordLabel.setVisibility(View.VISIBLE);

            roleSpinner.setSelection(0);
            addStudentView();

            return;
        }

        if (getArguments() != null && getArguments().containsKey("uid")) {

            uid = getArguments().getString("uid");

        } else {

            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser != null) {
                uid = currentUser.getUid();
            }
        }

        if (uid != null) {

            passwordEdit.setVisibility(View.GONE);
            passwordLabel.setVisibility(View.GONE);

            loadUserData(uid);
        }
    }

    private void loadUserData(String uid) {

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) return;

                    fullNameEdit.setText(document.getString("fullname"));
                    emailEdit.setText(document.getString("email"));
                    phoneEdit.setText(document.getString("phone"));
                    dobEdit.setText(document.getString("dob"));
                    notesEdit.setText(document.getString("notes"));

                    String role = document.getString("usertype");

                    if (role != null) {

                        ArrayAdapter adapter = (ArrayAdapter) roleSpinner.getAdapter();
                        roleSpinner.setSelection(adapter.getPosition(role));
                    }

                    if ("parent".equals(role)) {
                        loadStudents(uid);
                    }
                });
    }

    private void loadStudents(String parentUid) {

        db.collection("users")
                .document(parentUid)
                .collection("students")
                .get()
                .addOnSuccessListener(query -> {

                    studentContainer.removeAllViews();

                    for (var doc : query.getDocuments()) {

                        LinearLayout layout = createStudentLayout();

                        EditText name = (EditText) layout.getChildAt(0);
                        EditText dob = (EditText) layout.getChildAt(1);

                        name.setText(doc.getString("name"));
                        dob.setText(doc.getString("dob"));

                        studentContainer.addView(layout);
                    }
                });
    }

    private LinearLayout createStudentLayout() {

        LinearLayout studentLayout = new LinearLayout(getContext());
        studentLayout.setOrientation(LinearLayout.VERTICAL);
        studentLayout.setPadding(0,0,0,24);

        EditText nameEdit = new EditText(getContext());
        nameEdit.setHint("Student Name");

        EditText dobEdit = new EditText(getContext());
        dobEdit.setHint("Student DOB");

        studentLayout.addView(nameEdit);
        studentLayout.addView(dobEdit);

        return studentLayout;
    }

    private void addStudentView() {

        LinearLayout studentLayout = createStudentLayout();
        studentContainer.addView(studentLayout);
    }

    private void saveUser() {

        String fullname = fullNameEdit.getText().toString().trim();
        String email = emailEdit.getText().toString().trim();
        String role = roleSpinner.getSelectedItem().toString();
        String password = passwordEdit.getText().toString().trim();

        if (TextUtils.isEmpty(fullname) || TextUtils.isEmpty(email)) {

            Toast.makeText(getContext(),
                    "Name & Email required",
                    Toast.LENGTH_SHORT).show();

            return;
        }

        Map<String,Object> userMap = new HashMap<>();

        userMap.put("fullname",fullname);
        userMap.put("email",email);
        userMap.put("phone",phoneEdit.getText().toString());
        userMap.put("dob",dobEdit.getText().toString());
        userMap.put("notes",notesEdit.getText().toString());
        userMap.put("usertype",role);

        if (uid == null) {

            if (TextUtils.isEmpty(password)) {

                Toast.makeText(getContext(),
                        "Password required",
                        Toast.LENGTH_SHORT).show();

                return;
            }

            FirebaseApp secondaryApp;

            try {

                secondaryApp = FirebaseApp.getInstance("Secondary");

            } catch (IllegalStateException e) {

                secondaryApp = FirebaseApp.initializeApp(
                        requireContext(),
                        FirebaseApp.getInstance().getOptions(),
                        "Secondary"
                );
            }

            FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);

            secondaryAuth.createUserWithEmailAndPassword(email,password)
                    .addOnSuccessListener(authResult -> {

                        FirebaseUser firebaseUser = authResult.getUser();

                        if (firebaseUser == null) return;

                        String newUid = firebaseUser.getUid();

                        generateUserId(role, userId -> {

                            userMap.put("userId", userId);

                            db.collection("users")
                                    .document(newUid)
                                    .set(userMap)
                                    .addOnSuccessListener(unused -> {

                                        if (role.equals("parent")) {
                                            saveStudents(newUid);
                                        }

                                        secondaryAuth.signOut();

                                        Toast.makeText(getContext(),
                                                "User created successfully",
                                                Toast.LENGTH_SHORT).show();

                                        getParentFragmentManager().popBackStack();
                                    });
                        });
                    });

        } else {

            db.collection("users")
                    .document(uid)
                    .update(userMap)
                    .addOnSuccessListener(unused -> {

                        if (role.equals("parent")) {

                            db.collection("users")
                                    .document(uid)
                                    .collection("students")
                                    .get()
                                    .addOnSuccessListener(query -> {

                                        for (var doc : query.getDocuments()) {
                                            doc.getReference().delete();
                                        }

                                        saveStudents(uid);
                                    });
                        }

                        Toast.makeText(getContext(),
                                "User updated successfully",
                                Toast.LENGTH_SHORT).show();

                        getParentFragmentManager().popBackStack();
                    });
        }
    }

    private void generateUserId(String role, OnSuccessListener<String> listener) {

        String counterDoc;
        String prefix;

        switch (role) {

            case "parent":
                counterDoc = "parents";
                prefix = "P";
                break;

            case "teacher":
                counterDoc = "teachers";
                prefix = "T";
                break;

            case "admin":
                counterDoc = "admins";
                prefix = "A";
                break;

            case "student":
                counterDoc = "students";
                prefix = "S";
                break;

            default:
                counterDoc = "users";
                prefix = "U";
        }

        db.runTransaction(transaction -> {

                    DocumentSnapshot snapshot = transaction.get(
                            db.collection("counters").document(counterDoc)
                    );

                    long lastId = 0;

                    if (snapshot.exists() && snapshot.getLong("lastId") != null) {
                        lastId = snapshot.getLong("lastId");
                    }

                    long newId = lastId + 1;

                    transaction.update(
                            db.collection("counters").document(counterDoc),
                            "lastId",
                            newId
                    );

                    return String.format("%s%04d", prefix, newId);

                }).addOnSuccessListener(listener)
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                });
    }

    private void saveStudents(String parentUid) {

        for (int i = 0; i < studentContainer.getChildCount(); i++) {

            LinearLayout layout = (LinearLayout) studentContainer.getChildAt(i);

            EditText name = (EditText) layout.getChildAt(0);
            EditText dob = (EditText) layout.getChildAt(1);

            Map<String,Object> student = new HashMap<>();

            generateUserId("student", studentId -> {

                student.put("studentId", studentId);
                student.put("name", name.getText().toString());
                student.put("dob", dob.getText().toString());
                student.put("class","");
                student.put("classId","");

                db.collection("users")
                        .document(parentUid)
                        .collection("students")
                        .add(student);
            });
        }
    }
}