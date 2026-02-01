package com.example.edubridge;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
public class MainActivity extends AppCompatActivity {
    FirebaseAuth auth;
    FirebaseFirestore db;
    private void loadUserHomeFragment() {
        String uid = FirebaseAuth.getInstance()
                .getCurrentUser()
                .getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {

                    if (!document.exists()) {
                        Toast.makeText(
                                MainActivity.this,
                                "User data not found",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    String userType = document.getString("usertype");
                    Fragment fragment;

                    switch (userType) {
                        case "admin":
                            fragment = new AdminDashboardFragment();
                            break;

                        case "teacher":
                            fragment = new TeacherDashboardFragment();
                            break;

                        default:
                            fragment = new ParentDashboardFragment();
                            break;
                    }

                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .commit();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            MainActivity.this,
                            "Failed to load user data: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadUserHomeFragment();
        };
    }
