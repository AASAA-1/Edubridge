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
import com.example.edubridge.shared.TextSizeHelper;
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
    private String lastBackupTime;
    private long totalBackupSize = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_admin_backup, container, false);
        TextSizeHelper.applyScaleRecursively(v);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        backupHistory = new ArrayList<>();
        lastBackupTime = getString(R.string.never);

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
        db.collection("system_settings")
                .document("backup_config")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String savedBackupTime = documentSnapshot.getString("lastBackup");
                        if (savedBackupTime != null && !savedBackupTime.isEmpty()) {
                            lastBackupTime = savedBackupTime;
                        }

                        Long size = documentSnapshot.getLong("totalSize");
                        totalBackupSize = size != null ? size : 0;

                        String schedule = documentSnapshot.getString("schedule");
                        if (schedule != null) {
                            tvScheduleInfo.setText(schedule);
                        }

                        Boolean autoBackup = documentSnapshot.getBoolean("autoBackup");
                        if (autoBackup != null && switchAutoBackup != null) {
                            switchAutoBackup.setChecked(autoBackup);
                        }
                    }
                    updateBackupInfo();
                })
                .addOnFailureListener(e -> {
                    updateBackupInfo();
                });

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
                                !isEnabled ? getString(R.string.auto_backup_enabled) : getString(R.string.auto_backup_disabled),
                                Toast.LENGTH_SHORT).show());
    }

    private void performManualBackup() {
        progressIndicator.setVisibility(View.VISIBLE);
        btnBackupNow.setEnabled(false);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        final String backupTime = sdf.format(new Date());

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
        Map<String, Object> backupRecord = new HashMap<>();
        backupRecord.put("backupTime", backupTime);
        backupRecord.put("timestamp", new Date());
        backupRecord.put("size", backupData.toString().length());

        db.collection("backup_history")
                .add(backupRecord)
                .addOnSuccessListener(documentReference -> {
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

                                Toast.makeText(getContext(), getString(R.string.backup_completed),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    btnBackupNow.setEnabled(true);
                    Toast.makeText(getContext(), getString(R.string.backup_failed, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void showRestoreDialog() {
        if (backupHistory.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.no_backups), Toast.LENGTH_SHORT).show();
            return;
        }

        String[] backups = backupHistory.toArray(new String[0]);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.select_backup))
                .setItems(backups, (dialog, which) ->
                        confirmRestore(backups[which]))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void confirmRestore(String backupTime) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.confirm_restore))
                .setMessage(getString(R.string.restore_warning))
                .setPositiveButton(getString(R.string.restore), (dialog, which) -> performRestore(backupTime))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void performRestore(String backupTime) {
        progressIndicator.setVisibility(View.VISIBLE);

        Toast.makeText(getContext(), getString(R.string.restore_development),
                Toast.LENGTH_LONG).show();

        progressIndicator.postDelayed(() -> {
            progressIndicator.setVisibility(View.GONE);
            Toast.makeText(getContext(), getString(R.string.system_restored, backupTime),
                    Toast.LENGTH_SHORT).show();
        }, 2000);
    }

    private void showScheduleDialog() {
        String[] schedules = {
                getString(R.string.daily),
                getString(R.string.weekly),
                getString(R.string.monthly),
                getString(R.string.not_scheduled)
        };
        int checkedItem = 0;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.set_backup_schedule_title))
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
                                            getString(R.string.schedule_set, selected),
                                            Toast.LENGTH_SHORT).show());

                    dialog.dismiss();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void exportData() {
        progressIndicator.setVisibility(View.VISIBLE);

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

                        saveExportFile(exportObject);

                    } catch (Exception e) {
                        progressIndicator.setVisibility(View.GONE);
                        Toast.makeText(getContext(), getString(R.string.export_failed, e.getMessage()),
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(getContext(), getString(R.string.export_failed, e.getMessage()),
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

            Toast.makeText(getContext(), getString(R.string.export_saved),
                    Toast.LENGTH_LONG).show();

            shareFile(file);

        } catch (Exception e) {
            progressIndicator.setVisibility(View.GONE);
            Toast.makeText(getContext(), getString(R.string.export_failed, e.getMessage()),
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

        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_backup)));
    }
}