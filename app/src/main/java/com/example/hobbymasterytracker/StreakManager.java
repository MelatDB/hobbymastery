package com.example.hobbymasterytracker;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StreakManager {

    private static final String PREFS = "streak_prefs";
    private static final String KEY_STREAK = "streak_count";
    private static final String KEY_LAST_DATE = "last_practice_date";
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final SharedPreferences prefs;

    public StreakManager(Context context) {
        String user = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getString("user_name", "default");
        prefs = context.getSharedPreferences("streak_" + user, Context.MODE_PRIVATE);
    }

    /** Call this every time the user logs a practice hour. */
    public void recordPractice() {
        String today = DATE_FORMAT.format(new Date());
        String lastDate = prefs.getString(KEY_LAST_DATE, null);
        int streak = prefs.getInt(KEY_STREAK, 0);

        if (lastDate == null) {
            // First ever practice
            streak = 1;
        } else if (lastDate.equals(today)) {
            // Already practiced today, no change
            return;
        } else if (isYesterday(lastDate, today)) {
            // Practiced yesterday — keep the streak going
            streak += 1;
        } else {
            // Missed a day — reset
            streak = 1;
        }

        prefs.edit()
                .putInt(KEY_STREAK, streak)
                .putString(KEY_LAST_DATE, today)
                .apply();
    }

    /** Returns the current streak count. */
    public int getStreak() {
        // Check if streak should be reset (missed yesterday)
        String lastDate = prefs.getString(KEY_LAST_DATE, null);
        if (lastDate == null) return 0;

        String today = DATE_FORMAT.format(new Date());
        if (lastDate.equals(today) || isYesterday(lastDate, today)) {
            return prefs.getInt(KEY_STREAK, 0);
        }
        // Missed a day — streak is broken
        return 0;
    }

    /** Resets the streak to 0 */
    public void reset() {
        prefs.edit().remove(KEY_STREAK).remove(KEY_LAST_DATE).apply();
    }

    private boolean isYesterday(String lastDate, String today) {
        try {
            Date last = DATE_FORMAT.parse(lastDate);
            Date todayDate = DATE_FORMAT.parse(today);
            if (last == null || todayDate == null) return false;
            long diff = todayDate.getTime() - last.getTime();
            return diff == 86400000L;
        } catch (Exception e) {
            return false;
        }
    }
}
