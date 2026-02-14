package com.example.edubridge.parent;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.edubridge.R;
import com.google.android.material.appbar.MaterialToolbar;

public class AnnouncementDetailsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_announcement_details, container, false);

        MaterialToolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        Bundle b = getArguments();
        String title = b != null ? b.getString("title", "") : "";
        String body = b != null ? b.getString("body", "") : "";
        String date = b != null ? b.getString("date", "") : "";
        String by = b != null ? b.getString("by", "") : "";

        ((TextView) v.findViewById(R.id.tvTitle)).setText(title);
        ((TextView) v.findViewById(R.id.tvBody)).setText(body);
        ((TextView) v.findViewById(R.id.tvMeta)).setText((date + " • " + by).trim());

        return v;
    }
}
