package com.example.edubridge.teacher;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.view.ViewGroup;

import com.example.edubridge.R;
import com.example.edubridge.shared.BigModeHelper;
import com.example.edubridge.shared.TextSizeHelper;
import com.example.edubridge.shared.messaging.ConversationListFragment;
import com.google.android.material.card.MaterialCardView;

public class TeacherDashboardFragment extends Fragment {

    public TeacherDashboardFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_teacher_dashboard, container, false);
        TextSizeHelper.applyScaleRecursively(v);
        applyBigMode(v);

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

    private void applyBigMode(View view) {
        float scale = BigModeHelper.getScale(requireContext());
        if (scale <= 1.0f) return;

        int[] standardIds = {
                R.id.cardAttendance, R.id.cardProgress,
                R.id.cardMessages, R.id.cardAnnouncements,
                R.id.cardUpload, R.id.cardCalendar,
                R.id.cardTriggerTracker, R.id.cardCheckNeeds, R.id.cardProfile
        };
        int[] largeIds = { R.id.cardLiveMonitoring, R.id.cardPostGame };

        int baseH  = getResources().getDimensionPixelSize(R.dimen.dashboard_card_height);
        int largeH = getResources().getDimensionPixelSize(R.dimen.dashboard_card_height_large);

        for (int id : standardIds) {
            MaterialCardView card = view.findViewById(id);
            if (card == null) continue;
            ViewGroup.LayoutParams lp = card.getLayoutParams();
            lp.height = (int) (baseH * scale);
            card.setLayoutParams(lp);
        }
        for (int id : largeIds) {
            MaterialCardView card = view.findViewById(id);
            if (card == null) continue;
            ViewGroup.LayoutParams lp = card.getLayoutParams();
            lp.height = (int) (largeH * scale);
            card.setLayoutParams(lp);
        }
    }
}