package com.example.edubridge.parent;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.example.edubridge.shared.CalendarEvent;
import com.example.edubridge.shared.TextSizeHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ParentCalendarFragment extends Fragment {

    private static final String TAG = "ParentCalendar";
    private static final String PREFS = "attendance_prefs";
    private static final String KEY_SELECTED_CHILD_ID = "selected_child_id";
    private static final String KEY_SELECTED_CHILD_NAME = "selected_child_name";
    private static final int MAX_UPCOMING = 5;

    private static final int COLOR_BLUE = 0;   // homework / assignment
    private static final int COLOR_RED = 1;    // test / quiz
    private static final int COLOR_GREEN = 2;  // everything else

    // Views
    private TextView tvSelectedChild;
    private TextView btnFilter;
    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private TextView tvMonthYear;
    private RecyclerView rvCalendarGrid;
    private LinearLayout llUpcomingEvents;
    private TextView tvReadMore;
    private TextView emptyText;

    // Popup
    private PopupWindow eventPopup;
    private TextView tvPopupTitle;
    private TextView tvPopupDescription;
    private TextView tvPopupDate;
    private TextView tvPopupReadMore;

    // Firebase
    private FirebaseFirestore db;
    private String currentUserId;

    // State
    private boolean hideChildSelector = false;
    private final List<String> childNames = new ArrayList<>();
    private final List<String> childIds = new ArrayList<>();
    private final Map<String, String> childNameById = new HashMap<>();
    private final Map<String, String> childClassIdById = new HashMap<>();
    private String selectedChildId = "";
    private String selectedChildName = "";

    private final List<CalendarEvent> allEvents = new ArrayList<>();
    private final Map<String, List<CalendarEvent>> eventsByDate = new HashMap<>();

    private Calendar displayCalendar;
    private CalendarGridAdapter gridAdapter;

    private boolean filterBlue = true;
    private boolean filterRed = true;
    private boolean filterGreen = true;
    private boolean showAllUpcoming = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_calendar, container, false);
        TextSizeHelper.applyScaleRecursively(v);

        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack());

        if (getArguments() != null) {
            hideChildSelector = getArguments().getBoolean("hideChildSelector", false);
        }

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        tvSelectedChild = v.findViewById(R.id.tvSelectedChild);
        btnFilter = v.findViewById(R.id.btnFilter);
        btnPrevMonth = v.findViewById(R.id.btnPrevMonth);
        btnNextMonth = v.findViewById(R.id.btnNextMonth);
        tvMonthYear = v.findViewById(R.id.tvMonthYear);
        rvCalendarGrid = v.findViewById(R.id.rvCalendarGrid);
        llUpcomingEvents = v.findViewById(R.id.llUpcomingEvents);
        tvReadMore = v.findViewById(R.id.tvReadMore);
        emptyText = v.findViewById(R.id.emptyText);

        if (hideChildSelector) {
            tvSelectedChild.setVisibility(View.GONE);
            btnFilter.setVisibility(View.GONE);
        } else {
            tvSelectedChild.setOnClickListener(view -> showChildPicker());
            btnFilter.setOnClickListener(view -> showFilterDialog());
        }

        initPopupWindow(inflater);

        displayCalendar = Calendar.getInstance();
        displayCalendar.set(Calendar.DAY_OF_MONTH, 1);
        displayCalendar.set(Calendar.HOUR_OF_DAY, 0);
        displayCalendar.set(Calendar.MINUTE, 0);
        displayCalendar.set(Calendar.SECOND, 0);
        displayCalendar.set(Calendar.MILLISECOND, 0);

        btnPrevMonth.setOnClickListener(view -> {
            dismissPopup();
            displayCalendar.add(Calendar.MONTH, -1);
            refreshCalendar();
        });
        btnNextMonth.setOnClickListener(view -> {
            dismissPopup();
            displayCalendar.add(Calendar.MONTH, 1);
            refreshCalendar();
        });

        gridAdapter = new CalendarGridAdapter();
        rvCalendarGrid.setLayoutManager(new GridLayoutManager(requireContext(), 7));
        rvCalendarGrid.setAdapter(gridAdapter);
        rvCalendarGrid.setNestedScrollingEnabled(false);

        tvReadMore.setOnClickListener(view -> {
            showAllUpcoming = !showAllUpcoming;
            tvReadMore.setText(showAllUpcoming
                    ? getString(R.string.view_less)
                    : getString(R.string.view_more));
            updateUpcomingEvents();
        });

        updateMonthLabel();
        buildCalendarGrid();
        loadParentStudentClasses();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!selectedChildId.isEmpty()) {
            loadEventsForSelectedChild();
        }
    }

    private void initPopupWindow(LayoutInflater inflater) {
        View popupView = inflater.inflate(R.layout.popup_event_detail, null);
        tvPopupTitle = popupView.findViewById(R.id.tvPopupTitle);
        tvPopupDescription = popupView.findViewById(R.id.tvPopupDescription);
        tvPopupDate = popupView.findViewById(R.id.tvPopupDate);
        tvPopupReadMore = popupView.findViewById(R.id.tvPopupReadMore);

        int widthPx = dpToPx(220);
        eventPopup = new PopupWindow(popupView, widthPx, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        eventPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        eventPopup.setElevation(dpToPx(6));
        eventPopup.setOutsideTouchable(true);
    }

    private void dismissPopup() {
        if (eventPopup != null && eventPopup.isShowing()) {
            eventPopup.dismiss();
        }
    }

    // ---- Firestore (same logic as before) ----

    private void loadParentStudentClasses() {
        if (currentUserId == null) return;

        db.collection("users")
                .document(currentUserId)
                .collection("students")
                .get()
                .addOnSuccessListener(snap -> {
                    childNames.clear();
                    childIds.clear();
                    childNameById.clear();
                    childClassIdById.clear();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String studentId = doc.getString("studentId");
                        String name = doc.getString("name");
                        String classId = doc.getString("classId");

                        if (studentId != null && !studentId.isEmpty()) {
                            childIds.add(studentId);
                            childNames.add(name != null ? name : "Unknown");
                            childNameById.put(studentId, name != null ? name : "Unknown");
                            if (classId != null) {
                                childClassIdById.put(studentId, classId);
                            }
                        }
                    }

                    String savedId = requireContext()
                            .getSharedPreferences(PREFS, 0)
                            .getString(KEY_SELECTED_CHILD_ID, "");

                    if (!savedId.isEmpty() && childNameById.containsKey(savedId)) {
                        selectedChildId = savedId;
                        selectedChildName = childNameById.get(savedId);
                    } else if (!childIds.isEmpty()) {
                        selectedChildId = childIds.get(0);
                        selectedChildName = childNames.get(0);
                        saveSelectedChild();
                    }

                    updateChildDisplay();

                    if (childIds.isEmpty()) {
                        showEmptyState("No students linked");
                    } else {
                        loadEventsForSelectedChild();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load students", e);
                    showEmptyState("Failed to load student data");
                });
    }

    private void loadEventsForSelectedChild() {
        if (selectedChildId.isEmpty()) return;

        String childClassId = childClassIdById.get(selectedChildId);
        if (childClassId == null || childClassId.isEmpty()) {
            showEmptyState(selectedChildName + " is not assigned to a group");
            return;
        }

        db.collection("events")
                .get()
                .addOnSuccessListener(snap -> {
                    allEvents.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String eventClassId = doc.getString("classId");
                        boolean show = eventClassId == null || eventClassId.isEmpty()
                                || eventClassId.equals(childClassId);

                        if (show) {
                            CalendarEvent event = new CalendarEvent();
                            event.setId(doc.getId());
                            event.setTitle(doc.getString("title"));
                            event.setDescription(doc.getString("description"));
                            event.setType(doc.getString("type"));
                            String rawStart = doc.getString("startAt");
                            if (rawStart == null) {
                                com.google.firebase.Timestamp ts = doc.getTimestamp("startAt");
                                if (ts != null) rawStart = sdf.format(ts.toDate());
                            }
                            event.setStartAt(rawStart != null ? rawStart.trim() : null);

                            String rawEnd = doc.getString("endAt");
                            if (rawEnd == null) {
                                com.google.firebase.Timestamp ts = doc.getTimestamp("endAt");
                                if (ts != null) rawEnd = sdf.format(ts.toDate());
                            }
                            event.setEndAt(rawEnd != null ? rawEnd.trim() : null);
                            event.setClassId(eventClassId);
                            event.setClassName(doc.getString("className"));

                            try {
                                if (event.getStartAt() != null && !event.getStartAt().isEmpty()) {
                                    event.setStartDate(sdf.parse(event.getStartAt()));
                                }
                                if (event.getEndAt() != null && !event.getEndAt().isEmpty()) {
                                    event.setEndDate(sdf.parse(event.getEndAt()));
                                }
                            } catch (ParseException e) {
                                Log.w(TAG, "Date parse error: " + event.getTitle(), e);
                            }

                            allEvents.add(event);
                        }
                    }

                    emptyText.setVisibility(View.GONE);
                    refreshCalendar();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load events", e);
                    showEmptyState("Failed to load events");
                });
    }

    private void showEmptyState(String message) {
        emptyText.setText(message);
        emptyText.setVisibility(View.VISIBLE);
    }

    // ---- Calendar display ----

    private void refreshCalendar() {
        buildEventsByDateMap();
        updateMonthLabel();
        buildCalendarGrid();
        updateUpcomingEvents();
        dismissPopup();
    }

    private void buildEventsByDateMap() {
        eventsByDate.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        for (CalendarEvent e : allEvents) {
            if (e.getStartDate() == null) continue;
            if (!passesFilter(e)) continue;
            String key = sdf.format(e.getStartDate());
            List<CalendarEvent> list = eventsByDate.get(key);
            if (list == null) {
                list = new ArrayList<>();
                eventsByDate.put(key, list);
            }
            list.add(e);
        }
    }

    private boolean passesFilter(CalendarEvent e) {
        int t = colorTypeOf(e.getType());
        if (t == COLOR_BLUE) return filterBlue;
        if (t == COLOR_RED) return filterRed;
        return filterGreen;
    }

    private int colorTypeOf(String type) {
        if (type == null) return COLOR_GREEN;
        String lower = type.toLowerCase();
        if (lower.contains("homework") || lower.contains("assignment")) return COLOR_BLUE;
        if (lower.contains("test") || lower.contains("quiz")) return COLOR_RED;
        return COLOR_GREEN;
    }

    private int solidColorOf(int colorType) {
        switch (colorType) {
            case COLOR_BLUE:  return 0xFF2196F3;
            case COLOR_RED:   return 0xFFF44336;
            default:          return 0xFF4CAF50;
        }
    }

    private int dominantColorType(List<CalendarEvent> events) {
        int result = COLOR_GREEN;
        for (CalendarEvent e : events) {
            int t = colorTypeOf(e.getType());
            if (t == COLOR_RED) return COLOR_RED;
            if (t == COLOR_BLUE) result = COLOR_BLUE;
        }
        return result;
    }

    private void updateMonthLabel() {
        SimpleDateFormat fmt = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(fmt.format(displayCalendar.getTime()));
    }

    private void buildCalendarGrid() {
        int year = displayCalendar.get(Calendar.YEAR);
        int month = displayCalendar.get(Calendar.MONTH);

        Calendar c = Calendar.getInstance();
        c.set(year, month, 1);
        // Monday-first offset: Mon=0, Tue=1, ..., Sun=6
        int firstDow = c.get(Calendar.DAY_OF_WEEK);
        int offset = (firstDow - Calendar.MONDAY + 7) % 7;
        int daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        List<CalendarDay> days = new ArrayList<>();

        for (int i = 0; i < offset; i++) {
            days.add(new CalendarDay(0, null));
        }
        for (int d = 1; d <= daysInMonth; d++) {
            c.set(year, month, d);
            String key = sdf.format(c.getTime());
            days.add(new CalendarDay(d, eventsByDate.get(key)));
        }
        int rem = days.size() % 7;
        if (rem != 0) {
            for (int i = 0; i < 7 - rem; i++) {
                days.add(new CalendarDay(0, null));
            }
        }

        gridAdapter.setData(days, year, month);
    }

    private void updateUpcomingEvents() {
        llUpcomingEvents.removeAllViews();

        List<CalendarEvent> upcoming = new ArrayList<>();
        for (CalendarEvent e : allEvents) {
            if (e.getStartDate() == null) continue;
            if (!passesFilter(e)) continue;
            Calendar ec = Calendar.getInstance();
            ec.setTime(e.getStartDate());
            boolean sameOrLaterYear = ec.get(Calendar.YEAR) > displayCalendar.get(Calendar.YEAR);
            boolean sameYearLaterMonth = ec.get(Calendar.YEAR) == displayCalendar.get(Calendar.YEAR)
                    && ec.get(Calendar.MONTH) >= displayCalendar.get(Calendar.MONTH);
            if (sameOrLaterYear || sameYearLaterMonth) {
                upcoming.add(e);
            }
        }
        Collections.sort(upcoming, (a, b) -> a.getStartDate().compareTo(b.getStartDate()));

        int count = showAllUpcoming ? upcoming.size() : Math.min(upcoming.size(), MAX_UPCOMING);
        SimpleDateFormat fmt = new SimpleDateFormat("d MMMM", Locale.getDefault());

        for (int i = 0; i < count; i++) {
            CalendarEvent ev = upcoming.get(i);
            TextView tv = new TextView(requireContext());
            String date = ev.getStartDate() != null ? fmt.format(ev.getStartDate()) : "";
            tv.setText(date + ": " + ev.getTitle());
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.text));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 2, 0, 2);
            tv.setLayoutParams(lp);
            llUpcomingEvents.addView(tv);
        }

        if (upcoming.size() > MAX_UPCOMING) {
            tvReadMore.setVisibility(View.VISIBLE);
            tvReadMore.setText(showAllUpcoming
                    ? getString(R.string.view_less)
                    : getString(R.string.view_more));
        } else {
            tvReadMore.setVisibility(View.GONE);
        }
    }

    private void showEventPopupAt(View anchor, List<CalendarEvent> events) {
        if (events == null || events.isEmpty()) return;

        // Build title from all events on this day
        StringBuilder sb = new StringBuilder();
        for (CalendarEvent e : events) {
            if (sb.length() > 0) sb.append("\n");
            if (e.getTitle() != null) sb.append(e.getTitle());
        }
        tvPopupTitle.setText(sb.toString());

        // Description of first event — hidden behind "Read more"
        CalendarEvent first = events.get(0);
        String desc = first.getDescription();
        tvPopupDescription.setVisibility(View.GONE);
        if (!TextUtils.isEmpty(desc)) {
            tvPopupDescription.setText(desc);
            tvPopupReadMore.setVisibility(View.VISIBLE);
            tvPopupReadMore.setText("Read more");
            tvPopupReadMore.setOnClickListener(vv -> {
                boolean expanded = tvPopupDescription.getVisibility() == View.VISIBLE;
                tvPopupDescription.setVisibility(expanded ? View.GONE : View.VISIBLE);
                tvPopupReadMore.setText(expanded ? "Read more" : "Read less");
                eventPopup.update(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            });
        } else {
            tvPopupReadMore.setVisibility(View.GONE);
        }

        // Date info
        String dateStr = first.getStartAt() != null ? first.getStartAt() : "";
        if (!TextUtils.isEmpty(first.getEndAt()) && !first.getEndAt().equals(first.getStartAt())) {
            dateStr += " – " + first.getEndAt();
        }
        tvPopupDate.setText(dateStr);

        // Keep popup on screen horizontally
        int[] anchorLoc = new int[2];
        anchor.getLocationOnScreen(anchorLoc);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int popupWidth = dpToPx(220);
        int xOff = 0;
        if (anchorLoc[0] + popupWidth > screenWidth) {
            xOff = screenWidth - anchorLoc[0] - popupWidth - dpToPx(4);
        }

        dismissPopup();
        eventPopup.showAsDropDown(anchor, xOff, 0);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    // ---- UI helpers ----

    private void updateChildDisplay() {
        if (hideChildSelector || selectedChildName == null) return;
        tvSelectedChild.setText(childIds.size() > 1
                ? selectedChildName + " ▾"
                : selectedChildName);
    }

    private void showChildPicker() {
        if (childNames.size() <= 1) return;
        String[] items = childNames.toArray(new String[0]);
        int checked = Math.max(childIds.indexOf(selectedChildId), 0);
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Student")
                .setSingleChoiceItems(items, checked, (d, which) -> {
                    selectedChildId = childIds.get(which);
                    selectedChildName = childNames.get(which);
                })
                .setPositiveButton("Show Events", (d, which) -> {
                    saveSelectedChild();
                    updateChildDisplay();
                    loadEventsForSelectedChild();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void saveSelectedChild() {
        requireContext().getSharedPreferences(PREFS, 0).edit()
                .putString(KEY_SELECTED_CHILD_ID, selectedChildId)
                .putString(KEY_SELECTED_CHILD_NAME, selectedChildName)
                .apply();
    }

    private void showFilterDialog() {
        boolean[] checked = {filterBlue, filterRed, filterGreen};
        String[] items = {"Homework / Assignments", "Tests / Quizzes", "Other Events"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Filter Events")
                .setMultiChoiceItems(items, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(getString(R.string.apply), (d, which) -> {
                    filterBlue = checked[0];
                    filterRed = checked[1];
                    filterGreen = checked[2];
                    refreshCalendar();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissPopup();
    }

    // ---- Inner types ----

    static class CalendarDay {
        final int day;
        final List<CalendarEvent> events;

        CalendarDay(int day, List<CalendarEvent> events) {
            this.day = day;
            this.events = events;
        }
    }

    class CalendarGridAdapter extends RecyclerView.Adapter<CalendarGridAdapter.DayVH> {

        private List<CalendarDay> days = new ArrayList<>();
        private int year, month;

        void setData(List<CalendarDay> days, int year, int month) {
            this.days = days;
            this.year = year;
            this.month = month;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public DayVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View cell = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_day, parent, false);
            return new DayVH(cell);
        }

        @Override
        public void onBindViewHolder(@NonNull DayVH h, int position) {
            CalendarDay cd = days.get(position);

            if (cd.day == 0) {
                h.tvDay.setText("");
                h.circle.setVisibility(View.GONE);
                h.itemView.setClickable(false);
                return;
            }

            h.tvDay.setText(String.valueOf(cd.day));
            h.itemView.setClickable(true);

            Calendar today = Calendar.getInstance();
            boolean isToday = today.get(Calendar.YEAR) == year
                    && today.get(Calendar.MONTH) == month
                    && today.get(Calendar.DAY_OF_MONTH) == cd.day;

            if (cd.events != null && !cd.events.isEmpty()) {
                int colorType = dominantColorType(cd.events);
                GradientDrawable oval = new GradientDrawable();
                oval.setShape(GradientDrawable.OVAL);
                oval.setColor(solidColorOf(colorType));
                h.circle.setBackground(oval);
                h.circle.setVisibility(View.VISIBLE);
                h.tvDay.setTextColor(Color.WHITE);

                final List<CalendarEvent> dayEvents = cd.events;
                h.itemView.setOnClickListener(view -> showEventPopupAt(view, dayEvents));
            } else if (isToday) {
                GradientDrawable oval = new GradientDrawable();
                oval.setShape(GradientDrawable.OVAL);
                oval.setColor(ContextCompat.getColor(requireContext(), R.color.accent));
                h.circle.setBackground(oval);
                h.circle.setVisibility(View.VISIBLE);
                h.tvDay.setTextColor(Color.WHITE);
                h.itemView.setOnClickListener(null);
            } else {
                h.circle.setVisibility(View.GONE);
                h.tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.text));
                h.itemView.setOnClickListener(null);
            }
        }

        @Override
        public int getItemCount() {
            return days.size();
        }

        class DayVH extends RecyclerView.ViewHolder {
            final TextView tvDay;
            final View circle;

            DayVH(@NonNull View v) {
                super(v);
                tvDay = v.findViewById(R.id.tvDayNumber);
                circle = v.findViewById(R.id.viewEventCircle);
            }
        }
    }
}
