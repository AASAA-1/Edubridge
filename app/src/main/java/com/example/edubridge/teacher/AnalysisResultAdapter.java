package com.example.edubridge.teacher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AnalysisResultAdapter extends RecyclerView.Adapter<AnalysisResultAdapter.ViewHolder> {

    private final List<AnalysisResultItem> items;

    public AnalysisResultAdapter(List<AnalysisResultItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_analysis_result, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        AnalysisResultItem item = items.get(position);

        h.divider.setVisibility(position == 0 ? View.GONE : View.VISIBLE);

        if (item.getAnalyzedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy  •  h:mm a", Locale.ENGLISH);
            h.tvDate.setText(sdf.format(item.getAnalyzedAt().toDate()));
        }

        h.tvText.setText(item.getAnalysisText());
        applyExpandState(h, item.isExpanded());

        h.header.setOnClickListener(v -> {
            item.setExpanded(!item.isExpanded());
            applyExpandState(h, item.isExpanded());
        });
    }

    private void applyExpandState(ViewHolder h, boolean expanded) {
        h.tvText.setVisibility(expanded ? View.VISIBLE : View.GONE);
        h.chevron.setRotation(expanded ? 180f : 0f);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View divider;
        final View header;
        final TextView tvDate;
        final ImageView chevron;
        final TextView tvText;

        ViewHolder(View v) {
            super(v);
            divider = v.findViewById(R.id.divider_analysis);
            header  = v.findViewById(R.id.header_analysis);
            tvDate  = v.findViewById(R.id.tv_analysis_date);
            chevron = v.findViewById(R.id.iv_chevron);
            tvText  = v.findViewById(R.id.tv_analysis_text);
        }
    }
}
