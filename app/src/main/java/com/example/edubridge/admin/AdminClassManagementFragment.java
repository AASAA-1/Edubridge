package com.example.edubridge.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.edubridge.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class AdminClassManagementFragment extends Fragment {

    private RecyclerView recyclerView;
    private ClassAdapter adapter;
    private List<ClassData> fullClassList;
    private FirebaseFirestore db;
    private String currentSelectedGrade = "all";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_class_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        fullClassList = new ArrayList<>();

        recyclerView = view.findViewById(R.id.classes_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ClassAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        Spinner gradeSpinner = view.findViewById(R.id.grade_spinner);
        String[] grades = {"all", "Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5", "Grade 6", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"};

        gradeSpinner.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                grades
        ));

        gradeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSelectedGrade = grades[position];
                applyFilter();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        view.findViewById(R.id.add_class_button).setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AdminClassModificationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        loadClasses();
    }

    private void loadClasses() {
        db.collection("classes").get()
                .addOnSuccessListener(querySnapshot -> {
                    fullClassList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ClassData schoolClass = doc.toObject(ClassData.class);
                        if (schoolClass != null) {
                            schoolClass.id = doc.getId();
                            fullClassList.add(schoolClass);
                        }
                    }
                    applyFilter();
                });
    }

    private void applyFilter() {
        if (currentSelectedGrade.equals("all")) {
            adapter.updateList(new ArrayList<>(fullClassList));
            return;
        }

        List<ClassData> filtered = new ArrayList<>();
        for (ClassData schoolClass : fullClassList) {
            if (schoolClass.grade != null && schoolClass.grade.equals(currentSelectedGrade)) {
                filtered.add(schoolClass);
            }
        }
        adapter.updateList(filtered);
    }

    public static class ClassData {
        public String id;
        public String name;
        public String grade;
        public String teacherName;
        public String teacherId;
        public ClassData() {}
    }

    class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {
        private List<ClassData> classList;

        public ClassAdapter(List<ClassData> classList) {
            this.classList = classList;
        }

        @NonNull
        @Override
        public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.shared_class_list_item, parent, false);
            return new ClassViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
            ClassData schoolClass = classList.get(position);
            holder.nameText.setText(schoolClass.name);
            holder.teacherText.setText(schoolClass.teacherName != null ? schoolClass.teacherName : "No teacher assigned");
            holder.gradeText.setText(schoolClass.grade);

            holder.itemView.setOnClickListener(v -> {
                AdminClassModificationFragment fragment = new AdminClassModificationFragment();
                Bundle args = new Bundle();
                args.putString("classId", schoolClass.id);
                fragment.setArguments(args);

                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            });
        }

        @Override
        public int getItemCount() {
            return classList.size();
        }

        public void updateList(List<ClassData> newList) {
            classList = newList;
            notifyDataSetChanged();
        }

        class ClassViewHolder extends RecyclerView.ViewHolder {
            TextView nameText, teacherText, gradeText;
            public ClassViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.class_name);
                teacherText = itemView.findViewById(R.id.class_teacher);
                gradeText = itemView.findViewById(R.id.class_grade);
            }
        }
    }
}
