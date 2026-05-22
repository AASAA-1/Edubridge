package com.example.edubridge.teacher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.example.edubridge.shared.BigModeHelper;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateAnnouncementFragment extends Fragment {

    private TextInputEditText etTitle;
    private TextInputEditText etBody;
    private Spinner spinnerClass;
    private Spinner spinnerVisibility;
    private View btnSubmit;
    private TextView tvClassStatus;
    private TextView tvClassLabel;
    private TextView tvVisibilityLabel;

    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUserFullName = "Teacher";

    // Parallel lists for class spinner — index maps name → Firestore document ID
    private final List<String> classNames = new ArrayList<>();
    private final List<String> classIds   = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_create_announcement, container, false);
        TextSizeHelper.applyScaleRecursively(v);

        db            = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack());

        etTitle           = v.findViewById(R.id.etTitle);
        etBody            = v.findViewById(R.id.etBody);
        btnSubmit         = v.findViewById(R.id.btnSubmit);
        spinnerClass      = v.findViewById(R.id.spinnerClass);
        spinnerVisibility = v.findViewById(R.id.spinnerVisibility);
        tvClassStatus     = v.findViewById(R.id.tvClassStatus);
        tvClassLabel      = v.findViewById(R.id.tvClassLabel);
        tvVisibilityLabel = v.findViewById(R.id.tvVisibilityLabel);

        float scale = BigModeHelper.getScale(requireContext());
        if (scale > 1.0f) {
            float smallSp   = getResources().getDimension(R.dimen.text_small)   / getResources().getDisplayMetrics().density;
            float captionSp = getResources().getDimension(R.dimen.text_caption) / getResources().getDisplayMetrics().density;
            tvClassLabel.setTextSize(smallSp * scale);
            tvVisibilityLabel.setTextSize(smallSp * scale);
            tvClassStatus.setTextSize(captionSp * scale);
            android.view.ViewGroup.LayoutParams lp = btnSubmit.getLayoutParams();
            lp.height = (int)(getResources().getDimensionPixelSize(R.dimen.button_height) * scale);
            btnSubmit.setLayoutParams(lp);
        }

        // Visibility options per spec Announcement.Visibility enum (Table 69)
        String[] visibilityOptions = {"All Users", "Parents", "Teachers", "Parents / Students"};
        spinnerVisibility.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                visibilityOptions));

        // Disable submit until classes are loaded
        btnSubmit.setEnabled(false);
        btnSubmit.setOnClickListener(view -> submitAnnouncement());

        // Fetch teacher's full name and assigned classes in parallel
        fetchUserName();
        fetchAssignedClasses();

        return v;
    }

    /** Resolves current user's fullname from the users collection. */
    private void fetchUserName() {
        db.collection("users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("fullname");
                    if (name != null && !name.isEmpty()) {
                        currentUserFullName = name;
                    }
                });
    }

    /**
     * Loads all classes assigned to the current teacher (teacherId == currentUserId).
     * Populates the class spinner. Disables submit and shows an error if none found.
     */
    private void fetchAssignedClasses() {
        db.collection("classes")
                .whereEqualTo("teacherId", currentUserId)
                .get()
                .addOnSuccessListener(snap -> {
                    classNames.clear();
                    classIds.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            classNames.add(name);
                            classIds.add(doc.getId());
                        }
                    }

                    if (classNames.isEmpty()) {
                        // No classes assigned — block submission
                        tvClassStatus.setText(getString(R.string.no_classes_assigned));
                        tvClassStatus.setVisibility(View.VISIBLE);
                        spinnerClass.setVisibility(View.GONE);
                        btnSubmit.setEnabled(false);
                    } else {
                        tvClassStatus.setVisibility(View.GONE);
                        spinnerClass.setVisibility(View.VISIBLE);
                        spinnerClass.setAdapter(new ArrayAdapter<>(
                                requireContext(),
                                android.R.layout.simple_spinner_dropdown_item,
                                classNames));
                        btnSubmit.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    tvClassStatus.setText(getString(R.string.classes_load_failed));
                    tvClassStatus.setVisibility(View.VISIBLE);
                    spinnerClass.setVisibility(View.GONE);
                });
    }

    /**
     * Writes one notification document per target user after an announcement is posted.
     * Doc ID: {uid}_announcement_{announcementId} — deterministic, so the same
     * announcement never produces a duplicate if re-saved.
     *
     * Visibility → user types notified:
     *   "Parents"           → parent
     *   "Teachers"          → teacher  (excludes the posting teacher)
     *   "Parents / Students"→ parent
     *   "All Users"         → parent + teacher
     */
    private void fanOutAnnouncementNotification(String announcementId, String title,
                                                 String body, String dateStr,
                                                 String visibility) {
        java.util.List<String> types = new java.util.ArrayList<>();
        switch (visibility) {
            case "Parents":
            case "Parents / Students":
                types.add("parent");
                break;
            case "Teachers":
                types.add("teacher");
                break;
            default: // "All Users"
                types.add("parent");
                types.add("teacher");
                break;
        }

        for (String userType : types) {
            db.collection("users").whereEqualTo("usertype", userType).get()
                    .addOnSuccessListener(snap -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                            String uid = doc.getId();
                            if (uid.equals(currentUserId)) continue; // skip self

                            java.util.Map<String, Object> notif = new java.util.HashMap<>();
                            notif.put("userId",     uid);
                            notif.put("senderID",   currentUserId);
                            notif.put("senderName", currentUserFullName);
                            notif.put("title",      title);
                            notif.put("body",       body);
                            notif.put("type",       "announcement");
                            notif.put("read",       false);
                            notif.put("count",      1L);
                            notif.put("refId",      announcementId);
                            notif.put("refDate",    dateStr);
                            notif.put("createdAt",  com.google.firebase.Timestamp.now());

                            db.collection("notifications")
                                    .document(uid + "_announcement_" + announcementId)
                                    .set(notif);
                        }
                    });
        }
    }

    private void submitAnnouncement() {
        String title      = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String body       = etBody.getText()  != null ? etBody.getText().toString().trim()  : "";
        String visibility = spinnerVisibility.getSelectedItem().toString();

        // Validation (spec Table 61): title and body are required
        if (title.isEmpty() || body.isEmpty()) {
            Toast.makeText(requireContext(),
                    getString(R.string.fill_required_fields), Toast.LENGTH_SHORT).show();
            if (title.isEmpty()) etTitle.setError(getString(R.string.field_required));
            if (body.isEmpty())  etBody.setError(getString(R.string.field_required));
            return;
        }

        // Resolve selected class
        int classIndex    = spinnerClass.getSelectedItemPosition();
        String classId    = classIds.get(classIndex);
        String className  = classNames.get(classIndex);

        btnSubmit.setEnabled(false);

        Timestamp now = Timestamp.now();
        String dateString = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                .format(now.toDate());

        Map<String, Object> data = new HashMap<>();
        data.put("title",         title);
        data.put("body",          body);
        data.put("createdBy",     currentUserId);
        data.put("createdByName", currentUserFullName);
        data.put("classId",       classId);
        data.put("className",     className);   // denormalized for display convenience
        data.put("visibility",    visibility);
        data.put("createdAt",     now);
        data.put("date",          dateString);

        db.collection("announcements")
                .add(data)
                .addOnSuccessListener(ref -> {
                    // Fan out a notification to every relevant user
                    fanOutAnnouncementNotification(
                            ref.getId(), title, body, dateString, visibility);

                    Toast.makeText(requireContext(),
                            getString(R.string.announcement_created), Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(requireContext(),
                            getString(R.string.announcement_failed), Toast.LENGTH_SHORT).show();
                });
    }
}
