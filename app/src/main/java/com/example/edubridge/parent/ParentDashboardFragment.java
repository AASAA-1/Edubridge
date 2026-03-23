package com.example.edubridge.parent;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.edubridge.R;
import com.example.edubridge.shared.messaging.ConversationListFragment;

public class ParentDashboardFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_parent_dashboard, container, false);

        v.findViewById(R.id.student_mode_btn).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new StudentDashboardFragment())
                    .commit();
        });

        v.findViewById(R.id.btn_messages).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ConversationListFragment())
                        .addToBackStack(null)
                        .commit());

        v.findViewById(R.id.btn_reports).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ParentStudentReportFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return v;
    }
}
