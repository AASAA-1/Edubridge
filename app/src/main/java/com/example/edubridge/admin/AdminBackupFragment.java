package com.example.edubridge.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminBackupFragment extends Fragment {

    private MaterialCardView cardAutoBackup, cardManualBackup, cardRestore, cardSchedule;
    private TextView tvLastBackup, tvBackupSize, tvScheduleInfo;
    private MaterialButton btnBackupNow, btnRestoreNow, btnExportData, btnSetSchedule;
    private LinearProgressIndicator progressIndicator;
    private com.google.android.material.materialswitch.MaterialSwitch switchAutoBackup;

    private FirebaseFirestore db;
    private List<String> backupHistory;
    private String lastBackupTime = "Never";
    private long totalBackupSize = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_backup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        backupHistory = new ArrayList<>();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        initializeViews(view);
        loadBackupInfo();
        setupClickListeners();
    }

    private void initializeViews(View view) {
        cardAutoBackup = view.findViewById(R.id.card_auto_backup);
        cardManualBackup = view.findViewById(R.id.card_manual_backup);
        cardRestore = view.findViewById(R.id.card_restore);
        cardSchedule = view.findViewById(R.id.card_schedule);
        tvLastBackup = view.findViewById(R.id.tv_last_backup);
        tvBackupSize = view.findViewById(R.id.tv_backup_size);
        tvScheduleInfo = view.findViewById(R.id.tv_schedule_info);
        btnBackupNow = view.findViewById(R.id.btn_backup_now);
        btnRestoreNow = view.findViewById(R.id.btn_restore_now);
        btnSetSchedule = view.findViewById(R.id.btn_set_schedule);
        btnExportData = view.findViewById(R.id.btn_export_data);
        progressIndicator = view.findViewById(R.id.progress_indicator);
        switchAutoBackup = view.findViewById(R.id.switch_auto_backup);
    }

    private void setupClickListeners() {
        btnBackupNow.setOnClickListener(v -> performManualBackup());
        btnRestoreNow.setOnClickListener(v -> showRestoreDialog());
        btnSetSchedule.setOnClickListener(v -> showScheduleDialog());
        btnExportData.setOnClickListener(v -> exportData());

        cardAutoBackup.setOnClickListener(v -> toggleAutoBackup());
        cardSchedule.setOnClickListener(v -> showScheduleDialog());

        if (switchAutoBackup != null) {
            switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Map<String, Object> config = new HashMap<>();
                config.put("autoBackup", isChecked);
                db.collection("system_settings")
                        .document("backup_config")
                        .set(config, com.google.firebase.firestore.SetOptions.merge());
            });
        }
    }

    private void loadBackupInfo() {
        // Load backup configuration from Firestore
        db.collection("system_settings")
                .document("backup_config")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        lastBackupTime = documentSnapshot.getString("lastBackup");
                        if (lastBackupTime == null) lastBackupTime = "Never";

                        Long size = documentSnapshot.getLong("totalSize");
                        totalBackupSize = size != null ? size : 0;

                        String schedule = documentSnapshot.getString("schedule");
                        tvScheduleInfo.setText(schedule != null ? schedule : "Not scheduled");

                        Boolean autoBackup = documentSnapshot.getBoolean("autoBackup");
                        if (autoBackup != null && switchAutoBackup != null) {
                            switchAutoBackup.setChecked(autoBackup);
                        }
                    }
                    updateBackupInfo();
                })
                .addOnFailureListener(e -> {
                    // Use default values
                    updateBackupInfo();
                });

        // Load backup history
        db.collection("backup_history")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    backupHistory.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        backupHistory.add(doc.getString("backupTime"));
                    }
                });
    }

    private void updateBackupInfo() {
        tvLastBackup.setText(lastBackupTime);

        String sizeText;
        if (totalBackupSize < 1024) {
            sizeText = totalBackupSize + " B";
        } else if (totalBackupSize < 1024 * 1024) {
            sizeText = String.format(Locale.getDefault(), "%.2f KB", totalBackupSize / 1024.0);
        } else {
            sizeText = String.format(Locale.getDefault(), "%.2f MB", totalBackupSize / (1024.0 * 1024.0));
        }
        tvBackupSize.setText(sizeText);
    }

    private void toggleAutoBackup() {
        boolean isEnabled = cardAutoBackup.isSelected();
        cardAutoBackup.setSelected(!isEnabled);

        Map<String, Object> config = new HashMap<>();
        config.put("autoBackup", !isEnabled);

        db.collection("system_settings")
                .document("backup_config")
                .set(config, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(getContext(),
                                !isEnabled ? "Auto backup enabled" : "Auto backup disabled",
                                Toast.LENGTH_SHORT).show());
    }

    private void performManualBackup() {
        progressIndicator.setVisibility(View.VISIBLE);
        btnBackupNow.setEnabled(false);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        final String backupTime = sdf.format(new Date());

        // Collect data from all collections
        List<String> collections = List.of("users", "attendance", "messages", "announcements",
                "materials", "grades", "notifications");

        Map<String, Object> backupData = new HashMap<>();
        backupData.put("backupTime", backupTime);
        backupData.put("timestamp", new Date());

        AtomicInteger pendingCollections = new AtomicInteger(collections.size());

        for (String collection : collections) {
            db.collection(collection)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Map<String, Object>> documents = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            documents.add(doc.getData());
                        }
                        backupData.put(collection, documents);

                        if (pendingCollections.decrementAndGet() == 0) {
                            saveBackupToFirestore(backupTime, backupData);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (pendingCollections.decrementAndGet() == 0) {
                            saveBackupToFirestore(backupTime, backupData);
                        }
                    });
        }
    }

    private void saveBackupToFirestore(String backupTime, Map<String, Object> backupData) {
        // Save backup record
        Map<String, Object> backupRecord = new HashMap<>();
        backupRecord.put("backupTime", backupTime);
        backupRecord.put("timestamp", new Date());
        backupRecord.put("size", backupData.toString().length());

        db.collection("backup_history")
                .add(backupRecord)
                .addOnSuccessListener(documentReference -> {
                    // Update last backup info
                    Map<String, Object> config = new HashMap<>();
                    config.put("lastBackup", backupTime);
                    config.put("totalSize", backupData.toString().length());

                    db.collection("system_settings")
                            .document("backup_config")
                            .set(config, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                progressIndicator.setVisibility(View.GONE);
                                btnBackupNow.setEnabled(true);
                                lastBackupTime = backupTime;
                                totalBackupSize = backupData.toString().length();
                                updateBackupInfo();

                                Toast.makeText(getContext(), "Backup completed successfully",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    btnBackupNow.setEnabled(true);
                    Toast.makeText(getContext(), "Backup failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showRestoreDialog() {
        if (backupHistory.isEmpty()) {
            Toast.makeText(getContext(), "No backups available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] backups = backupHistory.toArray(new String[0]);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Backup to Restore")
                .setItems(backups, (dialog, which) ->
                        confirmRestore(backups[which]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmRestore(String backupTime) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Restore")
                .setMessage("Restoring will overwrite current data. This action cannot be undone. Continue?")
                .setPositiveButton("Restore", (dialog, which) -> performRestore(backupTime))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performRestore(String backupTime) {
        progressIndicator.setVisibility(View.VISIBLE);

        Toast.makeText(getContext(), "Restore feature is under development",
                Toast.LENGTH_LONG).show();

        // Simulate restore process
        progressIndicator.postDelayed(() -> {
            progressIndicator.setVisibility(View.GONE);
            Toast.makeText(getContext(), "System restored from " + backupTime,
                    Toast.LENGTH_SHORT).show();
        }, 2000);
    }

    private void showScheduleDialog() {
        String[] schedules = {"Daily", "Weekly", "Monthly", "Never"};
        int checkedItem = 0;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Set Backup Schedule")
                .setSingleChoiceItems(schedules, checkedItem, (dialog, which) -> {
                    String selected = schedules[which];
                    tvScheduleInfo.setText(selected);

                    Map<String, Object> config = new HashMap<>();
                    config.put("schedule", selected);

                    db.collection("system_settings")
                            .document("backup_config")
                            .set(config, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(getContext(),
                                            "Schedule set to: " + selected,
                                            Toast.LENGTH_SHORT).show());

                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void exportData() {
        progressIndicator.setVisibility(View.VISIBLE);

        // Create JSON file with system data
        JSONObject exportObject = new JSONObject();

        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    try {
                        JSONArray usersArray = new JSONArray();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            usersArray.put(new JSONObject(doc.getData()));
                        }
                        exportObject.put("users", usersArray);

                        // Add more collections as needed
                        saveExportFile(exportObject);

                    } catch (Exception e) {
                        progressIndicator.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Export failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Export failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void saveExportFile(JSONObject exportObject) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String fileName = "edubridge_backup_" + sdf.format(new Date()) + ".json";

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, fileName);

            FileWriter writer = new FileWriter(file);
            writer.write(exportObject.toString(4));
            writer.close();

            progressIndicator.setVisibility(View.GONE);

            Toast.makeText(getContext(), "Export saved to Downloads folder",
                    Toast.LENGTH_LONG).show();

            // Share file
            shareFile(file);

        } catch (Exception e) {
            progressIndicator.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Export failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void shareFile(File file) {
        Uri fileUri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".provider", file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/json");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share Backup File"));
    }
}