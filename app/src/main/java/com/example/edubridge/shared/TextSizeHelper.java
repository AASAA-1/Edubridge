package com.example.edubridge.shared;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.edubridge.R;

public class TextSizeHelper {

    private static final String PREFS_NAME = "edubridge_settings";

    // Text size multipliers
    private static final float[] TEXT_SIZE_MULTIPLIERS = {
            0.8f,   // Small
            0.9f,   // Slightly small
            1.0f,   // Normal
            1.15f,  // Large
            1.3f    // Extra large
    };

    /**
     * Get current scale
     */
    public static float getScale(Context context) {

        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        int level = prefs.getInt("text_size", 2);

        level = Math.max(0,
                Math.min(level, TEXT_SIZE_MULTIPLIERS.length - 1));

        float scale = TEXT_SIZE_MULTIPLIERS[level];

        // Big mode additional scaling
        if (BigModeHelper.isBigModeEnabled(context)) {
            scale *= 1.15f;
        }

        return scale;
    }

    /**
     * Apply scaling to single TextView
     */
    public static void applyTextSize(TextView textView) {

        Context context = textView.getContext();

        // Store original size only once
        Object tag = textView.getTag(R.id.original_text_size);

        float originalSizeSp;

        if (tag instanceof Float) {

            originalSizeSp = (Float) tag;

        } else {

            originalSizeSp =
                    textView.getTextSize() /
                            context.getResources()
                                    .getDisplayMetrics()
                                    .scaledDensity;

            textView.setTag(R.id.original_text_size, originalSizeSp);
        }

        float scaledSize = originalSizeSp * getScale(context);

        textView.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                scaledSize
        );
    }

    /**
     * Apply scaling recursively to all TextViews
     */
    public static void applyScaleRecursively(View view) {

        if (view instanceof TextView) {
            applyTextSize((TextView) view);
        }

        if (view instanceof ViewGroup) {

            ViewGroup group = (ViewGroup) view;

            for (int i = 0; i < group.getChildCount(); i++) {

                applyScaleRecursively(group.getChildAt(i));
            }
        }
    }
}