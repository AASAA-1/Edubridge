package com.example.edubridge.parent;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.example.edubridge.shared.BigModeHelper;
import com.example.edubridge.shared.LocationTrackingService;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class StudentDashboardFragment extends Fragment {

    private static final String TAG = "StudentDashboard";
    private static final String PREFS = "pin_prefs";
    private static final String KEY_PIN = "parent_mode_pin";
    private static final int PERMISSION_REQUEST_LOCATION = 1001;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_student_dashboard, container, false);
        TextSizeHelper.applyScaleRecursively(v);
        applyBigMode(v);

        // Start location tracking for safety
        checkPermissionsAndStartTracking();

        v.findViewById(R.id.student_mode_btn).setOnClickListener(view ->
                showPinVerificationDialog());

        v.findViewById(R.id.btn_student_games).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new StudentGamesFragment())
                        .addToBackStack("student")
                        .commit());

        v.findViewById(R.id.btn_student_announcements).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new AnnouncementsFragment())
                        .addToBackStack("student")
                        .commit());

        v.findViewById(R.id.btn_student_calendar).setOnClickListener(view -> {
            ParentCalendarFragment calendarFragment = new ParentCalendarFragment();
            Bundle args = new Bundle();
            args.putBoolean("hideChildSelector", true);
            calendarFragment.setArguments(args);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, calendarFragment)
                    .addToBackStack("student")
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
                    .addToBackStack("student")
                    .commit();
        });

        v.findViewById(R.id.btn_student_materials).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new HomeworkFragment())
                        .addToBackStack("student")
                        .commit());

        v.findViewById(R.id.btn_student_attendance).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new StudentAttendanceFragment())
                        .addToBackStack("student")
                        .commit());

        return v;
    }

    private void checkPermissionsAndStartTracking() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
        } else {
            startTrackingService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTrackingService();
            } else {
                Toast.makeText(getContext(), "Location permission denied. Safety tracking will not work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startTrackingService() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("students").limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String studentId = queryDocumentSnapshots.getDocuments().get(0).getString("studentId");
                        if (studentId == null) studentId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        
                        Log.d(TAG, "Starting LocationTrackingService for student: " + studentId);
                        Intent intent = new Intent(requireContext(), LocationTrackingService.class);
                        intent.setAction(LocationTrackingService.ACTION_START_TRACKING);
                        intent.putExtra(LocationTrackingService.EXTRA_STUDENT_ID, studentId);
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            requireContext().startForegroundService(intent);
                        } else {
                            requireContext().startService(intent);
                        }
                    } else {
                        Log.w(TAG, "No students found for this parent to track.");
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching student for tracking", e));
    }

    private void stopTrackingService() {
        Intent intent = new Intent(requireContext(), LocationTrackingService.class);
        intent.setAction(LocationTrackingService.ACTION_STOP_TRACKING);
        requireContext().startService(intent);
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
                        // Stop tracking when leaving student mode
                        stopTrackingService();

                        androidx.fragment.app.FragmentManager fm = requireActivity().getSupportFragmentManager();
                        fm.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        fm.beginTransaction()
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