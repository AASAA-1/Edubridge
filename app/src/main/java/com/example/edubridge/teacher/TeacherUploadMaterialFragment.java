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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TeacherUploadMaterialFragment extends Fragment {

    private static final int PICK_FILE_REQUEST = 1001;

    private EditText etTitle, etDescription, etSubject;
    private Spinner spinnerClass, spinnerType;
    private MaterialCardView cardSelectFile;
    private MaterialButton btnUpload;
    private LinearProgressIndicator progressIndicator;
    private TextView tvSelectedFile, tvClassStatus;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private String currentTeacherId;
    private Uri selectedFileUri;
    private String selectedFileName = "";
    private String selectedClassId = null;
    private String selectedClassName = null;

    private final List<String> classNames = new ArrayList<>();
    private final List<String> classIds = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_teacher_upload_material, container, false);
        TextSizeHelper.applyScaleRecursively(v);
        return v;
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
        setupTypeSpinner();
        setupClickListeners();
        fetchAssignedClasses();
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
        tvClassStatus = view.findViewById(R.id.tv_class_status);
    }

    private void fetchAssignedClasses() {
        db.collection("classes")
                .whereEqualTo("teacherId", currentTeacherId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (getContext() == null || !isAdded()) return;
                    classNames.clear();
                    classIds.clear();

                    classNames.add(getString(R.string.grade_all));
                    classIds.add("all");

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            classNames.add(name);
                            classIds.add(doc.getId());
                        }
                    }

                    if (classNames.size() <= 1) {
                        tvClassStatus.setVisibility(View.VISIBLE);
                        tvClassStatus.setText(getString(R.string.no_classes_assigned));
                        spinnerClass.setVisibility(View.GONE);
                    } else {
                        tvClassStatus.setVisibility(View.GONE);
                        spinnerClass.setVisibility(View.VISIBLE);
                        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_spinner_dropdown_item,
                                classNames);
                        spinnerClass.setAdapter(classAdapter);
                        spinnerClass.setEnabled(true);
                        setupClassSpinnerListener();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null || !isAdded()) return;
                    tvClassStatus.setVisibility(View.VISIBLE);
                    tvClassStatus.setText(getString(R.string.classes_load_failed));
                    spinnerClass.setVisibility(View.GONE);
                });
    }

    private void setupClassSpinnerListener() {
        spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= classIds.size()) return;
                selectedClassName = classNames.get(position);
                selectedClassId = classIds.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupTypeSpinner() {
        String[] types = {
                getString(R.string.type_assignment),
                getString(R.string.type_study_material),
                getString(R.string.type_homework),
                getString(R.string.type_lecture_notes),
                getString(R.string.type_video),
                getString(R.string.other)
        };
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
                "image/*", "video/*", "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_file)), PICK_FILE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == getActivity().RESULT_OK && data != null && data.getData() != null) {
            selectedFileUri = data.getData();
            selectedFileName = getFileName(selectedFileUri);
            tvSelectedFile.setText(selectedFileName);
            tvSelectedFile.setVisibility(View.VISIBLE);

            // عرض نوع الملف للمستخدم
            String mimeType = getContext().getContentResolver().getType(selectedFileUri);
            if (mimeType != null) {
                Toast.makeText(getContext(), "Selected: " + mimeType, Toast.LENGTH_SHORT).show();
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (fileName.isEmpty()) {
            String path = uri.getPath();
            int cut = path.lastIndexOf('/');
            if (cut != -1) {
                fileName = path.substring(cut + 1);
            }
        }
        return fileName;
    }

    private void uploadMaterial() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String subject = etSubject.getText().toString().trim();
        String materialType = spinnerType.getSelectedItem().toString();

        if (selectedClassName == null) {
            Toast.makeText(getContext(), "Please select a class first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty()) {
            etTitle.setError(getString(R.string.title_required));
            etTitle.requestFocus();
            return;
        }
        if (subject.isEmpty()) {
            etSubject.setError(getString(R.string.subject_required));
            etSubject.requestFocus();
            return;
        }
        if (selectedFileUri == null) {
            Toast.makeText(getContext(), getString(R.string.file_required), Toast.LENGTH_SHORT).show();
            return;
        }

        progressIndicator.setVisibility(View.VISIBLE);
        progressIndicator.setProgress(0);
        btnUpload.setEnabled(false);

        // استخدام UUID لضمان اسم ملف فريد
        String uniqueId = UUID.randomUUID().toString();
        String fileExtension = getFileExtension(selectedFileName);
        String storedFileName = currentTeacherId + "_" + uniqueId + "_" + System.currentTimeMillis() + fileExtension;

        // مسار التخزين: materials/teacherId/filename
        StorageReference fileRef = storageRef.child("materials/" + currentTeacherId + "/" + storedFileName);

        UploadTask uploadTask = fileRef.putFile(selectedFileUri);

        uploadTask.addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressIndicator.setProgress((int) progress);
                })
                .addOnSuccessListener(taskSnapshot -> {
                    // الملف تم رفعه بنجاح، الآن نجلب الـ URL
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveMaterialToFirestore(title, description, subject,
                                selectedClassName, selectedClassId, materialType, uri.toString(), storedFileName);
                    }).addOnFailureListener(e -> {
                        progressIndicator.setVisibility(View.GONE);
                        btnUpload.setEnabled(true);
                        Toast.makeText(getContext(), "Failed to get download URL: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    Toast.makeText(getContext(), getString(R.string.upload_failed, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot);
        }
        return "";
    }

    private void saveMaterialToFirestore(String title, String description, String subject,
                                         String className, String classId, String type,
                                         String fileUrl, String fileName) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        Map<String, Object> material = new HashMap<>();
        material.put("title", title);
        material.put("description", description);
        material.put("subject", subject);
        material.put("className", className);
        material.put("classId", classId);
        material.put("type", type);
        material.put("fileUrl", fileUrl);
        material.put("fileName", fileName);
        material.put("originalFileName", selectedFileName);
        material.put("teacherId", currentTeacherId);
        material.put("uploadDate", currentDate);
        material.put("timestamp", new Date());

        db.collection("materials")
                .add(material)
                .addOnSuccessListener(documentReference -> {
                    progressIndicator.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    Toast.makeText(getContext(), getString(R.string.material_uploaded) + " ✓",
                            Toast.LENGTH_SHORT).show();

                    // تنظيف الحقول بعد الرفع الناجح
                    clearForm();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    Toast.makeText(getContext(), getString(R.string.save_material_error, e.getMessage()),
                            Toast.LENGTH_LONG).show();

                    // حذف الملف من التخزين إذا فشل الحفظ في Firestore
                    storageRef.child("materials/" + currentTeacherId + "/" + fileName)
                            .delete()
                            .addOnFailureListener(deleteError ->
                                    Toast.makeText(getContext(), "Warning: File may remain in storage", Toast.LENGTH_SHORT).show());
                });
    }

    private void clearForm() {
        etTitle.setText("");
        etDescription.setText("");
        etSubject.setText("");
        selectedFileUri = null;
        selectedFileName = "";
        tvSelectedFile.setVisibility(View.GONE);
        tvSelectedFile.setText("");
        progressIndicator.setProgress(0);
    }
}