package com.example.edubridge.teacher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditTeacherProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;

    private CircleImageView ivProfileImage;
    private TextInputEditText etFullName, etPhone, etSubject;
    private MaterialButton btnSave;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;
    private String currentUserId;
    private Uri imageUri;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_edit_teacher_profile, container, false);
        TextSizeHelper.applyScaleRecursively(v);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUserId = auth.getCurrentUser().getUid();

        initializeViews(view);
        setupToolbar(view);
        loadCurrentProfile();
        setupClickListeners(view);
    }

    private void initializeViews(View view) {
        ivProfileImage = view.findViewById(R.id.iv_profile_image);
        etFullName = view.findViewById(R.id.et_full_name);
        etPhone = view.findViewById(R.id.et_phone);
        etSubject = view.findViewById(R.id.et_subject);
        btnSave = view.findViewById(R.id.btn_save);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void loadCurrentProfile() {
        showLoading(true);

        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullname");
                        String phone = documentSnapshot.getString("phone");
                        String subject = documentSnapshot.getString("subject");

                        etFullName.setText(fullName != null ? fullName : "");
                        etPhone.setText(phone != null ? phone : "");
                        etSubject.setText(subject != null ? subject : "");
                    }
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(getContext(), "Failed to load profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupClickListeners(View view) {
        ivProfileImage.setOnClickListener(v -> openFileChooser());
        view.findViewById(R.id.tv_change_photo).setOnClickListener(v -> openFileChooser());
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_profile_picture)), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();
            ivProfileImage.setImageURI(imageUri);
        }
    }

    private void saveProfile() {
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();

        if (fullName.isEmpty()) {
            etFullName.setError(getString(R.string.full_name_required));
            etFullName.requestFocus();
            return;
        }

        showLoading(true);
        btnSave.setEnabled(false);

        Map<String, Object> updates = new HashMap<>();
        updates.put("fullname", fullName);
        updates.put("phone", phone);
        updates.put("subject", subject);

        if (imageUri != null) {
            uploadImageAndSaveProfile(updates);
        } else {
            saveProfileToFirestore(updates);
        }
    }

    private void uploadImageAndSaveProfile(Map<String, Object> updates) {
        StorageReference fileReference = storage.getReference("profile_images")
                .child(currentUserId + ".jpg");

        fileReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        fileReference.getDownloadUrl().addOnSuccessListener(uri -> {
                            updates.put("profileImageUrl", uri.toString());
                            saveProfileToFirestore(updates);
                        }))
                .addOnFailureListener(e -> {
                    showLoading(false);
                    btnSave.setEnabled(true);
                    Toast.makeText(getContext(), getString(R.string.image_upload_failed, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void saveProfileToFirestore(Map<String, Object> updates) {
        db.collection("users").document(currentUserId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), getString(R.string.profile_updated),
                            Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    btnSave.setEnabled(true);
                    Toast.makeText(getContext(), getString(R.string.profile_update_failed, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}