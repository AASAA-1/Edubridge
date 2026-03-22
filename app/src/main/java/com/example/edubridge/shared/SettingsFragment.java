package com.example.edubridge.shared;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
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

        // theme selection
        RadioGroup themeGroup = v.findViewById(R.id.radio_theme_group);
        String currentTheme = prefs.getString("theme", "regular");

        if ("dark".equals(currentTheme)) {
            themeGroup.check(R.id.theme_dark);
        } else if ("contrast".equals(currentTheme)) {
            themeGroup.check(R.id.theme_high_contrast);
        } else if ("colorblind".equals(currentTheme)) {
            themeGroup.check(R.id.theme_colorblind);
        } else {
            themeGroup.check(R.id.theme_regular);
        }

        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String theme = "regular";

            if (checkedId == R.id.theme_dark) {
                theme = "dark";
            } else if (checkedId == R.id.theme_high_contrast) {
                theme = "contrast";
            } else if (checkedId == R.id.theme_colorblind) {
                theme = "colorblind";
            }

            prefs.edit().putString("theme", theme).apply();

            // recreate our main activity to apply the oncreate for the settings
            v.findViewById(R.id.btn_save_settings).setOnClickListener(view -> {
                requireActivity().recreate();
            });
        });

        // language toggle
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

                requireActivity().recreate();
            }
        });

        // text size
        SeekBar textSizeSeek = v.findViewById(R.id.seek_text_size);
        int savedSize = prefs.getInt("text_size", 2);
        textSizeSeek.setProgress(savedSize);

        textSizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prefs.edit().putInt("text_size", progress).apply();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                requireActivity().recreate();
            }
        });

        // big buttons mode
        MaterialSwitch switchBigButtons = v.findViewById(R.id.switch_big_buttons);
        switchBigButtons.setChecked(prefs.getBoolean("big_buttons", false));

        switchBigButtons.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("big_buttons", isChecked).apply()
        );

        // text to speech toggle
        MaterialSwitch switchTts = v.findViewById(R.id.switch_tts);
        switchTts.setChecked(prefs.getBoolean("tts_enabled", false));

        switchTts.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("tts_enabled", isChecked).apply()
        );

        // logout
        v.findViewById(R.id.btn_logout).setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return v;
    }
}