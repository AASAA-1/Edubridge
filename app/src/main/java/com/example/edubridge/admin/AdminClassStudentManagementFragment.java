package com.example.edubridge.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminClassStudentManagementFragment extends Fragment {

    private RecyclerView recyclerView;
    private StudentSelectionAdapter adapter;
    private List<StudentSelectionData> studentList = new ArrayList<>();
    private FirebaseFirestore db;
    private String classId;

    public static AdminClassStudentManagementFragment newInstance(String classId) {
        AdminClassStudentManagementFragment fragment = new AdminClassStudentManagementFragment();
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_class_student_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.students_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StudentSelectionAdapter(studentList);
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.done_button).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        loadStudents();
    }

    private void loadStudents() {
        // Fetch all students using collectionGroup
        db.collectionGroup("students").get().addOnSuccessListener(querySnapshot -> {
            studentList.clear();
            for (QueryDocumentSnapshot doc : querySnapshot) {
                String name = doc.getString("name");
                String sId = doc.getString("studentId");
                String studentClassId = doc.getString("classId");
                String path = doc.getReference().getPath();

                boolean isSelected = classId != null && classId.equals(studentClassId);
                
                // Only show students who are either in this class or not in any class
                if (studentClassId == null || studentClassId.isEmpty() || isSelected) {
                    studentList.add(new StudentSelectionData(doc.getId(), name, sId, path, isSelected));
                }
            }
            adapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleStudentSelection(StudentSelectionData student) {
        String newClassId = student.isSelected ? "" : classId;
        db.document(student.fullPath).update("classId", newClassId)
                .addOnSuccessListener(aVoid -> {
                    student.isSelected = !student.isSelected;
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private static class StudentSelectionData {
        String id;
        String name;
        String studentId;
        String fullPath;
        boolean isSelected;

        StudentSelectionData(String id, String name, String studentId, String fullPath, boolean isSelected) {
            this.id = id;
            this.name = name;
            this.studentId = studentId;
            this.fullPath = fullPath;
            this.isSelected = isSelected;
        }
    }

    private class StudentSelectionAdapter extends RecyclerView.Adapter<StudentSelectionAdapter.ViewHolder> {
        private List<StudentSelectionData> students;

        StudentSelectionAdapter(List<StudentSelectionData> students) {
            this.students = students;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student_selection, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            StudentSelectionData student = students.get(position);
            holder.nameText.setText(student.name);
            holder.idText.setText(student.studentId);
            holder.tickIcon.setVisibility(student.isSelected ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> toggleStudentSelection(student));
        }

        @Override
        public int getItemCount() {
            return students.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameText, idText;
            ImageView tickIcon;

            ViewHolder(@NonNull View view) {
                super(view);
                nameText = view.findViewById(R.id.student_name);
                idText = view.findViewById(R.id.student_id);
                tickIcon = view.findViewById(R.id.selection_tick);
            }
        }
    }
}
