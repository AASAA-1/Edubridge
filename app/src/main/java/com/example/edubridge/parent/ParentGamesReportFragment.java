package com.example.edubridge.parent;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.example.edubridge.shared.BigModeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ParentGamesReportFragment extends Fragment {

    private TextView tvSelectedChild;
    private LinearLayout reportContainer;
    private TextView tvEmptyReport;

    private FirebaseFirestore db;

    private final List<String> childNames = new ArrayList<>();
    private final List<String> childIds = new ArrayList<>();

    private String selectedChildId = "";
    private String selectedChildName = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_parent_games_report, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        tvSelectedChild = view.findViewById(R.id.tv_selected_child);
        reportContainer = view.findViewById(R.id.report_container);
        tvEmptyReport = view.findViewById(R.id.tv_empty_report);

        MaterialButton btnChangeChild = view.findViewById(R.id.btn_change_child);
        btnChangeChild.setOnClickListener(v -> showChildPicker());

        loadLinkedStudents();
    }

    private void loadLinkedStudents() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("students")
                .get()
                .addOnSuccessListener(snap -> {
                    childIds.clear();
                    childNames.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String studentId = value(doc.getString("studentId"));
                        String name = value(doc.getString("name"));
                        if (!studentId.isEmpty()) {
                            childIds.add(studentId);
                            childNames.add(name);
                        }
                    }

                    if (!childIds.isEmpty()) {
                        selectedChildId = childIds.get(0);
                        selectedChildName = childNames.get(0);
                    }

                    updateChildLabel();
                    loadReport();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showChildPicker() {
        if (childNames.isEmpty()) return;

        String[] items = childNames.toArray(new String[0]);
        int checked = Math.max(childIds.indexOf(selectedChildId), 0);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.games_report_change_child)
                .setSingleChoiceItems(items, checked, (dialog, which) -> {
                    selectedChildId = childIds.get(which);
                    selectedChildName = childNames.get(which);
                })
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    updateChildLabel();
                    loadReport();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void updateChildLabel() {
        if (selectedChildName.isEmpty()) {
            tvSelectedChild.setText(R.string.games_no_child_selected);
        } else {
            tvSelectedChild.setText(String.format(
                    getString(R.string.games_report_child), selectedChildName));
        }
    }

    private void loadReport() {
        if (selectedChildId.isEmpty()) {
            tvEmptyReport.setVisibility(View.VISIBLE);
            reportContainer.removeAllViews();
            return;
        }

        db.collection("game_sessions")
                .whereEqualTo("studentId", selectedChildId)
                .get()
                .addOnSuccessListener(snap -> {
                    Map<String, String> gameTitles = new LinkedHashMap<>();
                    Map<String, Integer> playCounts = new HashMap<>();
                    Map<String, Double> totalMinutes = new HashMap<>();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String gameId = value(doc.getString("gameId"));
                        String gameTitle = value(doc.getString("gameTitle"));
                        if (gameId.isEmpty()) continue;

                        gameTitles.put(gameId, gameTitle);
                        playCounts.put(gameId, playCounts.getOrDefault(gameId, 0) + 1);

                        Double duration = doc.getDouble("durationMinutes");
                        double prev = totalMinutes.getOrDefault(gameId, 0.0);
                        totalMinutes.put(gameId, prev + (duration != null ? duration : 0.0));
                    }

                    reportContainer.removeAllViews();

                    if (gameTitles.isEmpty()) {
                        tvEmptyReport.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvEmptyReport.setVisibility(View.GONE);
                    float scale = BigModeHelper.getScale(requireContext());

                    for (String gameId : gameTitles.keySet()) {
                        String title = gameTitles.get(gameId);
                        int count = playCounts.getOrDefault(gameId, 0);
                        double minutes = totalMinutes.getOrDefault(gameId, 0.0);

                        View row = LayoutInflater.from(requireContext())
                                .inflate(R.layout.item_game_report_entry, reportContainer, false);

                        TextView tvTitle = row.findViewById(R.id.tv_report_game_title);
                        TextView tvCount = row.findViewById(R.id.tv_report_play_count);
                        TextView tvMinutes = row.findViewById(R.id.tv_report_total_minutes);

                        tvTitle.setText(title);
                        tvCount.setText(String.format(Locale.getDefault(),
                                getString(R.string.games_play_count), count));
                        tvMinutes.setText(String.format(Locale.getDefault(),
                                getString(R.string.games_minutes), Math.round(minutes)));

                        if (scale != 1.0f) {
                            tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15 * scale);
                            tvCount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12 * scale);
                            tvMinutes.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12 * scale);
                        }

                        reportContainer.addView(row);
                    }
                })
                .addOnFailureListener(e -> {
                    tvEmptyReport.setVisibility(View.VISIBLE);
                    tvEmptyReport.setText(getString(R.string.games_report_no_sessions));
                });
    }

    private String value(String s) {
        return s == null ? "" : s;
    }
}
