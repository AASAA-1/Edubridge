package com.example.edubridge.parent;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.edubridge.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;

public class AnnouncementsFragment extends Fragment {

    private final ArrayList<AnnouncementItem> items = new ArrayList<>();
    private AnnouncementsAdapter adapter;
    private TextView emptyText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_announcements, container, false);

        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        emptyText = v.findViewById(R.id.emptyText);
        RecyclerView rv = v.findViewById(R.id.rvAnnouncements);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new AnnouncementsAdapter(items, item -> {
            AnnouncementDetailsFragment d = new AnnouncementDetailsFragment();
            Bundle b = new Bundle();
            b.putString("title", item.title);
            b.putString("body", item.body);
            b.putString("date", item.date);
            b.putString("by", item.createdByName);
            d.setArguments(b);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, d)
                    .addToBackStack(null)
                    .commit();
        });

        rv.setAdapter(adapter);

        loadAnnouncements();

        return v;
    }

    private void loadAnnouncements() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("announcements")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(snap -> {
                    items.clear();

                    snap.getDocuments().forEach(doc -> {
                        String id = doc.getId();
                        String title = doc.getString("title");
                        String body = doc.getString("body");
                        String date = doc.getString("date");
                        String by = doc.getString("createdByName");

                        if (title == null) title = "Announcement";
                        if (body == null) body = "";
                        if (date == null) date = "";
                        if (by == null) by = "School";

                        items.add(new AnnouncementItem(id, title, body, date, by));
                    });

                    if (items.isEmpty()) setDummy();

                    emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    setDummy();
                    emptyText.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                });
    }

    private void setDummy() {
        items.clear();
        items.add(new AnnouncementItem("1", "Web Systems", "Assignment due Sunday 11:59 PM.", "Today", "Mr. Mohammed"));
        items.add(new AnnouncementItem("2", "Database", "Quiz next week. Review chapters 3–5.", "This week", "Mrs. Sara"));
    }
}
