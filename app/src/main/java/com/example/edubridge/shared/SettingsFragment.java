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

    // temp values (only saved when user presses save)
    private String selectedTheme;
    private String selectedLanguage;
    private int selectedTextSize;
    private boolean selectedBigButtons;
    private boolean selectedTts;

    public SettingsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // load saved values
        selectedTheme = prefs.getString("theme", "regular");
        selectedLanguage = prefs.getString("language", "en");
        selectedTextSize = prefs.getInt("text_size", 2);
        selectedBigButtons = prefs.getBoolean("big_buttons", false);
        selectedTts = prefs.getBoolean("tts_enabled", false);

        // theme selection
        RadioGroup themeGroup = v.findViewById(R.id.radio_theme_group);

        if ("dark".equals(selectedTheme)) {
            themeGroup.check(R.id.theme_dark);
        } else if ("contrast".equals(selectedTheme)) {
            themeGroup.check(R.id.theme_high_contrast);
        } else if ("colorblind".equals(selectedTheme)) {
            themeGroup.check(R.id.theme_colorblind);
        } else {
            themeGroup.check(R.id.theme_regular);
        }

        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.theme_dark) {
                selectedTheme = "dark";
            } else if (checkedId == R.id.theme_high_contrast) {
                selectedTheme = "contrast";
            } else if (checkedId == R.id.theme_colorblind) {
                selectedTheme = "colorblind";
            } else {
                selectedTheme = "regular";
            }
        });

        // language toggle
        MaterialButtonToggleGroup toggleLang = v.findViewById(R.id.toggle_language);

        if ("ar".equals(selectedLanguage)) {
            toggleLang.check(R.id.btn_arabic);
        } else {
            toggleLang.check(R.id.btn_english);
        }

        toggleLang.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                selectedLanguage = (checkedId == R.id.btn_arabic) ? "ar" : "en";
            }
        });

        // text size
        SeekBar textSizeSeek = v.findViewById(R.id.seek_text_size);
        textSizeSeek.setProgress(selectedTextSize);

        textSizeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedTextSize = progress;
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // big buttons mode
        MaterialSwitch switchBigButtons = v.findViewById(R.id.switch_big_buttons);
        switchBigButtons.setChecked(selectedBigButtons);

        switchBigButtons.setOnCheckedChangeListener((btn, isChecked) ->
                selectedBigButtons = isChecked
        );

        // text to speech toggle
        MaterialSwitch switchTts = v.findViewById(R.id.switch_tts);
        switchTts.setChecked(selectedTts);

        switchTts.setOnCheckedChangeListener((btn, isChecked) ->
                selectedTts = isChecked
        );

        // save button
        v.findViewById(R.id.btn_save_settings).setOnClickListener(view -> {

            prefs.edit()
                    .putString("theme", selectedTheme)
                    .putString("language", selectedLanguage)
                    .putInt("text_size", selectedTextSize)
                    .putBoolean("big_buttons", selectedBigButtons)
                    .putBoolean("tts_enabled", selectedTts)
                    .apply();

            requireActivity().recreate();
        });

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