package com.example.edubridge.parent;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.example.edubridge.shared.BigModeHelper;

public class StudentDashboardFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_student_dashboard, container, false);

        applyBigMode(v);

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

        v.findViewById(R.id.btn_student_materials).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeworkFragment())
                    .addToBackStack(null)
                    .commit();
        });

        v.findViewById(R.id.btn_student_attendance).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new StudentAttendanceFragment())
                    .addToBackStack(null)
                    .commit();
        });

        v.findViewById(R.id.btn_student_games).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new StudentGamesFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return v;
    }

    private void applyBigMode(View view) {
        float scale = BigModeHelper.getScale(requireContext());

        int[] cardIds = {
                R.id.btn_student_attendance,
                R.id.btn_student_calendar,
                R.id.btn_student_materials,
                R.id.btn_student_announcements,
                R.id.btn_student_schedule,
                R.id.btn_student_games
        };

        for (int id : cardIds) {
            CardView card = view.findViewById(id);
            ViewGroup innerLayout = (ViewGroup) card.getChildAt(0);

            int basePadding = (int) getResources().getDimension(R.dimen.card_padding);
            int scaledPadding = (int) (basePadding * scale);
            innerLayout.setPadding(scaledPadding, scaledPadding, scaledPadding, scaledPadding);

            for (int i = 0; i < innerLayout.getChildCount(); i++) {
                View child = innerLayout.getChildAt(i);

                if (child instanceof ImageView) {
                    int baseSize = (int) getResources().getDimension(R.dimen.icon_size);
                    int newSize = (int) (baseSize * scale);
                    ViewGroup.LayoutParams params = child.getLayoutParams();
                    params.width = newSize;
                    params.height = newSize;
                    child.setLayoutParams(params);
                }

                if (child instanceof TextView) {
                    float baseSize = getResources().getDimension(R.dimen.text_medium);
                    ((TextView) child).setTextSize(TypedValue.COMPLEX_UNIT_PX, baseSize * scale);
                }
            }
        }
    }
}
