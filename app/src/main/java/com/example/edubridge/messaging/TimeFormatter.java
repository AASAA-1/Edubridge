package com.example.edubridge.messaging;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeFormatter {

    public static String formatTimestamp(long timestamp) {
        if (timestamp == 0) return "";

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        // Less than 1 minute
        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Just now";
        }

        // Less than 1 hour
        if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + "m ago";
        }

        Calendar msgCal = Calendar.getInstance();
        msgCal.setTimeInMillis(timestamp);

        Calendar todayCal = Calendar.getInstance();

        // Same day → show time
        if (msgCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
                && msgCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)) {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }

        // Yesterday
        Calendar yesterdayCal = Calendar.getInstance();
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);
        if (msgCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR)
                && msgCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)) {
            return "Yesterday";
        }

        // Older → show date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
