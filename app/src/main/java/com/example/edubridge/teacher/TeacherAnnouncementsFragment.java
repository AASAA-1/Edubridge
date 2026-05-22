package com.example.edubridge.teacher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.example.edubridge.parent.AnnouncementDetailsFragment;
import com.example.edubridge.parent.AnnouncementItem;
import com.example.edubridge.parent.AnnouncementsAdapter;
import com.example.edubridge.shared.BigModeHelper;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class TeacherAnnouncementsFragment extends Fragment {

    private final ArrayList<AnnouncementItem> items = new ArrayList<>();
    private AnnouncementsAdapter adapter;
    private TextView emptyText;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_teacher_announcements, container, false);
        TextSizeHelper.applyScaleRecursively(v);

        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack());

        emptyText = v.findViewById(R.id.emptyText);

        /*float scale = BigModeHelper.getScale(requireContext());
        emptyText.setTextSize(getResources().getDimension(R.dimen.text_medium)
                / getResources().getDisplayMetrics().density * scale);*/

        RecyclerView rv = v.findViewById(R.id.rvAnnouncements);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new AnnouncementsAdapter(items, item -> openDetails(item));
        rv.setAdapter(adapter);

        FloatingActionButton fab = v.findViewById(R.id.fabCreate);
        fab.setOnClickListener(view -> openCreateForm());

        loadAnnouncements();

        return v;
    }

    private void loadAnnouncements() {
        // Single-field orderBy is fine on Spark plan (no composite index needed)
        db.collection("announcements")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snap -> {
                    items.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String title     = doc.getString("title");
                        String body      = doc.getString("body");
                        String by        = doc.getString("createdByName");
                        String classId   = doc.getString("classId");
                        String className = doc.getString("className");
                        String visibility = doc.getString("visibility");

                        // Format createdAt Timestamp to readable date string
                        Timestamp ts = doc.getTimestamp("createdAt");
                        String date = ts != null
                                ? sdf.format(ts.toDate())
                                : (doc.getString("date") != null ? doc.getString("date") : "");

                        if (title == null) title = "Announcement";
                        if (body  == null) body  = "";
                        if (by    == null) by    = "School";

                        AnnouncementItem item = new AnnouncementItem(
                                doc.getId(), title, body, date, by);
                        item.classId    = classId;
                        item.className  = className;
                        item.visibility = visibility;
                        items.add(item);
                    }

                    updateEmptyState();
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> updateEmptyState());
    }

    private void updateEmptyState() {
        emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openDetails(AnnouncementItem item) {
        Bundle b = new Bundle();
        b.putString("title", item.title);
        b.putString("body",  item.body);
        b.putString("date",  item.date);
        b.putString("by",    item.createdByName);

        AnnouncementDetailsFragment frag = new AnnouncementDetailsFragment();
        frag.setArguments(b);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, frag)
                .addToBackStack(null)
                .commit();
    }

    private void openCreateForm() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new CreateAnnouncementFragment())
                .addToBackStack(null)
                .commit();
    }
}
