package com.example.edubridge.parent;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.util.TypedValue;
import androidx.fragment.app.Fragment;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.example.edubridge.shared.BigModeHelper;
import com.example.edubridge.R;
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

        v.findViewById(R.id.btn_student_games).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new StudentGamesFragment())
                    .addToBackStack(null)
                    .commit();
        });
        v.findViewById(R.id.student_mode_btn).setOnClickListener(view -> {
            // Check if PIN is set
            String savedPin = getSavedPin();

            if (TextUtils.isEmpty(savedPin)) {
                // No PIN set yet - prompt to create one
                showCreatePinDialog();
            } else {
                // PIN exists - verify before switching
                showPinVerificationDialog();
            }
        });

        v.findViewById(R.id.btn_student_announcements).setOnClickListener(view -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AnnouncementsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        v.findViewById(R.id.btn_student_calendar).setOnClickListener(view -> {
            // Use ParentCalendarFragment but hide child selector for student mode
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
            // Use ScheduleFragment but hide child selector for student mode
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

        return v;
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

    /**
     * Dialog to CREATE a new PIN (shown when no PIN exists yet)
     */
    private void showCreatePinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Create Parent PIN");

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

        builder.setView(layout);

        builder.setPositiveButton("Set PIN", (dialog, which) -> {
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

            showPinVerificationDialog();
        });

        builder.setNegativeButton("Cancel", null);
        builder.setCancelable(false);
        builder.show();
    }

    /**
     * Dialog to VERIFY existing PIN before switching to parent mode
     */
    private void showPinVerificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Enter Parent PIN");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);

        EditText pinInput = new EditText(requireContext());
        pinInput.setHint("Enter PIN to access parent mode");
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        layout.addView(pinInput);

        builder.setView(layout);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            String enteredPin = pinInput.getText().toString().trim();
            String savedPin = getSavedPin();

            if (enteredPin.equals(savedPin)) {
                Toast.makeText(requireContext(), "Switching to parent mode...", Toast.LENGTH_SHORT).show();
                switchToParentMode();
            } else {
                Toast.makeText(requireContext(), "Incorrect PIN", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);

        builder.setNeutralButton("Change PIN", (dialog, which) -> {
            showChangePinDialog();
        });

        builder.setCancelable(false);
        builder.show();
    }

    /**
     * Dialog to CHANGE an existing PIN
     */
    private void showChangePinDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Change Parent PIN");

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

        builder.setView(layout);

        builder.setPositiveButton("Change PIN", (dialog, which) -> {
            String currentPin = currentPinInput.getText().toString().trim();
            String newPin = newPinInput.getText().toString().trim();
            String confirmPin = confirmPinInput.getText().toString().trim();
            String savedPin = getSavedPin();

            if (!currentPin.equals(savedPin)) {
                Toast.makeText(requireContext(), "Current PIN is incorrect", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(newPin)) {
                Toast.makeText(requireContext(), "New PIN cannot be empty", Toast.LENGTH_SHORT).show();
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

            switchToParentMode();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            showPinVerificationDialog();
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void switchToParentMode() {
        if (isAdded()) {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ParentDashboardFragment())
                    .commit();
        }
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