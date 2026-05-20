package com.example.edubridge.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.example.edubridge.shared.LoginActivity;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class TeacherProfileFragment extends Fragment {

    private CircleImageView ivProfileImage;
    private TextView tvTeacherName, tvTeacherId, tvEmail, tvPhone, tvSubject, tvJoinDate;
    private TextView tvTotalStudents, tvMaterialsCount;
    private RecyclerView rvClasses;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;

    private ClassesAdapter classesAdapter;
    private List<String> classesList = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_teacher_profile, container, false);
        TextSizeHelper.applyScaleRecursively(v);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser().getUid();

        initializeViews(view);
        setupToolbar(view);
        setupClassesRecyclerView();
        setupClickListeners(view);
        loadTeacherProfile();
    }

    private void initializeViews(View view) {
        ivProfileImage = view.findViewById(R.id.iv_profile_image);
        tvTeacherName = view.findViewById(R.id.tv_teacher_name);
        tvTeacherId = view.findViewById(R.id.tv_teacher_id);
        tvEmail = view.findViewById(R.id.tv_email);
        tvPhone = view.findViewById(R.id.tv_phone);
        tvSubject = view.findViewById(R.id.tv_subject);
        tvJoinDate = view.findViewById(R.id.tv_join_date);
        tvTotalStudents = view.findViewById(R.id.tv_total_students);
        tvMaterialsCount = view.findViewById(R.id.tv_materials_count);
        rvClasses = view.findViewById(R.id.rv_classes);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void setupClassesRecyclerView() {
        classesAdapter = new ClassesAdapter(classesList);
        rvClasses.setLayoutManager(new LinearLayoutManager(getContext()));
        rvClasses.setAdapter(classesAdapter);
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.btn_edit_profile).setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new EditTeacherProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.btn_change_password).setOnClickListener(v -> {
            sendPasswordResetEmail();
        });

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            logout();
        });
    }

    private void loadTeacherProfile() {
        showLoading(true);

        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullname");
                        String email = documentSnapshot.getString("email");
                        String phone = documentSnapshot.getString("phone");
                        String subject = documentSnapshot.getString("subject");
                        String teacherId = documentSnapshot.getString("teacherId");
                        Date joinDate = documentSnapshot.getDate("createdAt");

                        tvTeacherName.setText(fullName != null ? fullName : "Teacher");
                        tvTeacherId.setText("ID: " + (teacherId != null ? teacherId : "N/A"));
                        tvEmail.setText(email != null ? email : "Not set");
                        tvPhone.setText(phone != null ? phone : "Not set");
                        tvSubject.setText(subject != null ? subject : "Not set");

                        if (joinDate != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                            tvJoinDate.setText(sdf.format(joinDate));
                        }

                        // Load additional stats
                        loadTeacherStats();
                        loadAssignedClasses();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(getContext(), getString(R.string.profile_load_failed, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadTeacherStats() {
        // Count materials uploaded
        db.collection("materials")
                .whereEqualTo("teacherId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int materialCount = queryDocumentSnapshots.size();
                    tvMaterialsCount.setText(String.valueOf(materialCount));
                });

        // Count total students in assigned classes
        db.collection("classes")
                .whereEqualTo("teacherId", currentUserId)
                .get()
                .addOnSuccessListener(classSnapshots -> {
                    int totalStudents = 0;
                    for (QueryDocumentSnapshot classDoc : classSnapshots) {
                        Long studentCount = classDoc.getLong("studentCount");
                        if (studentCount != null) {
                            totalStudents += studentCount;
                        }
                    }
                    tvTotalStudents.setText(String.valueOf(totalStudents));
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    tvTotalStudents.setText("0");
                });
    }

    private void loadAssignedClasses() {
        db.collection("classes")
                .whereEqualTo("teacherId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    classesList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String className = document.getString("name");
                        if (className != null) {
                            classesList.add(className);
                        }
                    }
                    classesAdapter.notifyDataSetChanged();
                });
    }

    private void sendPasswordResetEmail() {
        String email = auth.getCurrentUser().getEmail();
        if (email != null && !email.isEmpty()) {
            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(),
                                    getString(R.string.password_reset_sent, email),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(),
                                    getString(R.string.password_reset_failed, task.getException().getMessage()),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void logout() {
        auth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    // Adapter for classes
    private static class ClassesAdapter extends RecyclerView.Adapter<ClassesAdapter.ViewHolder> {
        private List<String> classes;

        public ClassesAdapter(List<String> classes) {
            this.classes = classes;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            TextSizeHelper.applyScaleRecursively(view);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.textView.setText(classes.get(position));
        }

        @Override
        public int getItemCount() {
            return classes.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}