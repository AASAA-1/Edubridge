package com.example.edubridge.parent;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.edubridge.R;
import com.example.edubridge.shared.TextSizeHelper;
import com.example.edubridge.shared.messaging.ConversationListFragment;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import com.example.edubridge.shared.BigModeHelper;

public class ParentDashboardFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_parent_dashboard, container, false);
        TextSizeHelper.applyScaleRecursively(v);
        applyBigMode(v);
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

        v.findViewById(R.id.btn_announcements).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AnnouncementsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        v.findViewById(R.id.btn_schedule).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ScheduleFragment())
                    .addToBackStack(null)
                    .commit();
        });

        v.findViewById(R.id.btn_materials).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeworkFragment())
                    .addToBackStack(null)
                    .commit();
        });

        v.findViewById(R.id.btn_attendance).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ParentAttendanceFragment())
                    .addToBackStack(null)
                    .commit();
        });

        v.findViewById(R.id.btn_profile).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ParentProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        v.findViewById(R.id.btn_calendar).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ParentCalendarFragment())
                    .addToBackStack(null)
                    .commit();
        });
        v.findViewById(R.id.btn_games).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ParentGamesReportFragment())
                    .addToBackStack(null)
                    .commit();
        });
        v.findViewById(R.id.btn_live_monitoring).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ParentLiveClassFragment())
                    .addToBackStack(null)
                    .commit();
        });
        v.findViewById(R.id.btn_student_needs).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SetStudentNeedsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return v;
    }
    private void applyBigMode(View view) {
        float scale = BigModeHelper.getScale(requireContext());

        int[] cardIds = {
                R.id.btn_attendance,
                R.id.btn_calendar,
                R.id.btn_reports,
                R.id.btn_schedule,
                R.id.btn_messages,
                R.id.btn_announcements,
                R.id.btn_materials,
                R.id.btn_profile,
                R.id.btn_live_monitoring,
                R.id.btn_games,
                R.id.btn_student_needs
        };

        for (int id : cardIds) {
            CardView card = view.findViewById(id);
            if (card == null) continue;
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
