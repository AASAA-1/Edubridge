package com.example.edubridge.parent;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.edubridge.R;

public class StudentDashboardFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_student_dashboard, container, false);

        v.findViewById(R.id.student_mode_btn).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ParentDashboardFragment())
                    .commit();
        });

        v.findViewById(R.id.btn_student_announcements).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AnnouncementsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        v.findViewById(R.id.btn_student_schedule).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ScheduleFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return v;
    }
}
