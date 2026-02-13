package com.example.edubridge.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.example.edubridge.LoginActivity;
import com.example.edubridge.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "edubridge_settings";

    public SettingsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Dark Mode toggle
        MaterialSwitch switchDarkMode = v.findViewById(R.id.switch_dark_mode);
        switchDarkMode.setChecked(prefs.getBoolean("dark_mode", false));
        switchDarkMode.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("dark_mode", isChecked).apply());

        // Color-Blind Mode toggle
        MaterialSwitch switchColorBlind = v.findViewById(R.id.switch_color_blind);
        switchColorBlind.setChecked(prefs.getBoolean("color_blind_mode", false));
        switchColorBlind.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("color_blind_mode", isChecked).apply());

        // Language toggle
        MaterialButtonToggleGroup toggleLang = v.findViewById(R.id.toggle_language);
        String currentLang = prefs.getString("language", "en");
        if ("ar".equals(currentLang)) {
            toggleLang.check(R.id.btn_arabic);
        } else {
            toggleLang.check(R.id.btn_english);
        }
        toggleLang.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                String lang = (checkedId == R.id.btn_arabic) ? "ar" : "en";
                prefs.edit().putString("language", lang).apply();
            }
        });

        // Visual & Display nav
        v.findViewById(R.id.card_visual_display).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new VisualDisplayFragment())
                        .addToBackStack(null)
                        .commit());

        // Interaction & Assistance nav
        v.findViewById(R.id.card_interaction_assistance).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new InteractionAssistanceFragment())
                        .addToBackStack(null)
                        .commit());

        // Log Out
        v.findViewById(R.id.btn_logout).setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return v;
    }
}
