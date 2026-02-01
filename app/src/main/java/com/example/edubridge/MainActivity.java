package com.example.edubridge;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.edubridge.admin.AdminDashboardFragment;
import com.example.edubridge.parent.ParentDashboardFragment;
import com.example.edubridge.teacher.TeacherDashboardFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
public class MainActivity extends AppCompatActivity {
    FirebaseAuth auth;
    FirebaseFirestore db;
    private Fragment homeFragment;  //for remembering the user's fragment for the navbar
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
                    switch (userType) {
                        case "admin":
                            homeFragment = new AdminDashboardFragment();
                            break;

                        case "teacher":
                            homeFragment = new TeacherDashboardFragment();
                            break;

                        default:
                            homeFragment = new ParentDashboardFragment();
                            break;
                    }
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, homeFragment)
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
        /* TODO: Test when new fragments get added
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_view);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            if (item.getItemId() == R.id.home_button) {
                selectedFragment = homeFragment;
            }
            /* TODO: Test when settings/notification fragments get added
            else if (item.getItemId() == R.id.settings_button) {
                selectedFragment = new SettingsFragment();
            }
            else if (item.getItemId() == R.id.notifications_button) {
                selectedFragment = new NotificationsFragment();
            }
            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });*/
        }
    }
