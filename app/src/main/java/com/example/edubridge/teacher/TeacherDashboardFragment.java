package com.example.edubridge.teacher;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.edubridge.R;
import com.example.edubridge.shared.messaging.ConversationListFragment;

public class TeacherDashboardFragment extends Fragment {

    public TeacherDashboardFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_teacher_dashboard, container, false);

        v.findViewById(R.id.cardMessages).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ConversationListFragment())
                        .addToBackStack(null)
                        .commit());

        // Add to onCreateView after existing click listeners:

        v.findViewById(R.id.cardAttendance).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TeacherAttendanceFragment())
                    .addToBackStack(null)
                    .commit();
        });

        v.findViewById(R.id.cardUpload).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TeacherUploadMaterialFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return v;
    }
}
