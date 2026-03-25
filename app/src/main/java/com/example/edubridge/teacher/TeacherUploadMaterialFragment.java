package com.example.edubridge.teacher;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TeacherUploadMaterialFragment extends Fragment {

    private EditText etTitle, etDescription, etSubject;
    private Spinner spinnerClass, spinnerType;
    private MaterialCardView cardSelectFile;
    private MaterialButton btnUpload;
    private LinearProgressIndicator progressIndicator;
    private TextView tvSelectedFile;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private String currentTeacherId;
    private Uri selectedFileUri;
    private String selectedFileName = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_upload_material, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        currentTeacherId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        initializeViews(view);
        setupSpinners();
        setupClickListeners();
    }

    private void initializeViews(View view) {
        etTitle = view.findViewById(R.id.et_title);
        etDescription = view.findViewById(R.id.et_description);
        etSubject = view.findViewById(R.id.et_subject);
        spinnerClass = view.findViewById(R.id.spinner_class);
        spinnerType = view.findViewById(R.id.spinner_type);
        cardSelectFile = view.findViewById(R.id.card_select_file);
        btnUpload = view.findViewById(R.id.btn_upload);
        progressIndicator = view.findViewById(R.id.progress_indicator);
        tvSelectedFile = view.findViewById(R.id.tv_selected_file);
    }

    private void setupSpinners() {
        String[] classes = {"Class 5A", "Class 5B", "Class 6A", "Class 6B", "Class 7A", "All Classes"};
        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, classes);
        spinnerClass.setAdapter(classAdapter);

        String[] types = {"Assignment", "Study Material", "Homework", "Lecture Notes", "Video", "Other"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(typeAdapter);
    }

    private void setupClickListeners() {
        cardSelectFile.setOnClickListener(v -> openFilePicker());
        btnUpload.setOnClickListener(v -> uploadMaterial());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "image/*", "video/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(Intent.createChooser(intent, "Select File"), 1001);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == getActivity().RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            if (selectedFileUri != null) {
                selectedFileName = getFileName(selectedFileUri);
                tvSelectedFile.setText(selectedFileName);
                tvSelectedFile.setVisibility(View.VISIBLE);
            }
        }
    }

    private String getFileName(Uri uri) {
        String fileName = "";
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (fileName.isEmpty()) {
            fileName = uri.getPath();
            int cut = fileName.lastIndexOf('/');
            if (cut != -1) {
                fileName = fileName.substring(cut + 1);
            }
        }
        return fileName;
    }

    private void uploadMaterial() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();
        String selectedClass = spinnerClass.getSelectedItem().toString();
        String materialType = spinnerType.getSelectedItem().toString();

        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            return;
        }
        if (subject.isEmpty()) {
            etSubject.setError("Subject is required");
            return;
        }
        if (selectedFileUri == null) {
            Toast.makeText(getContext(), "Please select a file", Toast.LENGTH_SHORT).show();
            return;
        }

        progressIndicator.setVisibility(View.VISIBLE);
        btnUpload.setEnabled(false);

        String timestamp = String.valueOf(System.currentTimeMillis());
        String fileName = timestamp + "_" + selectedFileName;
        StorageReference fileRef = storageRef.child("materials/" + fileName);

        fileRef.putFile(selectedFileUri)
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressIndicator.setProgress((int) progress);
                })
                .addOnSuccessListener(taskSnapshot ->
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            saveMaterialToFirestore(title, description, subject,
                                    selectedClass, materialType, uri.toString(), fileName);
                        }))
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    Toast.makeText(getContext(), "Upload failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveMaterialToFirestore(String title, String description, String subject,
                                         String className, String type, String fileUrl, String fileName) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        Map<String, Object> material = new HashMap<>();
        material.put("title", title);
        material.put("description", description);
        material.put("subject", subject);
        material.put("className", className);
        material.put("type", type);
        material.put("fileUrl", fileUrl);
        material.put("fileName", fileName);
        material.put("teacherId", currentTeacherId);
        material.put("uploadDate", currentDate);
        material.put("timestamp", new Date());

        db.collection("materials")
                .add(material)
                .addOnSuccessListener(documentReference -> {
                    progressIndicator.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    Toast.makeText(getContext(), "Material uploaded successfully",
                            Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    Toast.makeText(getContext(), "Error saving material: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}