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
import com.example.edubridge.shared.messaging.ConversationListFragment;

public class ParentDashboardFragment extends Fragment {

    private static final String PREFS = "pin_prefs";
    private static final String KEY_PIN = "parent_mode_pin";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_parent_dashboard, container, false);
        TextSizeHelper.applyScaleRecursively(v);
        applyBigMode(v);

        v.findViewById(R.id.student_mode_btn).setOnClickListener(view -> {
            String savedPin = getSavedPin();
            if (TextUtils.isEmpty(savedPin)) {
                showCreatePinDialog();
            } else {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Student Mode")
                        .setMessage("What would you like to do?")
                        .setPositiveButton("Enter Student Mode", (dialog, which) -> navigateToStudentDashboard())
                        .setNeutralButton("Change PIN", (dialog, which) -> showChangePinDialog())
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        v.findViewById(R.id.btn_messages).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ConversationListFragment())
                        .addToBackStack(null)
                        .commit());

        v.findViewById(R.id.btn_reports).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ParentStudentReportFragment())
                        .addToBackStack(null)
                        .commit());

        v.findViewById(R.id.btn_announcements).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new AnnouncementsFragment())
                        .addToBackStack(null)
                        .commit());

        v.findViewById(R.id.btn_schedule).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ScheduleFragment())
                        .addToBackStack(null)
                        .commit());

        v.findViewById(R.id.btn_materials).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new HomeworkFragment())
                        .addToBackStack(null)
                        .commit());

        v.findViewById(R.id.btn_attendance).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ParentAttendanceFragment())
                        .addToBackStack(null)
                        .commit());

        v.findViewById(R.id.btn_profile).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ParentProfileFragment())
                        .addToBackStack(null)
                        .commit());

        v.findViewById(R.id.btn_calendar).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ParentCalendarFragment())
                        .addToBackStack(null)
                        .commit());

        v.findViewById(R.id.btn_games).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ParentGamesReportFragment())
                        .addToBackStack(null)
                        .commit());

        v.findViewById(R.id.btn_live_monitoring).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ParentLiveClassFragment())
                        .addToBackStack(null)
                        .commit());

        v.findViewById(R.id.btn_student_needs).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new SetStudentNeedsFragment())
                        .addToBackStack(null)
                        .commit());

        v.findViewById(R.id.btn_student_tracking).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new StudentTrackingFragment())
                        .addToBackStack(null)
                        .commit());

        return v;
    }

    private void showCreatePinDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText pinInput = new EditText(requireContext());
        pinInput.setHint("Enter new PIN (4-6 digits)");
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        layout.addView(pinInput);

        EditText confirmPinInput = new EditText(requireContext());
        confirmPinInput.setHint("Confirm PIN");
        confirmPinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        confirmPinInput.setPadding(0, 24, 0, 0);
        layout.addView(confirmPinInput);

        new AlertDialog.Builder(requireContext())
                .setTitle("Create Student Mode PIN")
                .setMessage("This PIN will be required to return to parent mode.")
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton("Set PIN", (dialog, which) -> {
                    String pin = pinInput.getText().toString().trim();
                    String confirmPin = confirmPinInput.getText().toString().trim();

                    if (TextUtils.isEmpty(pin)) {
                        Toast.makeText(requireContext(), "PIN cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (pin.length() < 4) {
                        Toast.makeText(requireContext(), "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!pin.equals(confirmPin)) {
                        Toast.makeText(requireContext(), "PINs do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    savePin(pin);
                    Toast.makeText(requireContext(), "PIN created successfully", Toast.LENGTH_SHORT).show();
                    navigateToStudentDashboard();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showChangePinDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText currentPinInput = new EditText(requireContext());
        currentPinInput.setHint("Current PIN");
        currentPinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        layout.addView(currentPinInput);

        EditText newPinInput = new EditText(requireContext());
        newPinInput.setHint("New PIN (4-6 digits)");
        newPinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        newPinInput.setPadding(0, 24, 0, 0);
        layout.addView(newPinInput);

        EditText confirmPinInput = new EditText(requireContext());
        confirmPinInput.setHint("Confirm new PIN");
        confirmPinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        confirmPinInput.setPadding(0, 24, 0, 0);
        layout.addView(confirmPinInput);

        new AlertDialog.Builder(requireContext())
                .setTitle("Change PIN")
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton("Change PIN", (dialog, which) -> {
                    String currentPin = currentPinInput.getText().toString().trim();
                    String newPin = newPinInput.getText().toString().trim();
                    String confirmPin = confirmPinInput.getText().toString().trim();

                    if (!currentPin.equals(getSavedPin())) {
                        Toast.makeText(requireContext(), "Current PIN is incorrect", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPin.length() < 4) {
                        Toast.makeText(requireContext(), "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newPin.equals(confirmPin)) {
                        Toast.makeText(requireContext(), "New PINs do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    savePin(newPin);
                    Toast.makeText(requireContext(), "PIN changed successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void navigateToStudentDashboard() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new StudentDashboardFragment())
                .commit();
    }

    private String getSavedPin() {
        return requireContext()
                .getSharedPreferences(PREFS, 0)
                .getString(KEY_PIN, "");
    }

    private void savePin(String pin) {
        requireContext()
                .getSharedPreferences(PREFS, 0)
                .edit()
                .putString(KEY_PIN, pin)
                .apply();
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
                R.id.btn_student_needs,
                R.id.btn_student_tracking
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
