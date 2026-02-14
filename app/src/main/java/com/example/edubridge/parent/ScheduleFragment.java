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
import java.util.HashMap;

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

        buildDummyTable();

        return v;
    }

    private void buildDummyTable() {
        table.removeAllViews();

        String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday"};
        int periods = 7;

        addHeaderRow(periods);

        HashMap<String, String> m = new HashMap<>();
        m.put(key("Sunday", 1), "Eng");
        m.put(key("Sunday", 2), "Math");
        m.put(key("Sunday", 3), "Science");
        m.put(key("Sunday", 4), "Arabic");
        m.put(key("Sunday", 5), "PE");
        m.put(key("Sunday", 6), "Art");
        m.put(key("Sunday", 7), "ICT");

        for (String d : days) {
            TableRow row = new TableRow(requireContext());
            row.addView(cell(d, true));

            for (int p = 1; p <= periods; p++) {
                String val = m.get(key(d, p));
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
            header.addView(headerCell(p + "st Period".replace("1st", "1st").replace("2st", "2nd").replace("3st", "3rd")));
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

    private String key(String day, int period) {
        return day + "_" + period;
    }
}
