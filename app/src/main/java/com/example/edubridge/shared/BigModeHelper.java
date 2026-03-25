package com.example.edubridge.shared;

import android.content.Context;
import android.content.SharedPreferences;

public class BigModeHelper {

    private static final String PREFS_NAME = "edubridge_settings";

    public static float getScale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        boolean bigMode = prefs.getBoolean("big_buttons", false);

        return bigMode ? 1.4f : 1.0f;
    }

    public static boolean isBigModeEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean("big_buttons", false);
    }
}