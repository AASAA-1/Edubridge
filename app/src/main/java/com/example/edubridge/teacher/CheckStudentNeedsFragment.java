package com.example.edubridge.teacher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.example.edubridge.parent.ParentStudentReportFragment;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CheckStudentNeedsFragment extends Fragment {

    private Spinner spinnerStudent;
    private TextView tvLastUpdated, tvDietaryDesc, tvSleepDesc, tvBathroomDesc;
    private TextView tvDietaryNotes, tvSleepNotes, tvBathroomNotes;
    private RadioGroup rgDietary, rgSleep, rgBathroom;
    private TextInputEditText etDietaryResp, etSleepResp, etBathroomResp;
    private FirebaseFirestore db;
    private String teacherId;
    private String teacherName = "Teacher";
    private List<StudentWithParent> studentList = new ArrayList<>();
    private String selectedStudentId;
    private String selectedParentId;
    private String selectedStudentName;

    private static class StudentWithParent {
        ParentStudentReportFragment.StudentInfo info;
        String parentId;
        StudentWithParent(ParentStudentReportFragment.StudentInfo info, String parentId) {
            this.info = info;
            this.parentId = parentId;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_check_student_needs, container, false);
        TextSizeHelper.applyScaleRecursively(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            teacherId = currentUser.getUid();
            fetchTeacherName();
        }

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        spinnerStudent = view.findViewById(R.id.spinner_student);
        tvLastUpdated = view.findViewById(R.id.tv_last_updated);
        
        tvDietaryDesc = view.findViewById(R.id.tv_dietary_desc);
        tvDietaryNotes = view.findViewById(R.id.tv_dietary_notes);
        etDietaryResp = view.findViewById(R.id.et_dietary_response);
        
        tvSleepDesc = view.findViewById(R.id.tv_sleep_desc);
        tvSleepNotes = view.findViewById(R.id.tv_sleep_notes);
        etSleepResp = view.findViewById(R.id.et_sleep_response);
        
        tvBathroomDesc = view.findViewById(R.id.tv_bathroom_desc);
        tvBathroomNotes = view.findViewById(R.id.tv_bathroom_notes);
        etBathroomResp = view.findViewById(R.id.et_bathroom_response);
        
        rgDietary = view.findViewById(R.id.rg_dietary);
        rgSleep = view.findViewById(R.id.rg_sleep);
        rgBathroom = view.findViewById(R.id.rg_bathroom);
        MaterialButton btnSave = view.findViewById(R.id.btn_save_check);
        MaterialButton btnSendReport = view.findViewById(R.id.btn_send_report);

        loadStudents();

        btnSave.setOnClickListener(v -> saveChecks());
        btnSendReport.setOnClickListener(v -> sendReportToParent());
    }

    private void fetchTeacherName() {
        if (teacherId == null) return;
        db.collection("users").document(teacherId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullname");
                        if (name != null && !name.isEmpty()) {
                            teacherName = name;
                        }
                    }
                });
    }

    private void loadStudents() {
        if (teacherId == null) return;
        db.collection("classes")
                .whereEqualTo("teacherId", teacherId)
                .get()
                .addOnSuccessListener(classSnap -> {
                    List<String> assignedClassIds = new ArrayList<>();
                    for (DocumentSnapshot doc : classSnap) {
                        assignedClassIds.add(doc.getId());
                    }

                    if (assignedClassIds.isEmpty()) {
                        if (getContext() != null)
                            Toast.makeText(getContext(), R.string.no_classes_assigned, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("users")
                            .whereEqualTo("usertype", "parent")
                            .get()
                            .addOnSuccessListener(parentSnapshots -> {
                                studentList.clear();
                                final int totalParents = parentSnapshots.size();
                                if (totalParents == 0) return;
                                final int[] processedParents = {0};

                                for (DocumentSnapshot parentDoc : parentSnapshots) {
                                    String parentId = parentDoc.getId();
                                    parentDoc.getReference().collection("students")
                                            .get()
                                            .addOnSuccessListener(studentSnapshots -> {
                                                for (DocumentSnapshot studentDoc : studentSnapshots) {
                                                    String cid = studentDoc.getString("classId");
                                                    if (cid != null && assignedClassIds.contains(cid)) {
                                                        ParentStudentReportFragment.StudentInfo student = new ParentStudentReportFragment.StudentInfo();
                                                        String sid = studentDoc.getString("studentId");
                                                        if (sid == null) sid = studentDoc.getId();
                                                        student.setId(sid);
                                                        student.setName(studentDoc.getString("name"));
                                                        
                                                        boolean exists = false;
                                                        for(StudentWithParent s : studentList) {
                                                            if(s.info.getId().equals(student.getId())) { exists = true; break; }
                                                        }
                                                        if(!exists) studentList.add(new StudentWithParent(student, parentId));
                                                    }
                                                }
                                                processedParents[0]++;
                                                if (processedParents[0] >= totalParents) {
                                                    updateStudentSpinner();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                processedParents[0]++;
                                                if (processedParents[0] >= totalParents) updateStudentSpinner();
                                            });
                                }
                            });
                });
    }

    private void updateStudentSpinner() {
        if (!isAdded() || getContext() == null) return;
        List<String> names = new ArrayList<>();
        for (StudentWithParent s : studentList) names.add(s.info.getName());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, names);
        spinnerStudent.setAdapter(adapter);

        spinnerStudent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStudentId = studentList.get(position).info.getId();
                selectedParentId = studentList.get(position).parentId;
                selectedStudentName = studentList.get(position).info.getName();
                loadNeedsAndChecks();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadNeedsAndChecks() {
        if (selectedStudentId == null) return;

        db.collection("studentNeeds").document(selectedStudentId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvDietaryDesc.setText(doc.getString("dietary"));
                        displayNotes(tvDietaryNotes, doc.getString("dietaryNotes"));
                        
                        tvSleepDesc.setText(doc.getString("sleep"));
                        displayNotes(tvSleepNotes, doc.getString("sleepNotes"));
                        
                        tvBathroomDesc.setText(doc.getString("bathroom"));
                        displayNotes(tvBathroomNotes, doc.getString("bathroomNotes"));
                        
                        com.google.firebase.Timestamp ts = doc.getTimestamp("lastUpdated");
                        if (ts != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                            tvLastUpdated.setText(getString(R.string.last_updated, sdf.format(ts.toDate())));
                            tvLastUpdated.setVisibility(View.VISIBLE);
                        } else {
                            tvLastUpdated.setVisibility(View.GONE);
                        }
                    } else {
                        tvDietaryDesc.setText(R.string.na);
                        tvDietaryNotes.setVisibility(View.GONE);
                        tvSleepDesc.setText(R.string.na);
                        tvSleepNotes.setVisibility(View.GONE);
                        tvBathroomDesc.setText(R.string.na);
                        tvBathroomNotes.setVisibility(View.GONE);
                        tvLastUpdated.setVisibility(View.GONE);
                    }
                    loadTodaysChecks();
                });
    }

    private void displayNotes(TextView tv, String notes) {
        if (notes != null && !notes.trim().isEmpty()) {
            tv.setText(getString(R.string.parent_notes_display, notes));
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private void loadTodaysChecks() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        db.collection("needsChecks")
                .document(selectedStudentId + "_" + today)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        setRadioGroupValue(rgDietary, doc.getString("dietaryCheck"));
                        setRadioGroupValue(rgSleep, doc.getString("sleepCheck"));
                        setRadioGroupValue(rgBathroom, doc.getString("bathroomCheck"));
                        
                        etDietaryResp.setText(doc.getString("dietaryResponse"));
                        etSleepResp.setText(doc.getString("sleepResponse"));
                        etBathroomResp.setText(doc.getString("bathroomResponse"));
                    } else {
                        rgDietary.clearCheck();
                        rgSleep.clearCheck();
                        rgBathroom.clearCheck();
                        etDietaryResp.setText("");
                        etSleepResp.setText("");
                        etBathroomResp.setText("");
                    }
                });
    }

    private void setRadioGroupValue(RadioGroup rg, String val) {
        if (val == null || rg == null) return;
        int yesId, noId, naId;
        int rgId = rg.getId();
        if (rgId == R.id.rg_dietary) {
            yesId = R.id.rb_dietary_yes; noId = R.id.rb_dietary_no; naId = R.id.rb_dietary_na;
        } else if (rgId == R.id.rg_sleep) {
            yesId = R.id.rb_sleep_yes; noId = R.id.rb_sleep_no; naId = R.id.rb_sleep_na;
        } else if (rgId == R.id.rg_bathroom) {
            yesId = R.id.rb_bathroom_yes; noId = R.id.rb_bathroom_no; naId = R.id.rb_bathroom_na;
        } else return;

        if (val.equals(getString(R.string.yes))) rg.check(yesId);
        else if (val.equals(getString(R.string.no))) rg.check(noId);
        else if (val.equals(getString(R.string.not_applicable))) rg.check(naId);
    }

    private String getRadioGroupValue(RadioGroup rg) {
        if (rg == null) return getString(R.string.na);
        int id = rg.getCheckedRadioButtonId();
        if (id == -1) return getString(R.string.na);
        
        if (id == R.id.rb_dietary_yes || id == R.id.rb_sleep_yes || id == R.id.rb_bathroom_yes)
            return getString(R.string.yes);
        if (id == R.id.rb_dietary_no || id == R.id.rb_sleep_no || id == R.id.rb_bathroom_no)
            return getString(R.string.no);
        if (id == R.id.rb_dietary_na || id == R.id.rb_sleep_na || id == R.id.rb_bathroom_na)
            return getString(R.string.not_applicable);
            
        return getString(R.string.na);
    }

    private void saveChecks() {
        if (selectedStudentId == null) {
            if (getContext() != null)
                Toast.makeText(getContext(), "Please select a student", Toast.LENGTH_SHORT).show();
            return;
        }

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Map<String, Object> checks = new HashMap<>();
        checks.put("dietaryCheck", getRadioGroupValue(rgDietary));
        checks.put("dietaryResponse", etDietaryResp.getText() != null ? etDietaryResp.getText().toString().trim() : "");
        checks.put("sleepCheck", getRadioGroupValue(rgSleep));
        checks.put("sleepResponse", etSleepResp.getText() != null ? etSleepResp.getText().toString().trim() : "");
        checks.put("bathroomCheck", getRadioGroupValue(rgBathroom));
        checks.put("bathroomResponse", etBathroomResp.getText() != null ? etBathroomResp.getText().toString().trim() : "");
        checks.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("needsChecks")
                .document(selectedStudentId + "_" + today)
                .set(checks)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) Toast.makeText(getContext(), R.string.saved_successfully, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendReportToParent() {
        if (selectedStudentId == null || selectedParentId == null) {
            if (getContext() != null)
                Toast.makeText(getContext(), "Please select a student", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder message = new StringBuilder(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date())).append("\n\n");
        
        message.append("Dietary: ").append(getRadioGroupValue(rgDietary));
        String dResp = etDietaryResp.getText() != null ? etDietaryResp.getText().toString().trim() : "";
        if(!dResp.isEmpty()) message.append("\nNote: ").append(dResp);
        
        message.append("\n\nSleep: ").append(getRadioGroupValue(rgSleep));
        String sResp = etSleepResp.getText() != null ? etSleepResp.getText().toString().trim() : "";
        if(!sResp.isEmpty()) message.append("\nNote: ").append(sResp);
        
        message.append("\n\nBathroom: ").append(getRadioGroupValue(rgBathroom));
        String bResp = etBathroomResp.getText() != null ? etBathroomResp.getText().toString().trim() : "";
        if(!bResp.isEmpty()) message.append("\nNote: ").append(bResp);

        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", selectedParentId);
        notification.put("title", getString(R.string.needs_report_title, selectedStudentName));
        notification.put("body", message.toString());
        notification.put("type", "needs_report");
        notification.put("timestamp", com.google.firebase.Timestamp.now());
        notification.put("isRead", false);
        notification.put("senderID", teacherId);
        notification.put("senderName", teacherName);

        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(doc -> {
                    if (isAdded()) Toast.makeText(getContext(), R.string.report_sent_success, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) Toast.makeText(getContext(), getString(R.string.report_send_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
                });
    }
}
