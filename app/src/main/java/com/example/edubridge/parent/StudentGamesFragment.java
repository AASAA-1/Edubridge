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
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.example.edubridge.shared.BigModeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StudentGamesFragment extends Fragment {

    private static final String PREFS = "attendance_prefs";
    private static final String KEY_SELECTED_CHILD_ID = "selected_child_id";

    private LinearLayout gamesContainer;
    private TextView tvEmpty;
    private FirebaseFirestore db;

    private String selectedChildId = "";
    private String studentClass = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_games, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        gamesContainer = view.findViewById(R.id.games_container);
        tvEmpty = view.findViewById(R.id.tv_empty_games);

        selectedChildId = requireContext()
                .getSharedPreferences(PREFS, 0)
                .getString(KEY_SELECTED_CHILD_ID, "");

        loadStudentClass();
    }

    private void loadStudentClass() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            loadGames();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
                .document(uid)
                .collection("students")
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String studentId = value(doc.getString("studentId"));
                        if (studentId.isEmpty()) continue;

                        if (selectedChildId.isEmpty()) {
                            selectedChildId = studentId;
                            requireContext().getSharedPreferences(PREFS, 0)
                                    .edit()
                                    .putString(KEY_SELECTED_CHILD_ID, studentId)
                                    .putString("selected_child_name", value(doc.getString("name")))
                                    .apply();
                        }

                        if (studentId.equals(selectedChildId)) {
                            studentClass = value(doc.getString("class"));
                            if (studentClass.isEmpty()) {
                                studentClass = value(doc.getString("classId"));
                            }
                            break;
                        }
                    }
                    loadGames();
                })
                .addOnFailureListener(e -> loadGames());
    }

    private void loadGames() {
        db.collection("games")
                .get()
                .addOnSuccessListener(snap -> {
                    List<GameItem> games = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String assignedClass = value(doc.getString("assignedClass"));
                        boolean isForThisClass = assignedClass.equalsIgnoreCase("All Classes")
                                || (!studentClass.isEmpty()
                                    && assignedClass.equalsIgnoreCase(studentClass));
                        if (!isForThisClass) continue;

                        games.add(new GameItem(
                                doc.getId(),
                                value(doc.getString("title")),
                                value(doc.getString("description")),
                                value(doc.getString("url")),
                                value(doc.getString("teacherId")),
                                doc.getTimestamp("timestamp"),
                                assignedClass
                        ));
                    }

                    if (games.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        renderGames(games);
                    }
                })
                .addOnFailureListener(e -> {
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText(getString(R.string.games_no_games));
                });
    }

    private void renderGames(List<GameItem> games) {
        gamesContainer.removeAllViews();
        float scale = BigModeHelper.getScale(requireContext());

        for (GameItem game : games) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_game_entry, gamesContainer, false);

            TextView tvTitle = row.findViewById(R.id.tv_game_title);
            TextView tvClass = row.findViewById(R.id.tv_game_class);
            TextView tvDesc = row.findViewById(R.id.tv_game_description);
            MaterialButton btnPlay = row.findViewById(R.id.btn_play_game);

            tvTitle.setText(game.title);
            tvClass.setText(String.format(Locale.getDefault(),
                    getString(R.string.games_class_label), game.assignedClass));

            if (!game.description.isEmpty()) {
                tvDesc.setText(game.description);
                tvDesc.setVisibility(View.VISIBLE);
            }

            if (scale != 1.0f) {
                tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16 * scale);
                tvClass.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12 * scale);
                if (!game.description.isEmpty()) {
                    tvDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14 * scale);
                }
                ViewGroup.LayoutParams btnParams = btnPlay.getLayoutParams();
                btnParams.height = (int) (TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics()) * scale);
                btnPlay.setLayoutParams(btnParams);
            }

            btnPlay.setOnClickListener(v -> openGame(game));
            row.setOnClickListener(v -> openGame(game));

            gamesContainer.addView(row);
        }
    }

    private void openGame(GameItem game) {
        Bundle args = new Bundle();
        args.putString("gameId", game.id);
        args.putString("gameTitle", game.title);
        args.putString("gameUrl", game.url);
        args.putString("studentId", selectedChildId);

        StudentGameWebViewFragment webViewFragment = new StudentGameWebViewFragment();
        webViewFragment.setArguments(args);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, webViewFragment)
                .addToBackStack(null)
                .commit();
    }

    private String value(String s) {
        return s == null ? "" : s;
    }
}
