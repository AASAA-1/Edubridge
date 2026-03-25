package com.example.edubridge.admin;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.example.edubridge.shared.BigModeHelper;
import com.google.android.material.card.MaterialCardView;

public class AdminDashboardFragment extends Fragment {

    public AdminDashboardFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        applyBigMode(view);

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

        MaterialCardView manageEventsCard = view.findViewById(R.id.card_manage_events);
        manageEventsCard.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AdminEventManagementFragment())
                    .addToBackStack(null)
                    .commit();
        });

        MaterialCardView manageCurriculumCard = view.findViewById(R.id.card_curriculum);
        manageCurriculumCard.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AdminCurriculumManagementFragment())
                    .addToBackStack(null)
                    .commit();
        });

        MaterialCardView backupCard = view.findViewById(R.id.card_backup);
        backupCard.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AdminBackupFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void applyBigMode(View view) {

        // "import com.example.edubridge.shared.BigModeHelper;"
        // to access the scaling factor based on user settings
        // (either 100% normally or 140% with big button mode)
        float scale = BigModeHelper.getScale(requireContext());

        // listing all cards in this layout that need scaling applied
        int[] cardIds = {
                R.id.card_manage_users,
                R.id.card_manage_classes,
                R.id.card_manage_events,
                R.id.card_curriculum,
                R.id.card_backup
        };

        for (int id : cardIds) {

            // get each card and access its inner layout
            MaterialCardView card = view.findViewById(id);
            ViewGroup innerLayout = (ViewGroup) card.getChildAt(0);

            // scale padding of the inner layout to increase spacing inside cards
            int basePadding = (int) getResources().getDimension(R.dimen.card_padding);
            int scaledPadding = (int) (basePadding * scale);

            innerLayout.setPadding(
                    scaledPadding,
                    scaledPadding,
                    scaledPadding,
                    scaledPadding
            );

            // iterate through child xml in inner layout (text + icons) and scale them
            for (int i = 0; i < innerLayout.getChildCount(); i++) {

                View child = innerLayout.getChildAt(i);

                // scale icons
                if (child instanceof ImageView) {
                    int baseSize = (int) getResources().getDimension(R.dimen.icon_size);
                    int newSize = (int) (baseSize * scale);

                    ViewGroup.LayoutParams params = child.getLayoutParams();
                    params.width = newSize;
                    params.height = newSize;
                    child.setLayoutParams(params);
                }

                // scale text
                if (child instanceof TextView) {
                    float baseSize = getResources().getDimension(R.dimen.text_medium);

                    ((TextView) child).setTextSize(
                            TypedValue.COMPLEX_UNIT_PX,
                            baseSize * scale
                    );
                }
            }
        }
    }
}