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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private EditText fullNameEdit, emailEdit, phoneEdit, dobEdit, notesEdit, passwordEdit;
    private TextView passwordLabel;
    private Spinner roleSpinner;
    private Button saveButton;

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

        String[] roles = {"parent", "student", "admin", "teacher"};
        roleSpinner.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                roles
        ));

        if (getArguments() != null) {
            uid = getArguments().getString("uid");
        }

        if (uid != null) {
            passwordEdit.setVisibility(View.GONE);
            passwordLabel.setVisibility(View.GONE);
            loadUserData(uid);
        }

        saveButton.setOnClickListener(v -> saveUser());
    }

    private void loadUserData(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {

                    fullNameEdit.setText(document.getString("fullname"));
                    emailEdit.setText(document.getString("email"));
                    phoneEdit.setText(document.getString("phone"));
                    dobEdit.setText(document.getString("dob"));
                    notesEdit.setText(document.getString("notes"));

                    String role = document.getString("usertype");
                    if (role != null) {
                        ArrayAdapter adapter =
                                (ArrayAdapter) roleSpinner.getAdapter();
                        roleSpinner.setSelection(adapter.getPosition(role));
                    }
                });
    }

    private void saveUser() {

        String fullname = fullNameEdit.getText().toString().trim();
        String email = emailEdit.getText().toString().trim();
        String phone = phoneEdit.getText().toString().trim();
        String dob = dobEdit.getText().toString().trim();
        String notes = notesEdit.getText().toString().trim();
        String role = roleSpinner.getSelectedItem().toString();
        String password = passwordEdit.getText().toString().trim();

        if (TextUtils.isEmpty(fullname) || TextUtils.isEmpty(email)) {
            Toast.makeText(getContext(), "Name & Email required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (uid == null && TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Password required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("fullname", fullname);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("dob", dob);
        userMap.put("notes", notes);
        userMap.put("usertype", role);

        if (uid == null) {

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {

                        FirebaseUser firebaseUser = authResult.getUser();
                        if (firebaseUser != null) {

                            String newUid = firebaseUser.getUid();

                            db.collection("users")
                                    .document(newUid)
                                    .set(userMap)
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(getContext(),
                                                "User created successfully",
                                                Toast.LENGTH_SHORT).show();
                                        getParentFragmentManager().popBackStack();
                                    });
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(),
                                    "Creation failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());

        } else {

            db.collection("users")
                    .document(uid)
                    .update(userMap)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(getContext(),
                                "User updated successfully",
                                Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    });
        }
    }
}
