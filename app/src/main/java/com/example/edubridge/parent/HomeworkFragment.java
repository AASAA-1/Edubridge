package com.example.edubridge.parent;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class HomeworkFragment extends Fragment {

    private final ArrayList<MaterialItem> allMaterials = new ArrayList<>();
    private final ArrayList<HomeworkItem> allHomework = new ArrayList<>();
    private final ArrayList<MaterialItem> filteredMaterials = new ArrayList<>();
    private final ArrayList<HomeworkItem> filteredHomework = new ArrayList<>();

    private LinearLayout materialsContainer;
    private LinearLayout homeworkContainer;
    private LinearLayout tabsContainer;
    private TextView materialsEmpty;
    private TextView homeworkEmpty;
    private TextView materialsViewMore;
    private TextView homeworkViewMore;

    private final ArrayList<String> subjects = new ArrayList<>();
    private String selectedSubject = "All";
    private String searchText = "";
    private boolean materialsExpanded = false;
    private boolean homeworkExpanded = false;
    private boolean materialsLoaded = false;
    private boolean homeworkLoaded = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_homework, container, false);

        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        materialsContainer = v.findViewById(R.id.materialsContainer);
        homeworkContainer = v.findViewById(R.id.homeworkContainer);
        tabsContainer = v.findViewById(R.id.tabsContainer);
        materialsEmpty = v.findViewById(R.id.materialsEmpty);
        homeworkEmpty = v.findViewById(R.id.homeworkEmpty);
        materialsViewMore = v.findViewById(R.id.materialsViewMore);
        homeworkViewMore = v.findViewById(R.id.homeworkViewMore);

        EditText searchInput = v.findViewById(R.id.searchInput);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchText = s.toString().trim();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        materialsViewMore.setOnClickListener(view -> {
            materialsExpanded = !materialsExpanded;
            renderMaterials();
        });

        homeworkViewMore.setOnClickListener(view -> {
            homeworkExpanded = !homeworkExpanded;
            renderHomework();
        });

        loadMaterials();
        loadHomework();

        return v;
    }

    private void loadMaterials() {
        FirebaseFirestore.getInstance()
                .collection("materials")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    allMaterials.clear();
                    snap.getDocuments().forEach(doc -> {
                        String id = doc.getId();
                        String title = value(doc.getString("title"));
                        String subject = value(doc.getString("subject"));
                        String fileType = value(doc.getString("fileType"));
                        String fileUrl = value(doc.getString("fileUrl"));
                        String createdByName = value(doc.getString("createdByName"));
                        Timestamp ts = doc.getTimestamp("createdAt");
                        long createdAtMillis = ts != null ? ts.toDate().getTime() : 0L;

                        allMaterials.add(new MaterialItem(
                                id,
                                title,
                                subject,
                                fileType,
                                fileUrl,
                                createdByName,
                                createdAtMillis
                        ));
                    });
                    materialsLoaded = true;
                    refreshSubjects();
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    allMaterials.clear();
                    materialsLoaded = true;
                    refreshSubjects();
                    applyFilters();
                });
    }

    private void loadHomework() {
        FirebaseFirestore.getInstance()
                .collection("homework")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    allHomework.clear();
                    snap.getDocuments().forEach(doc -> {
                        String id = doc.getId();
                        String title = value(doc.getString("title"));
                        String body = value(doc.getString("body"));
                        String subject = value(doc.getString("subject"));
                        String fileType = value(doc.getString("fileType"));
                        String fileUrl = value(doc.getString("fileUrl"));
                        String createdByName = value(doc.getString("createdByName"));
                        String dueDate = value(doc.getString("dueDate"));
                        Timestamp ts = doc.getTimestamp("createdAt");
                        long createdAtMillis = ts != null ? ts.toDate().getTime() : 0L;

                        allHomework.add(new HomeworkItem(
                                id,
                                title,
                                body,
                                subject,
                                fileType,
                                fileUrl,
                                createdByName,
                                dueDate,
                                createdAtMillis
                        ));
                    });
                    homeworkLoaded = true;
                    refreshSubjects();
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    allHomework.clear();
                    homeworkLoaded = true;
                    refreshSubjects();
                    applyFilters();
                });
    }

    private void refreshSubjects() {
        if (!materialsLoaded || !homeworkLoaded) return;

        Set<String> set = new LinkedHashSet<>();
        set.add("All");

        for (MaterialItem item : allMaterials) {
            if (!item.subject.isEmpty()) set.add(item.subject);
        }

        for (HomeworkItem item : allHomework) {
            if (!item.subject.isEmpty()) set.add(item.subject);
        }

        subjects.clear();
        subjects.addAll(set);

        if (!subjects.contains(selectedSubject)) {
            selectedSubject = "All";
        }

        renderTabs();
    }

    private void renderTabs() {
        tabsContainer.removeAllViews();

        for (String subject : subjects) {
            TextView tab = new TextView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(64), dp(40));
            params.setMarginEnd(dp(2));
            tab.setLayoutParams(params);
            tab.setGravity(Gravity.CENTER);
            tab.setText(subject);
            tab.setSingleLine(true);
            tab.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tab.setTypeface(tab.getTypeface(), android.graphics.Typeface.BOLD);
            tab.setBackgroundResource(subject.equals(selectedSubject) ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
            tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            tab.setOnClickListener(v -> {
                selectedSubject = subject;
                materialsExpanded = false;
                homeworkExpanded = false;
                renderTabs();
                applyFilters();
            });
            tabsContainer.addView(tab);
        }
    }

    private void applyFilters() {
        filteredMaterials.clear();
        filteredHomework.clear();

        String q = searchText.toLowerCase(Locale.US);

        for (MaterialItem item : allMaterials) {
            boolean subjectMatch = selectedSubject.equals("All") || selectedSubject.equalsIgnoreCase(item.subject);
            boolean searchMatch = q.isEmpty()
                    || item.title.toLowerCase(Locale.US).contains(q)
                    || item.subject.toLowerCase(Locale.US).contains(q)
                    || item.createdByName.toLowerCase(Locale.US).contains(q)
                    || item.fileType.toLowerCase(Locale.US).contains(q);

            if (subjectMatch && searchMatch) {
                filteredMaterials.add(item);
            }
        }

        for (HomeworkItem item : allHomework) {
            boolean subjectMatch = selectedSubject.equals("All") || selectedSubject.equalsIgnoreCase(item.subject);
            boolean searchMatch = q.isEmpty()
                    || item.title.toLowerCase(Locale.US).contains(q)
                    || item.body.toLowerCase(Locale.US).contains(q)
                    || item.subject.toLowerCase(Locale.US).contains(q)
                    || item.createdByName.toLowerCase(Locale.US).contains(q)
                    || item.fileType.toLowerCase(Locale.US).contains(q)
                    || item.dueDate.toLowerCase(Locale.US).contains(q);

            if (subjectMatch && searchMatch) {
                filteredHomework.add(item);
            }
        }

        renderMaterials();
        renderHomework();
    }

    private void renderMaterials() {
        materialsContainer.removeAllViews();

        if (filteredMaterials.isEmpty()) {
            materialsEmpty.setVisibility(View.VISIBLE);
            materialsViewMore.setVisibility(View.GONE);
            return;
        }

        materialsEmpty.setVisibility(View.GONE);

        int visibleCount = materialsExpanded ? filteredMaterials.size() : Math.min(filteredMaterials.size(), 2);

        for (int i = 0; i < visibleCount; i++) {
            MaterialItem item = filteredMaterials.get(i);
            View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_material_entry, materialsContainer, false);

            TextView title = row.findViewById(R.id.tvTitle);
            TextView meta = row.findViewById(R.id.tvMeta);
            ImageView fileIcon = row.findViewById(R.id.ivFileIcon);

            title.setText(item.title);
            meta.setText("Uploaded By: " + item.createdByName + " On " + formatDate(item.createdAtMillis));
            fileIcon.setImageResource(getFileIcon(item.fileType));
            fileIcon.setOnClickListener(v -> openFile(item.fileUrl));

            materialsContainer.addView(row);
        }

        materialsViewMore.setVisibility(filteredMaterials.size() > 2 ? View.VISIBLE : View.GONE);
        materialsViewMore.setText(materialsExpanded ? "View Less" : "View More");
    }

    private void renderHomework() {
        homeworkContainer.removeAllViews();

        if (filteredHomework.isEmpty()) {
            homeworkEmpty.setVisibility(View.VISIBLE);
            homeworkViewMore.setVisibility(View.GONE);
            return;
        }

        homeworkEmpty.setVisibility(View.GONE);

        int visibleCount = homeworkExpanded ? filteredHomework.size() : Math.min(filteredHomework.size(), 2);

        for (int i = 0; i < visibleCount; i++) {
            HomeworkItem item = filteredHomework.get(i);
            View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_assignment_entry, homeworkContainer, false);

            TextView title = row.findViewById(R.id.tvTitle);
            TextView meta = row.findViewById(R.id.tvMeta);
            TextView dueDate = row.findViewById(R.id.tvDueDate);
            ImageView fileIcon = row.findViewById(R.id.ivFileIcon);

            title.setText(item.title);
            meta.setText("Uploaded By: " + item.createdByName + " On " + formatDate(item.createdAtMillis));
            dueDate.setText("Due Date: " + item.dueDate);
            fileIcon.setImageResource(getFileIcon(item.fileType));
            fileIcon.setOnClickListener(v -> openFile(item.fileUrl));

            homeworkContainer.addView(row);
        }

        homeworkViewMore.setVisibility(filteredHomework.size() > 2 ? View.VISIBLE : View.GONE);
        homeworkViewMore.setText(homeworkExpanded ? "View Less" : "View More");
    }

    private int getFileIcon(String fileType) {
        String type = value(fileType).toLowerCase(Locale.US);
        if (type.contains("pdf")) return R.drawable.ic_pdf;
        if (type.contains("word") || type.contains("doc")) return R.drawable.ic_word;
        return R.drawable.ic_word;
    }

    private void openFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) return;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fileUrl));
        startActivity(intent);
    }

    private String formatDate(long millis) {
        if (millis == 0L) return "";
        return new SimpleDateFormat("d MMM yyyy", Locale.US).format(new Date(millis));
    }

    private String value(String s) {
        return s == null ? "" : s;
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        );
    }
}