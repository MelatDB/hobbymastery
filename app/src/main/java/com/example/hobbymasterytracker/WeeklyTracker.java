package com.example.hobbymasterytracker;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class WeeklyTracker {

    private static final String PREFS = "weekly_prefs";
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final SharedPreferences prefs;

    public WeeklyTracker(Context context) {
        String user = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getString("user_name", "default");
        prefs = context.getSharedPreferences("weekly_" + user, Context.MODE_PRIVATE);
    }

    /** Call when user logs hours */
    public void recordHour() {
        recordHours(1f);
    }

    public void recordHours(float hours) {
        String today = DATE_FORMAT.format(new Date());
        float current = prefs.getFloat(today, 0f);
        prefs.edit().putFloat(today, current + hours).apply();
    }

    /** Returns today's logged hours */
    public float getTodayHours() {
        String today = DATE_FORMAT.format(new Date());
        return prefs.getFloat(today, 0f);
    }

    /**
     * Returns hours for Mon–Sun of the current week.
     * Index 0 = Monday, 6 = Sunday.
     */
    public float[] getWeeklyHours() {
        float[] hours = new float[7];

        // Find Monday of this week
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        // Roll back to Monday
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        // DAY_OF_WEEK: Sun=1, Mon=2 ... Sat=7
        int daysFromMonday = (dow == Calendar.SUNDAY) ? 6 : dow - Calendar.MONDAY;
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < 7; i++) {
            String date = DATE_FORMAT.format(cal.getTime());
            hours[i] = prefs.getFloat(date, 0f);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return hours;
    }
}
