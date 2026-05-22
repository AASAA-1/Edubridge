package com.example.edubridge.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminClassScheduleManagementFragment extends Fragment {

    private static final String TAG = "ScheduleManagement";
    private String classId;
    private FirebaseFirestore db;
    private TableLayout scheduleTable;
    private Button saveButton;

    private List<CurriculumData> curriculumList = new ArrayList<>();
    private Map<String, Spinner> spinnerMap = new HashMap<>();
    private String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday"};
    private int numPeriods = 7;

    public static AdminClassScheduleManagementFragment newInstance(String classId) {
        AdminClassScheduleManagementFragment fragment = new AdminClassScheduleManagementFragment();
        Bundle args = new Bundle();
        args.putString("classId", classId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            classId = getArguments().getString("classId");
        }
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_admin_class_schedule_management, container, false);
        TextSizeHelper.applyScaleRecursively(v);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scheduleTable = view.findViewById(R.id.schedule_table);
        saveButton = view.findViewById(R.id.save_schedule_button);

        loadCurriculumAndExistingSchedule();

        saveButton.setOnClickListener(v -> saveSchedule());
    }

    private void loadCurriculumAndExistingSchedule() {
        db.collection("curriculum").get().addOnSuccessListener(querySnapshot -> {
            curriculumList.clear();
            curriculumList.add(new CurriculumData("", getString(R.string.other))); // Free slot
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                curriculumList.add(new CurriculumData(doc.getId(), doc.getString("name")));
            }

            db.collection("classes").document(classId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Map<String, Object> scheduleData = (Map<String, Object>) documentSnapshot.get("schedule");
                    buildTable(scheduleData);
                } else {
                    buildTable(null);
                }
            });
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                Toast.makeText(getContext(), getString(R.string.classes_load_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buildTable(Map<String, Object> existingSchedule) {
        scheduleTable.removeAllViews();

        // Header Row
        TableRow headerRow = new TableRow(getContext());
        headerRow.addView(createHeaderTextView("Period"));
        for (String day : days) {
            headerRow.addView(createHeaderTextView(day));
        }
        scheduleTable.addView(headerRow);

        List<String> curriculumNames = new ArrayList<>();
        for (CurriculumData data : curriculumList) {
            curriculumNames.add(data.name);
        }

        for (int p = 1; p <= numPeriods; p++) {
            TableRow row = new TableRow(getContext());
            row.addView(createTextView("P" + p));

            for (String day : days) {
                Spinner spinner = new Spinner(getContext());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, curriculumNames);
                spinner.setAdapter(adapter);

                String key = day + "_" + p;
                spinnerMap.put(key, spinner);

                if (existingSchedule != null && existingSchedule.containsKey(key)) {
                    String savedId = (String) existingSchedule.get(key);
                    for (int i = 0; i < curriculumList.size(); i++) {
                        if (curriculumList.get(i).id.equals(savedId)) {
                            spinner.setSelection(i);
                            break;
                        }
                    }
                }

                row.addView(spinner);
            }
            scheduleTable.addView(row);
        }

        // Apply scaling to the entire table (includes all TextViews added above)
        TextSizeHelper.applyScaleRecursively(scheduleTable);
    }

    private TextView createHeaderTextView(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setPadding(16, 16, 16, 16);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private TextView createTextView(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setPadding(16, 16, 16, 16);
        return tv;
    }

    private void saveSchedule() {
        if (classId == null || classId.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.invalid_class_id), Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> scheduleToSave = new HashMap<>();
        for (Map.Entry<String, Spinner> entry : spinnerMap.entrySet()) {
            int selectedPos = entry.getValue().getSelectedItemPosition();
            String curriculumId = curriculumList.get(selectedPos).id;
            scheduleToSave.put(entry.getKey(), curriculumId);
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("schedule", scheduleToSave);

        db.collection("classes").document(classId)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), getString(R.string.schedule_saved), Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), getString(R.string.schedule_save_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private static class CurriculumData {
        String id;
        String name;
        CurriculumData(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}