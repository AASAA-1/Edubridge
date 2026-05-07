package com.example.edubridge.teacher;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.BuildConfig;
import com.example.edubridge.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TriggerTrackerFragment extends Fragment {

    private static final String CLAUDE_API_KEY = BuildConfig.CLAUDE_API_KEY;
    private static final String CLAUDE_MODEL   = "claude-haiku-4-5-20251001";

    // ── Views ─────────────────────────────────────────────────────────────────
    private Spinner spinnerClass, spinnerStudent;
    private TextView tvClassStatus, tvEmptyIncidents;
    private RecyclerView rvIncidents;
    private MaterialButton btnLogIncident, btnAnalyze;
    private MaterialCardView cardAiResult;
    private ProgressBar pbAnalyzing;
    private RecyclerView rvAnalysisHistory;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseFirestore db;
    private String currentTeacherId;

    // ── Spinner data ──────────────────────────────────────────────────────────
    private final List<String> classNames   = new ArrayList<>();
    private final List<String> classIds     = new ArrayList<>();
    private final List<String> studentNames = new ArrayList<>();
    private final List<String> studentIds   = new ArrayList<>();

    // ── Selected state ────────────────────────────────────────────────────────
    private String selectedClassId    = null;
    private String selectedClassName  = null;
    private String selectedStudentId  = null;
    private String selectedStudentName = null;

    // ── Incidents list ────────────────────────────────────────────────────────
    private final List<BehaviorLogItem> logItems = new ArrayList<>();
    private BehaviorLogAdapter adapter;

    // ── Analysis history ──────────────────────────────────────────────────────
    private final List<AnalysisResultItem> analysisItems = new ArrayList<>();
    private AnalysisResultAdapter analysisAdapter;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_trigger_tracker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        currentTeacherId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        bindViews(view);
        setupToolbar(view);

        adapter = new BehaviorLogAdapter(logItems);
        adapter.setOnItemActionListener(new BehaviorLogAdapter.OnItemActionListener() {
            @Override
            public void onEdit(BehaviorLogItem item) {
                openEditIncident(item);
            }
            @Override
            public void onDelete(BehaviorLogItem item) {
                confirmDeleteIncident(item);
            }
        });
        rvIncidents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvIncidents.setAdapter(adapter);

        analysisAdapter = new AnalysisResultAdapter(analysisItems);
        rvAnalysisHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAnalysisHistory.setAdapter(analysisAdapter);

        spinnerClass.setEnabled(false);
        spinnerStudent.setEnabled(false);

        setupClassSpinnerListener();
        setupStudentSpinnerListener();
        setupButtonListeners();

        fetchAssignedClasses();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload the list whenever we return from LogIncidentFragment
        if (selectedStudentId != null) {
            loadIncidents(selectedStudentId);
        }
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void bindViews(View view) {
        spinnerClass      = view.findViewById(R.id.spinner_class);
        spinnerStudent    = view.findViewById(R.id.spinner_student);
        tvClassStatus     = view.findViewById(R.id.tv_class_status);
        tvEmptyIncidents  = view.findViewById(R.id.tv_empty_incidents);
        rvIncidents       = view.findViewById(R.id.rv_incidents);
        btnLogIncident    = view.findViewById(R.id.btn_log_incident);
        btnAnalyze        = view.findViewById(R.id.btn_analyze);
        cardAiResult      = view.findViewById(R.id.card_ai_result);
        pbAnalyzing       = view.findViewById(R.id.pb_analyzing);
        rvAnalysisHistory = view.findViewById(R.id.rv_analysis_history);
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    // ── Spinner listeners ─────────────────────────────────────────────────────

    private void setupClassSpinnerListener() {
        spinnerClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= classIds.size()) return;
                selectedClassId   = classIds.get(position);
                selectedClassName = classNames.get(position);
                loadStudentsForClass(selectedClassId);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupStudentSpinnerListener() {
        spinnerStudent.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= studentIds.size()) return;
                selectedStudentId   = studentIds.get(position);
                selectedStudentName = studentNames.get(position);
                loadIncidents(selectedStudentId);
                loadSavedAnalysis(selectedStudentId);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedStudentId = null;
            }
        });
    }

    // ── Button listeners ──────────────────────────────────────────────────────

    private void setupButtonListeners() {
        btnLogIncident.setOnClickListener(v -> {
            if (selectedStudentId == null) {
                Toast.makeText(getContext(),
                        getString(R.string.select_student_first), Toast.LENGTH_SHORT).show();
                return;
            }
            Bundle args = new Bundle();
            args.putString(LogIncidentFragment.ARG_STUDENT_ID,   selectedStudentId);
            args.putString(LogIncidentFragment.ARG_STUDENT_NAME, selectedStudentName);
            args.putString(LogIncidentFragment.ARG_CLASS_ID,     selectedClassId);
            args.putString(LogIncidentFragment.ARG_CLASS_NAME,   selectedClassName);

            LogIncidentFragment logFrag = new LogIncidentFragment();
            logFrag.setArguments(args);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, logFrag)
                    .addToBackStack(null)
                    .commit();
        });

        btnAnalyze.setOnClickListener(v -> {
            if (selectedStudentId == null) {
                Toast.makeText(getContext(),
                        getString(R.string.select_student_first), Toast.LENGTH_SHORT).show();
                return;
            }
            if (logItems.isEmpty()) {
                Toast.makeText(getContext(),
                        getString(R.string.no_incidents_to_analyze), Toast.LENGTH_SHORT).show();
                return;
            }
            analyzeWithClaude(new ArrayList<>(logItems));
        });
    }

    // ── Firebase: class loading ───────────────────────────────────────────────

    private void fetchAssignedClasses() {
        db.collection("classes")
                .whereEqualTo("teacherId", currentTeacherId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (getContext() == null || !isAdded()) return;
                    classNames.clear();
                    classIds.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            classNames.add(name);
                            classIds.add(doc.getId());
                        }
                    }

                    if (classNames.isEmpty()) {
                        tvClassStatus.setText(getString(R.string.no_classes_assigned));
                        tvClassStatus.setVisibility(View.VISIBLE);
                        spinnerClass.setVisibility(View.GONE);
                    } else {
                        tvClassStatus.setVisibility(View.GONE);
                        spinnerClass.setVisibility(View.VISIBLE);
                        spinnerClass.setAdapter(new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_spinner_dropdown_item,
                                classNames));
                        spinnerClass.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null || !isAdded()) return;
                    tvClassStatus.setText(getString(R.string.classes_load_failed));
                    tvClassStatus.setVisibility(View.VISIBLE);
                    spinnerClass.setVisibility(View.GONE);
                });
    }

    // ── Firebase: student loading ─────────────────────────────────────────────

    private void loadStudentsForClass(String classId) {
        selectedStudentId   = null;
        selectedStudentName = null;
        studentNames.clear();
        studentIds.clear();
        spinnerStudent.setEnabled(false);
        clearIncidentList();
        analysisItems.clear();
        if (analysisAdapter != null) analysisAdapter.notifyDataSetChanged();
        cardAiResult.setVisibility(View.GONE);

        db.collectionGroup("students")
                .get()
                .addOnSuccessListener(snap -> {
                    if (getContext() == null || !isAdded()) return;

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        if (!classId.equals(doc.getString("classId"))) continue;
                        String name      = doc.getString("name");
                        String studentId = doc.getString("studentId");
                        if (studentId == null || studentId.trim().isEmpty()) {
                            studentId = doc.getId();
                        }
                        if (name != null && !name.isEmpty()) {
                            studentNames.add(name);
                            studentIds.add(studentId);
                        }
                    }

                    if (studentNames.isEmpty()) {
                        Toast.makeText(getContext(),
                                getString(R.string.no_students_in_class),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        spinnerStudent.setAdapter(new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_spinner_dropdown_item,
                                studentNames));
                        spinnerStudent.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null || !isAdded()) return;
                    Toast.makeText(getContext(),
                            getString(R.string.students_load_failed), Toast.LENGTH_LONG).show();
                });
    }

    // ── Firebase: incident loading ────────────────────────────────────────────
    // Single whereEqualTo — no composite index needed. Sort client-side.

    private void loadIncidents(String studentId) {
        clearIncidentList();

        db.collection("behaviorLogs")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (getContext() == null || !isAdded()) return;

                    List<BehaviorLogItem> fetched = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        BehaviorLogItem item = new BehaviorLogItem();
                        item.setId(doc.getId());
                        item.setStudentId(doc.getString("studentId"));
                        item.setStudentName(doc.getString("studentName"));
                        item.setTeacherId(doc.getString("teacherId"));
                        item.setClassId(doc.getString("classId"));
                        item.setClassName(doc.getString("className"));
                        item.setSubject(doc.getString("subject"));
                        item.setDayOfWeek(doc.getString("dayOfWeek"));
                        item.setTimeOfDay(doc.getString("timeOfDay"));
                        item.setIncidentType(doc.getString("incidentType"));
                        item.setDescription(doc.getString("description"));
                        item.setLoggedAt(doc.getTimestamp("loggedAt"));
                        fetched.add(item);
                    }

                    // Client-side sort by loggedAt descending (newest first)
                    Collections.sort(fetched, (a, b) -> {
                        if (a.getLoggedAt() == null) return 1;
                        if (b.getLoggedAt() == null) return -1;
                        return Long.compare(
                                b.getLoggedAt().toDate().getTime(),
                                a.getLoggedAt().toDate().getTime());
                    });

                    logItems.addAll(fetched);
                    adapter.notifyDataSetChanged();
                    updateIncidentListVisibility();
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null || !isAdded()) return;
                    Toast.makeText(getContext(),
                            getString(R.string.incidents_load_failed), Toast.LENGTH_SHORT).show();
                    updateIncidentListVisibility();
                });
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void clearIncidentList() {
        logItems.clear();
        if (adapter != null) adapter.notifyDataSetChanged();
        updateIncidentListVisibility();
    }

    private void updateIncidentListVisibility() {
        if (logItems.isEmpty()) {
            rvIncidents.setVisibility(View.GONE);
            tvEmptyIncidents.setVisibility(View.VISIBLE);
        } else {
            rvIncidents.setVisibility(View.VISIBLE);
            tvEmptyIncidents.setVisibility(View.GONE);
        }
    }

    // ── Edit / Delete incidents ───────────────────────────────────────────────

    private void openEditIncident(BehaviorLogItem item) {
        Bundle args = new Bundle();
        args.putString(LogIncidentFragment.ARG_STUDENT_ID,    selectedStudentId);
        args.putString(LogIncidentFragment.ARG_STUDENT_NAME,  selectedStudentName);
        args.putString(LogIncidentFragment.ARG_CLASS_ID,      selectedClassId);
        args.putString(LogIncidentFragment.ARG_CLASS_NAME,    selectedClassName);
        args.putString(LogIncidentFragment.ARG_INCIDENT_ID,   item.getId());
        args.putString(LogIncidentFragment.ARG_SUBJECT,       item.getSubject());
        args.putString(LogIncidentFragment.ARG_DAY,           item.getDayOfWeek());
        args.putString(LogIncidentFragment.ARG_TIME,          item.getTimeOfDay());
        args.putString(LogIncidentFragment.ARG_INCIDENT_TYPE, item.getIncidentType());
        args.putString(LogIncidentFragment.ARG_DESCRIPTION,   item.getDescription());

        LogIncidentFragment frag = new LogIncidentFragment();
        frag.setArguments(args);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, frag)
                .addToBackStack(null)
                .commit();
    }

    private void confirmDeleteIncident(BehaviorLogItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Incident")
                .setMessage("Remove this incident from the log?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("behaviorLogs").document(item.getId())
                            .delete()
                            .addOnSuccessListener(v -> {
                                if (getContext() == null || !isAdded()) return;
                                logItems.remove(item);
                                adapter.notifyDataSetChanged();
                                updateIncidentListVisibility();
                            })
                            .addOnFailureListener(e -> {
                                if (getContext() == null || !isAdded()) return;
                                Toast.makeText(getContext(),
                                        getString(R.string.incidents_load_failed),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Analysis history ──────────────────────────────────────────────────────

    private void loadSavedAnalysis(String studentId) {
        analysisItems.clear();
        if (analysisAdapter != null) analysisAdapter.notifyDataSetChanged();
        cardAiResult.setVisibility(View.GONE);
        if (studentId == null) return;

        String key = currentTeacherId + "_" + studentId;
        db.collection("behaviorAnalysis")
                .whereEqualTo("teacherStudentKey", key)
                .get()
                .addOnSuccessListener(snap -> {
                    if (getContext() == null || !isAdded()) return;
                    List<AnalysisResultItem> fetched = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        AnalysisResultItem item = new AnalysisResultItem(
                                doc.getId(),
                                doc.getString("analysisText"),
                                doc.getTimestamp("analyzedAt"));
                        fetched.add(item);
                    }
                    // Sort newest first
                    Collections.sort(fetched, (a, b) -> {
                        if (a.getAnalyzedAt() == null) return 1;
                        if (b.getAnalyzedAt() == null) return -1;
                        return Long.compare(
                                b.getAnalyzedAt().toDate().getTime(),
                                a.getAnalyzedAt().toDate().getTime());
                    });
                    analysisItems.addAll(fetched);
                    if (!analysisItems.isEmpty()) analysisItems.get(0).setExpanded(true);
                    analysisAdapter.notifyDataSetChanged();
                    if (!analysisItems.isEmpty()) cardAiResult.setVisibility(View.VISIBLE);
                });
    }

    // ── AI Pattern Analysis ───────────────────────────────────────────────────

    private void analyzeWithClaude(List<BehaviorLogItem> incidents) {
        cardAiResult.setVisibility(View.VISIBLE);
        pbAnalyzing.setVisibility(View.VISIBLE);
        btnAnalyze.setEnabled(false);

        String prompt = buildPrompt(incidents);
        List<BehaviorLogItem> snapshot = new ArrayList<>(incidents);

        new Thread(() -> {
            String result = callClaudeApi(prompt);
            requireActivity().runOnUiThread(() -> {
                if (getContext() == null || !isAdded()) return;
                pbAnalyzing.setVisibility(View.GONE);
                btnAnalyze.setEnabled(true);
                if (result != null) {
                    saveAnalysisAndDeleteIncidents(result, snapshot);
                } else {
                    Toast.makeText(getContext(),
                            getString(R.string.ai_analysis_failed), Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void saveAnalysisAndDeleteIncidents(String analysisText, List<BehaviorLogItem> incidents) {
        Timestamp now = Timestamp.now();
        String key = currentTeacherId + "_" + selectedStudentId;

        Map<String, Object> data = new HashMap<>();
        data.put("teacherStudentKey", key);
        data.put("studentId",         selectedStudentId);
        data.put("studentName",       selectedStudentName);
        data.put("teacherId",         currentTeacherId);
        data.put("analysisText",      analysisText);
        data.put("analyzedAt",        now);

        db.collection("behaviorAnalysis").add(data)
                .addOnSuccessListener(ref -> {
                    if (getContext() == null || !isAdded()) return;
                    // Prepend to local list (newest first), auto-expand it
                    AnalysisResultItem newEntry = new AnalysisResultItem(ref.getId(), analysisText, now);
                    newEntry.setExpanded(true);
                    // Collapse any previously expanded entry
                    if (!analysisItems.isEmpty()) analysisItems.get(0).setExpanded(false);
                    analysisItems.add(0, newEntry);
                    analysisAdapter.notifyDataSetChanged();
                    cardAiResult.setVisibility(View.VISIBLE);
                });

        // Batch delete incidents
        WriteBatch batch = db.batch();
        for (BehaviorLogItem item : incidents) {
            batch.delete(db.collection("behaviorLogs").document(item.getId()));
        }
        batch.commit().addOnSuccessListener(v -> {
            if (getContext() == null || !isAdded()) return;
            logItems.clear();
            adapter.notifyDataSetChanged();
            updateIncidentListVisibility();
        });
    }

    private String buildPrompt(List<BehaviorLogItem> incidents) {
        SimpleDateFormat dateFmt = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.ENGLISH);
        StringBuilder sb = new StringBuilder();
        sb.append("Here are the behavioral incident logs for student: ")
          .append(selectedStudentName)
          .append("\n\n");

        for (int i = 0; i < incidents.size(); i++) {
            BehaviorLogItem item = incidents.get(i);
            sb.append("Incident ").append(i + 1).append(":\n");
            if (item.getLoggedAt() != null) {
                sb.append("  Date: ").append(dateFmt.format(item.getLoggedAt().toDate())).append("\n");
            }
            sb.append("  Day: ").append(item.getDayOfWeek()).append("\n");
            sb.append("  Time: ").append(item.getTimeOfDay()).append("\n");
            sb.append("  Subject/Context: ").append(item.getSubject()).append("\n");
            sb.append("  Incident Type: ").append(item.getIncidentType()).append("\n");
            if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                sb.append("  Description: ").append(item.getDescription()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("Please analyze these incidents and identify any patterns or triggers. ")
          .append("Be concise and actionable — a teacher should be able to read this in under a minute.");

        return sb.toString();
    }

    private String callClaudeApi(String userMessage) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", CLAUDE_MODEL);
            requestBody.put("max_tokens", 1024);
            requestBody.put("system",
                    "You are an expert behavioral analyst for an early childhood education platform. " +
                            "Your goal is to quickly identify triggers in a student's incident logs. " +
                            "CRITICAL INSTRUCTIONS: " +
                            "1. NO introductory or concluding filler text (e.g., do not say 'Here is the analysis'). " +
                            "2. Output EXACTLY three concise bullet points. " +
                            "3. Format as: " +
                            "• Pattern: [What is happening and when] " +
                            "• Likely Trigger: [The contextual cause] " +
                            "• Strategy: [One specific, immediate action the teacher can take]"
            );

            JSONArray messages = new JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.put(userMsg);
            requestBody.put("messages", messages);

            byte[] body = requestBody.toString().getBytes(StandardCharsets.UTF_8);

            URL url = new URL("https://api.anthropic.com/v1/messages");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-api-key", CLAUDE_API_KEY);
            conn.setRequestProperty("anthropic-version", "2023-06-01");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                android.util.Log.e("TriggerTracker", "Claude API HTTP " + responseCode);
                return null;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
            reader.close();
            conn.disconnect();

            JSONObject json = new JSONObject(response.toString());
            return json.getJSONArray("content").getJSONObject(0).getString("text");

        } catch (Exception e) {
            android.util.Log.e("TriggerTracker", "Claude API call failed", e);
            return null;
        }
    }
}
