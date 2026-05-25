package com.example.edubridge.teacher;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.edubridge.R;
import com.example.edubridge.shared.TextSizeHelper;
import com.example.edubridge.shared.messaging.ConversationListFragment;

public class TeacherDashboardFragment extends Fragment {

    public TeacherDashboardFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_teacher_dashboard, container, false);
        TextSizeHelper.applyScaleRecursively(v);

        // Row 1
        v.findViewById(R.id.cardAttendance).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TeacherAttendanceFragment())
                    .addToBackStack(null)
                    .commit();
        });

        v.findViewById(R.id.cardProgress).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new StudentProgressFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Row 2
        v.findViewById(R.id.cardMessages).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ConversationListFragment())
                        .addToBackStack(null)
                        .commit());

        v.findViewById(R.id.cardAnnouncements).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TeacherAnnouncementsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Row 3
        v.findViewById(R.id.cardUpload).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TeacherUploadMaterialFragment())
                    .addToBackStack(null)
                    .commit();
        });

        v.findViewById(R.id.cardCalendar).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TeacherCalendarFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Row 4
        v.findViewById(R.id.cardProfile).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TeacherProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });
        v.findViewById(R.id.cardTriggerTracker).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TriggerTrackerFragment())
                    .addToBackStack(null)
                    .commit();
        });
        v.findViewById(R.id.cardPostGame).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TeacherPostGameFragment())
                    .addToBackStack(null)
                    .commit();
        });
        v.findViewById(R.id.cardLiveMonitoring).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TeacherLiveMonitoringFragment())
                    .addToBackStack(null)
                    .commit();
        });
        v.findViewById(R.id.cardCheckNeeds).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new CheckStudentNeedsFragment())
                    .addToBackStack(null)
                    .commit();
        });
        return v;
    }
}