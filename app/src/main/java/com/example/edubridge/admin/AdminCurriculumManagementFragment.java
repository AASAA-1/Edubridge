package com.example.edubridge.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminCurriculumManagementFragment extends Fragment {

    private RecyclerView recyclerView;
    private Spinner gradeSpinner;
    private ImageButton addButton;

    private FirebaseFirestore db;

    private List<Map<String, Object>> curriculumList = new ArrayList<>();
    private List<String> curriculumIds = new ArrayList<>();

    public AdminCurriculumManagementFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_curriculum_management, container, false);

        recyclerView = view.findViewById(R.id.curriculum_recycler_view);
        gradeSpinner = view.findViewById(R.id.grade_spinner);
        addButton = view.findViewById(R.id.add_curriculum_button);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new CurriculumAdapter());

        db = FirebaseFirestore.getInstance();

        setupSpinner();
        loadCurriculum(null);

        addButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AdminCurriculumModificationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void setupSpinner() {

        List<String> grades = new ArrayList<>();
        grades.add("All");

        for (int i = 1; i <= 12; i++) {
            grades.add(String.valueOf(i)); // matches class_id_edit input
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                grades
        );

        gradeSpinner.setAdapter(adapter);

        gradeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String selected = gradeSpinner.getSelectedItem().toString();

                if (selected.equals("All")) {
                    loadCurriculum(null);
                } else {
                    loadCurriculum(selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadCurriculum(String gradeFilter) {

        Query query;

        if (gradeFilter == null) {
            query = db.collection("curriculum");
        } else {
            query = db.collection("curriculum")
                    .whereEqualTo("grade", gradeFilter);
        }

        query.get().addOnSuccessListener(snapshots -> {

            curriculumList.clear();
            curriculumIds.clear();

            for (DocumentSnapshot doc : snapshots) {
                curriculumList.add(doc.getData());
                curriculumIds.add(doc.getId());
            }

            recyclerView.getAdapter().notifyDataSetChanged();
        });
    }

    private class CurriculumAdapter extends RecyclerView.Adapter<CurriculumAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.shared_user_list_item, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

            Map<String, Object> item = curriculumList.get(position);
            holder.title.setText((String) item.get("name"));

            holder.itemView.setOnClickListener(v -> {

                AdminCurriculumModificationFragment fragment =
                        new AdminCurriculumModificationFragment();

                Bundle bundle = new Bundle();
                bundle.putString("curriculumId", curriculumIds.get(position));
                fragment.setArguments(bundle);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            });
        }

        @Override
        public int getItemCount() {
            return curriculumList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView title;

            ViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.user_full_name);
            }
        }
    }
}