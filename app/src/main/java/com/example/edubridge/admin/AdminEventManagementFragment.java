package com.example.edubridge.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edubridge.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AdminEventManagementFragment extends Fragment {

    private RecyclerView recyclerView;
    private Spinner filterSpinner;
    private ImageButton addButton;

    private FirebaseFirestore db;

    private List<Map<String, Object>> events = new ArrayList<>();
    private List<String> eventIds = new ArrayList<>();

    public AdminEventManagementFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_admin_event_management,
                container,
                false
        );

        recyclerView = view.findViewById(R.id.events_recycler_view);
        filterSpinner = view.findViewById(R.id.event_filter_spinner);
        addButton = view.findViewById(R.id.add_event_button);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new EventAdapter());

        db = FirebaseFirestore.getInstance();

        setupFilterSpinner();
        loadEvents(null); // default = all events

        addButton.setOnClickListener(v -> {

            AdminEventModificationFragment fragment =
                    new AdminEventModificationFragment();

            requireActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    /**
     * Setup filter spinner options
     */
    private void setupFilterSpinner() {

        List<String> filterOptions = Arrays.asList(
                "All",
                "Field Trip",
                "School Event",
                "Important",
                "Other"
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_dropdown_item,
                filterOptions
        );

        filterSpinner.setAdapter(adapter);

        filterSpinner.setSelection(0); // default = All

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String selected = filterSpinner.getSelectedItem().toString();

                if (selected.equals("All")) {
                    loadEvents(null);
                } else {
                    loadEvents(selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Load events from Firestore
     */
    private void loadEvents(String typeFilter) {

        Query query;

        if (typeFilter == null) {
            query = db.collection("events");
        } else {
            query = db.collection("events")
                    .whereEqualTo("type", typeFilter);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    events.clear();
                    eventIds.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {

                        events.add(doc.getData());
                        eventIds.add(doc.getId());
                    }

                    recyclerView.getAdapter().notifyDataSetChanged();
                });
    }

    private class EventAdapter
            extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.shared_user_list_item, parent, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

            Map<String, Object> event = events.get(position);

            holder.title.setText((String) event.get("title"));

            holder.itemView.setOnClickListener(v -> {

                AdminEventModificationFragment fragment =
                        new AdminEventModificationFragment();

                Bundle bundle = new Bundle();
                bundle.putString("eventId", eventIds.get(position));

                fragment.setArguments(bundle);

                requireActivity()
                        .getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            });
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView title;

            ViewHolder(View itemView) {
                super(itemView);

                title = itemView.findViewById(R.id.user_full_name);
            }
        }
    }
}