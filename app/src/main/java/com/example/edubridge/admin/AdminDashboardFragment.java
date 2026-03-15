package com.example.edubridge.admin;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.edubridge.R;
import com.google.android.material.card.MaterialCardView;

public class AdminDashboardFragment extends Fragment {

    public AdminDashboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        MaterialCardView manageUsersCard = view.findViewById(R.id.card_manage_users);
        manageUsersCard.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AdminUserManagementFragment())
                    .addToBackStack(null)
                    .commit();
        });

        MaterialCardView manageClassesCard = view.findViewById(R.id.card_manage_classes);
        manageClassesCard.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AdminClassManagementFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }
}
