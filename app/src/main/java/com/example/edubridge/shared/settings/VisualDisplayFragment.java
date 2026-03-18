package com.example.edubridge.shared.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

public class VisualDisplayFragment extends Fragment {

    private static final String PREFS_NAME = "edubridge_settings";

    public VisualDisplayFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_visual_display, container, false);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Back button
        v.findViewById(R.id.btn_back).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Text Size seekbar
        SeekBar seekbarTextSize = v.findViewById(R.id.seekbar_text_size);
        seekbarTextSize.setProgress(prefs.getInt("text_size", 2));
        seekbarTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    prefs.edit().putInt("text_size", progress).apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // High Contrast toggle
        MaterialSwitch switchHighContrast = v.findViewById(R.id.switch_high_contrast);
        switchHighContrast.setChecked(prefs.getBoolean("high_contrast", false));
        switchHighContrast.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("high_contrast", isChecked).apply());

        // Screen Reader toggle
        MaterialSwitch switchScreenReader = v.findViewById(R.id.switch_screen_reader);
        switchScreenReader.setChecked(prefs.getBoolean("screen_reader", false));
        switchScreenReader.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("screen_reader", isChecked).apply());

        // Theme toggle
        MaterialButtonToggleGroup toggleTheme = v.findViewById(R.id.toggle_theme);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        toggleTheme.check(isDark ? R.id.btn_theme_dark : R.id.btn_theme_light);
        toggleTheme.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                boolean dark = (checkedId == R.id.btn_theme_dark);
                prefs.edit().putBoolean("dark_mode", dark).apply();
            }
        });

        return v;
    }
}
