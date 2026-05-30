package com.example.edubridge.parent;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.example.edubridge.shared.BigModeHelper;
import com.example.edubridge.shared.TextSizeHelper;

public class StudentDashboardFragment extends Fragment {

    private static final String PREFS = "pin_prefs";
    private static final String KEY_PIN = "parent_mode_pin";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_student_dashboard, container, false);
        TextSizeHelper.applyScaleRecursively(v);
        applyBigMode(v);

        v.findViewById(R.id.student_mode_btn).setOnClickListener(view ->
                showPinVerificationDialog());

        v.findViewById(R.id.btn_student_games).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new StudentGamesFragment())
                        .addToBackStack(null)
                        .commit());

        v.findViewById(R.id.btn_student_announcements).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new AnnouncementsFragment())
                        .addToBackStack(null)
                        .commit());

        v.findViewById(R.id.btn_student_calendar).setOnClickListener(view -> {
            ParentCalendarFragment calendarFragment = new ParentCalendarFragment();
            Bundle args = new Bundle();
            args.putBoolean("hideChildSelector", true);
            calendarFragment.setArguments(args);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, calendarFragment)
                    .addToBackStack(null)
                    .commit();
        });

        v.findViewById(R.id.btn_student_schedule).setOnClickListener(view -> {
            ScheduleFragment scheduleFragment = new ScheduleFragment();
            Bundle args = new Bundle();
            args.putBoolean("hideChildSelector", true);
            scheduleFragment.setArguments(args);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, scheduleFragment)
                    .addToBackStack(null)
                    .commit();
        });

        v.findViewById(R.id.btn_student_materials).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new HomeworkFragment())
                        .addToBackStack(null)
                        .commit());

        v.findViewById(R.id.btn_student_attendance).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new StudentAttendanceFragment())
                        .addToBackStack(null)
                        .commit());

        return v;
    }

    private void showPinVerificationDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText pinInput = new EditText(requireContext());
        pinInput.setHint("Enter parent PIN");
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        layout.addView(pinInput);

        new AlertDialog.Builder(requireContext())
                .setTitle("Parent Mode")
                .setMessage("Enter the parent PIN to switch back.")
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String entered = pinInput.getText().toString().trim();
                    String savedPin = getSavedPin();

                    if (entered.equals(savedPin)) {
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new ParentDashboardFragment())
                                .commit();
                    } else {
                        Toast.makeText(requireContext(), "Incorrect PIN", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getSavedPin() {
        return requireContext()
                .getSharedPreferences(PREFS, 0)
                .getString(KEY_PIN, "");
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

        int baseCardHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 140, getResources().getDisplayMetrics());

        for (int id : cardIds) {
            CardView card = view.findViewById(id);
            if (card == null) continue;

            ViewGroup.LayoutParams cardParams = card.getLayoutParams();
            cardParams.height = (int) (baseCardHeight * scale);
            card.setLayoutParams(cardParams);

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