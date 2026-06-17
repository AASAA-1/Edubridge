package com.example.edubridge.teacher;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.ViewGroup;

import com.example.edubridge.R;
import com.example.edubridge.shared.BigModeHelper;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LogIncidentFragment extends Fragment {

    public static final String ARG_STUDENT_ID     = "studentId";
    public static final String ARG_STUDENT_NAME   = "studentName";
    public static final String ARG_CLASS_ID       = "classId";
    public static final String ARG_CLASS_NAME     = "className";
    // Edit mode extras
    public static final String ARG_INCIDENT_ID    = "incidentId";
    public static final String ARG_SUBJECT        = "subject";
    public static final String ARG_DAY            = "day";
    public static final String ARG_TIME           = "time";
    public static final String ARG_INCIDENT_TYPE  = "incidentType";
    public static final String ARG_DESCRIPTION    = "description";

    private static final String[] SUBJECTS       = {"Math", "Arabic", "Science", "English", "Art", "PE", "Recess", "Other"};
    private static final String[] DAYS           = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday"};
    private static final String[] INCIDENT_TYPES = {"Aggression", "Disruption", "Withdrawal", "Outburst", "Refusal", "Other"};

    private Spinner spinnerSubject, spinnerDay, spinnerIncidentType;
    private TextView tvTimePicker;
    private EditText etDescription;
    private MaterialButton btnSave;

    private int selectedHour   = -1;
    private int selectedMinute = -1;

    private FirebaseFirestore db;
    private String teacherId;
    private String studentId, studentName, classId, className;
    private String incidentId; // non-null = edit mode

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log_incident, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        teacherId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Bundle args = getArguments();
        if (args != null) {
            studentId   = args.getString(ARG_STUDENT_ID, "");
            studentName = args.getString(ARG_STUDENT_NAME, "");
            classId     = args.getString(ARG_CLASS_ID, "");
            className   = args.getString(ARG_CLASS_NAME, "");
            incidentId  = args.getString(ARG_INCIDENT_ID, null);
        }

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        spinnerSubject      = view.findViewById(R.id.spinner_subject);
        spinnerDay          = view.findViewById(R.id.spinner_day);
        spinnerIncidentType = view.findViewById(R.id.spinner_incident_type);
        tvTimePicker        = view.findViewById(R.id.tv_time_picker);
        etDescription       = view.findViewById(R.id.et_description);
        btnSave             = view.findViewById(R.id.btn_save_incident);

        spinnerSubject.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, SUBJECTS));
        spinnerDay.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, DAYS));
        spinnerIncidentType.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, INCIDENT_TYPES));

        // Pre-fill in edit mode
        if (incidentId != null && args != null) {
            prefill(args);
        }

        tvTimePicker.setOnClickListener(v -> showTimePicker());
        btnSave.setOnClickListener(v -> saveIncident());

        TextSizeHelper.applyScaleRecursively(view);
        applyBigMode(view);
    }

    private void applyBigMode(View view) {
        float scale = BigModeHelper.getScale(requireContext());
        if (scale <= 1.0f) return;

        int scaledH = (int) (getResources().getDimensionPixelSize(R.dimen.button_height) * scale);
        ViewGroup.LayoutParams lp = btnSave.getLayoutParams();
        lp.height = scaledH;
        btnSave.setLayoutParams(lp);
    }

    private void prefill(Bundle args) {
        String subject  = args.getString(ARG_SUBJECT, "");
        String day      = args.getString(ARG_DAY, "");
        String time     = args.getString(ARG_TIME, "");
        String type     = args.getString(ARG_INCIDENT_TYPE, "");
        String desc     = args.getString(ARG_DESCRIPTION, "");

        int si = Arrays.asList(SUBJECTS).indexOf(subject);
        if (si >= 0) spinnerSubject.setSelection(si);

        int di = Arrays.asList(DAYS).indexOf(day);
        if (di >= 0) spinnerDay.setSelection(di);

        int ti = Arrays.asList(INCIDENT_TYPES).indexOf(type);
        if (ti >= 0) spinnerIncidentType.setSelection(ti);

        if (!desc.isEmpty()) etDescription.setText(desc);

        if (!time.isEmpty()) {
            tvTimePicker.setText(time);
            try {
                // Parse "9:05 AM" / "10:30 PM" format back to 24-hour values
                String[] colonParts = time.split(":");
                int hour = Integer.parseInt(colonParts[0].trim());
                String[] minAmPm = colonParts[1].trim().split(" ");
                int minute = Integer.parseInt(minAmPm[0].trim());
                String ampm = minAmPm[1].trim().toUpperCase(Locale.ENGLISH);
                if (ampm.equals("PM") && hour != 12) hour += 12;
                if (ampm.equals("AM") && hour == 12) hour = 0;
                selectedHour   = hour;
                selectedMinute = minute;
            } catch (Exception ignored) {}
        }
    }

    private void showTimePicker() {
        Calendar cal = Calendar.getInstance();
        int initHour   = selectedHour   >= 0 ? selectedHour   : cal.get(Calendar.HOUR_OF_DAY);
        int initMinute = selectedMinute >= 0 ? selectedMinute : cal.get(Calendar.MINUTE);

        new TimePickerDialog(requireContext(), (picker, hour, minute) -> {
            selectedHour   = hour;
            selectedMinute = minute;
            int h     = hour % 12 == 0 ? 12 : hour % 12;
            String ap = hour < 12 ? "AM" : "PM";
            tvTimePicker.setText(String.format(Locale.ENGLISH, "%d:%02d %s", h, minute, ap));
        }, initHour, initMinute, false).show();
    }

    private void saveIncident() {
        String time = tvTimePicker.getText().toString().trim();
        if (time.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.field_required), Toast.LENGTH_SHORT).show();
            return;
        }

        String description = etDescription.getText().toString().trim();
        String subject     = spinnerSubject.getSelectedItem().toString();
        String day         = spinnerDay.getSelectedItem().toString();
        String type        = spinnerIncidentType.getSelectedItem().toString();

        btnSave.setEnabled(false);

        Map<String, Object> log = new HashMap<>();
        log.put("studentId",    studentId);
        log.put("studentName",  studentName);
        log.put("teacherId",    teacherId);
        log.put("classId",      classId);
        log.put("className",    className);
        log.put("subject",      subject);
        log.put("dayOfWeek",    day);
        log.put("timeOfDay",    time);
        log.put("incidentType", type);
        log.put("description",  description);

        if (incidentId != null) {
            // Edit mode — update existing document, preserve original loggedAt
            db.collection("behaviorLogs").document(incidentId)
                    .update(log)
                    .addOnSuccessListener(v -> {
                        if (getContext() == null || !isAdded()) return;
                        Toast.makeText(getContext(),
                                getString(R.string.incident_saved), Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() == null || !isAdded()) return;
                        btnSave.setEnabled(true);
                        Toast.makeText(getContext(),
                                getString(R.string.incident_save_failed), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // New incident
            log.put("loggedAt", Timestamp.now());
            db.collection("behaviorLogs")
                    .add(log)
                    .addOnSuccessListener(ref -> {
                        if (getContext() == null || !isAdded()) return;
                        Toast.makeText(getContext(),
                                getString(R.string.incident_saved), Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    })
                    .addOnFailureListener(e -> {
                        if (getContext() == null || !isAdded()) return;
                        btnSave.setEnabled(true);
                        Toast.makeText(getContext(),
                                getString(R.string.incident_save_failed), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
