package com.example.edubridge.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import com.example.edubridge.shared.ProfileFragment;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.edubridge.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class AdminUserManagementFragment extends Fragment {

    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private List<UserData> fullUserList;
    private FirebaseFirestore db;
    private String currentSelectedRole = "all";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_user_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        fullUserList = new ArrayList<>();

        recyclerView = view.findViewById(R.id.users_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        Spinner roleSpinner = view.findViewById(R.id.role_spinner);
        String[] roles = {"all", "parent", "student", "admin", "teacher"};

        roleSpinner.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                roles
        ));

        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSelectedRole = roles[position];
                applyFilter();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        view.findViewById(R.id.add_user_button).setOnClickListener(v -> {
            ProfileFragment fragment = new ProfileFragment();
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        loadUsers();
    }

    private void loadUsers() {
        db.collection("users").get()
                .addOnSuccessListener(querySnapshot -> {

                    fullUserList.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        UserData user = doc.toObject(UserData.class);
                        if (user != null) {
                            user.uid = doc.getId();
                            fullUserList.add(user);
                        }
                    }

                    applyFilter();
                });
    }

    private void applyFilter() {

        if (currentSelectedRole.equals("all")) {
            adapter.updateList(new ArrayList<>(fullUserList));
            return;
        }

        List<UserData> filtered = new ArrayList<>();

        for (UserData user : fullUserList) {
            if (user.usertype != null &&
                    user.usertype.equals(currentSelectedRole)) {
                filtered.add(user);
            }
        }

        adapter.updateList(filtered);
    }

    static class UserData {
        public String uid;
        public String fullname;
        public String email;
        public String phone;
        public String usertype;
        public String dob;
        public String notes;
        public UserData() {}
    }

    class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

        private List<UserData> userList;

        public UserAdapter(List<UserData> userList) {
            this.userList = userList;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.shared_user_list_item, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {

            UserData user = userList.get(position);
            holder.nameText.setText(user.fullname);

            holder.itemView.setOnClickListener(v -> {

                Bundle bundle = new Bundle();
                bundle.putString("uid", user.uid);

                ProfileFragment fragment = new ProfileFragment();
                fragment.setArguments(bundle);

                getParentFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            });
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        public void updateList(List<UserData> newList) {
            userList = newList;
            notifyDataSetChanged();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView nameText;
            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.user_full_name);
            }
        }
    }
}

