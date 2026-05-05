package com.example.edubridge.teacher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TeacherPostGameFragment extends Fragment {

    private EditText etTitle, etDescription, etUrl;
    private Spinner spinnerClass;
    private MaterialButton btnPost;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_teacher_post_game, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        etTitle = view.findViewById(R.id.et_game_title);
        etDescription = view.findViewById(R.id.et_game_description);
        etUrl = view.findViewById(R.id.et_game_url);
        spinnerClass = view.findViewById(R.id.spinner_game_class);
        btnPost = view.findViewById(R.id.btn_post_game);

        String[] classes = {"Class 5A", "Class 5B", "Class 6A", "Class 6B", "Class 7A", "All Classes"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, classes);
        spinnerClass.setAdapter(adapter);

        btnPost.setOnClickListener(v -> postGame());
    }

    private void postGame() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String url = etUrl.getText().toString().trim();
        String assignedClass = spinnerClass.getSelectedItem().toString();

        if (title.isEmpty()) {
            etTitle.setError(getString(R.string.games_error_title));
            return;
        }
        if (url.isEmpty()) {
            etUrl.setError(getString(R.string.games_error_url));
            return;
        }

        btnPost.setEnabled(false);

        String teacherId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        Map<String, Object> game = new HashMap<>();
        game.put("title", title);
        game.put("description", description);
        game.put("url", url);
        game.put("teacherId", teacherId);
        game.put("timestamp", new Date());
        game.put("assignedClass", assignedClass);

        db.collection("games")
                .add(game)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(getContext(), getString(R.string.games_posted_success),
                            Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    btnPost.setEnabled(true);
                    Toast.makeText(getContext(),
                            getString(R.string.games_posted_error) + ": " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
