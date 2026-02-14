package com.example.edubridge.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.fragment.app.Fragment;

import com.example.edubridge.R;
import com.google.android.material.materialswitch.MaterialSwitch;

public class InteractionAssistanceFragment extends Fragment {

    private static final String PREFS_NAME = "edubridge_settings";

    public InteractionAssistanceFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_interaction_assistance, container, false);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Back button
        v.findViewById(R.id.btn_back).setOnClickListener(view ->
                requireActivity().getSupportFragmentManager().popBackStack());

        // Auto-Captions
        MaterialSwitch switchCaptions = v.findViewById(R.id.switch_auto_captions);
        switchCaptions.setChecked(prefs.getBoolean("auto_captions", false));
        switchCaptions.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("auto_captions", isChecked).apply());

        // Audio Descriptions
        MaterialSwitch switchAudio = v.findViewById(R.id.switch_audio_descriptions);
        switchAudio.setChecked(prefs.getBoolean("audio_descriptions", false));
        switchAudio.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("audio_descriptions", isChecked).apply());

        // Large Buttons
        MaterialSwitch switchLargeButtons = v.findViewById(R.id.switch_large_buttons);
        switchLargeButtons.setChecked(prefs.getBoolean("large_buttons", false));
        switchLargeButtons.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("large_buttons", isChecked).apply());

        // Touch Sensitivity
        SeekBar seekbarTouch = v.findViewById(R.id.seekbar_touch_sensitivity);
        seekbarTouch.setProgress(prefs.getInt("touch_sensitivity", 2));
        seekbarTouch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    prefs.edit().putInt("touch_sensitivity", progress).apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Gesture Reduction
        MaterialSwitch switchGesture = v.findViewById(R.id.switch_gesture_reduction);
        switchGesture.setChecked(prefs.getBoolean("gesture_reduction", false));
        switchGesture.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("gesture_reduction", isChecked).apply());

        // Enhanced Tooltips
        MaterialSwitch switchTooltips = v.findViewById(R.id.switch_enhanced_tooltips);
        switchTooltips.setChecked(prefs.getBoolean("enhanced_tooltips", false));
        switchTooltips.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean("enhanced_tooltips", isChecked).apply());

        return v;
    }
}
