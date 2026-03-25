package com.example.edubridge.shared;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.example.edubridge.admin.AdminDashboardFragment;
import com.example.edubridge.parent.ParentDashboardFragment;
import com.example.edubridge.shared.notifications.NotificationsFragment;
import com.example.edubridge.teacher.TeacherDashboardFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "edubridge_settings";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Fragment homeFragment;

    @Override
    protected void attachBaseContext(Context newBase) {

        SharedPreferences prefs = newBase.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lang = prefs.getString("language", "en");

        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = newBase.getResources().getConfiguration();
        config.setLocale(locale);

        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }

    private void applyThemeFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String theme = prefs.getString("theme", "regular");

        switch (theme) {
            case "dark":
                setTheme(R.style.Theme_Edubridge_Dark);
                break;
            case "contrast":
                setTheme(R.style.Theme_Edubridge_Contrast);
                break;
            case "colorblind":
                setTheme(R.style.Theme_Edubridge_Colorblind);
                break;
            default:
                setTheme(R.style.Theme_Edubridge);
                break;
        }
    }

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

        // applying theme before ui loads
        applyThemeFromPrefs();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserHomeFragment();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_view);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.home_button) {
                selectedFragment = homeFragment;

            } else if (itemId == R.id.settings_button) {
                selectedFragment = new SettingsFragment();

            } else if (itemId == R.id.notifications_button) {
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
        });
    }
}