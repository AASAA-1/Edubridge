package com.example.edubridge.parent;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import com.example.edubridge.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ScheduleFragment extends Fragment {

    private TableLayout table;
    private TextView emptyText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_schedule, container, false);

        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        table = v.findViewById(R.id.table);
        emptyText = v.findViewById(R.id.emptyText);

        loadSchedule();

        return v;
    }

    private void loadSchedule() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("schedule")
                .document("default")
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        emptyText.setVisibility(View.VISIBLE);
                        return;
                    }

                    Long periodsLong = doc.getLong("periods");
                    int periods = periodsLong == null ? 7 : periodsLong.intValue();

                    Map<String, String> cells =
                            (Map<String, String>) doc.get("cells");

                    if (cells == null) cells = new HashMap<>();

                    buildTable(periods, cells);
                })
                .addOnFailureListener(e ->
                        emptyText.setVisibility(View.VISIBLE)
                );
    }

    private void buildTable(int periods, Map<String, String> cells) {
        table.removeAllViews();

        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday"};

        addHeaderRow(periods);

        for (String d : days) {
            TableRow row = new TableRow(requireContext());
            row.addView(cell(d, true));

            for (int p = 1; p <= periods; p++) {
                String val = cells.get(d + "_" + p);
                if (val == null) val = "";
                row.addView(cell(val, false));
            }

            table.addView(row);
        }

        emptyText.setVisibility(View.GONE);
    }

    private void addHeaderRow(int periods) {
        TableRow header = new TableRow(requireContext());
        header.addView(headerCell("Day"));

        for (int p = 1; p <= periods; p++) {
            header.addView(headerCell(p + " Period"));
        }

        table.addView(header);
    }

    private TextView headerCell(String t) {
        TextView tv = new TextView(requireContext());
        tv.setText(t);
        tv.setPadding(16, 14, 16, 14);
        tv.setTextSize(12f);
        tv.setTextColor(0xFF111111);
        tv.setBackgroundColor(0xFF9FD0E6);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        return tv;
    }

    private TextView cell(String t, boolean day) {
        TextView tv = new TextView(requireContext());
        tv.setText(t);
        tv.setPadding(16, 14, 16, 14);
        tv.setTextSize(12f);
        tv.setTextColor(0xFF111111);
        tv.setBackgroundColor(day ? 0xFFE6F3FA : 0xFFFFFFFF);
        tv.setMinEms(6);
        return tv;
    }
}
