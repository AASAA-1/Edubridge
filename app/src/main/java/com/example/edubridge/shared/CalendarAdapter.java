package com.example.edubridge.shared;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_EVENT = 1;

    private final List<Object> displayItems = new ArrayList<>();
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());

    // Color mapping for event types
    private final Map<String, Integer> typeColors = new HashMap<>();

    public CalendarAdapter() {
        typeColors.put("Field Trip", Color.parseColor("#4CAF50"));
        typeColors.put("School Event", Color.parseColor("#2196F3"));
        typeColors.put("Important", Color.parseColor("#F44336"));
        typeColors.put("Other", Color.parseColor("#FF9800"));
    }

    public void setEvents(List<CalendarEvent> events) {
        displayItems.clear();

        // Sort events by start date
        Collections.sort(events, (e1, e2) -> {
            if (e1.getStartDate() == null && e2.getStartDate() == null) return 0;
            if (e1.getStartDate() == null) return 1;
            if (e2.getStartDate() == null) return -1;
            return e1.getStartDate().compareTo(e2.getStartDate());
        });

        String currentMonth = "";
        for (CalendarEvent event : events) {
            if (event.getStartDate() != null) {
                String month = monthFormat.format(event.getStartDate());
                if (!month.equals(currentMonth)) {
                    currentMonth = month;
                    displayItems.add(currentMonth);
                }
            }
            displayItems.add(event);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position) instanceof String ? TYPE_HEADER : TYPE_EVENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_header, parent, false);
            TextSizeHelper.applyScaleRecursively(v); // scale header
            return new HeaderViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_event, parent, false);
            TextSizeHelper.applyScaleRecursively(v); // scale event item
            return new EventViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            String month = (String) displayItems.get(position);
            ((HeaderViewHolder) holder).tvMonth.setText(month);
        } else if (holder instanceof EventViewHolder) {
            CalendarEvent event = (CalendarEvent) displayItems.get(position);
            EventViewHolder h = (EventViewHolder) holder;

            h.tvTitle.setText(event.getTitle());
            h.tvType.setText(event.getTypeDisplayName());

            // Set date
            if (event.getStartDate() != null) {
                String dateStr = dateFormat.format(event.getStartDate());
                if (event.getEndDate() != null && !event.getEndDate().equals(event.getStartDate())) {
                    dateStr += " - " + dateFormat.format(event.getEndDate());
                }
                h.tvDate.setText(dateStr);

                // Add time if same day
                if (event.getStartAt() != null && !event.getStartAt().isEmpty()) {
                    h.tvTime.setText(event.getStartAt());
                    h.tvTime.setVisibility(View.VISIBLE);
                } else {
                    h.tvTime.setVisibility(View.GONE);
                }
            }

            // Set class name if available
            if (event.getClassName() != null && !event.getClassName().isEmpty()) {
                h.tvClass.setVisibility(View.VISIBLE);
                h.tvClass.setText("Group: " + event.getClassName());
            } else {
                h.tvClass.setVisibility(View.GONE);
            }

            // Set type color
            Integer color = typeColors.get(event.getType());
            if (color != null) {
                h.typeIndicator.setBackgroundColor(color);
            } else {
                h.typeIndicator.setBackgroundColor(Color.GRAY);
            }

            // Set description
            if (event.getDescription() != null && !event.getDescription().isEmpty()) {
                h.tvDescription.setText(event.getDescription());
                h.tvDescription.setVisibility(View.VISIBLE);
            } else {
                h.tvDescription.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvMonth;
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMonth = itemView.findViewById(R.id.tvMonth);
        }
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        View typeIndicator;
        TextView tvTitle, tvType, tvDate, tvTime, tvClass, tvDescription;
        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            typeIndicator = itemView.findViewById(R.id.typeIndicator);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvType = itemView.findViewById(R.id.tvType);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvClass = itemView.findViewById(R.id.tvClass);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}