package com.example.hobbymasterytracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREFS = "app_prefs";
    private static final String KEY_LANGUAGE = "language_code";

    public static void applyLocale(Context context) {
        String code = getSavedCode(context);
        setLocale(context, code);
    }

    public static void setAndSave(Context context, String code) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_LANGUAGE, code).apply();
        setLocale(context, code);
    }

    public static String getSavedCode(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, "en");
    }

    private static void setLocale(Context context, String code) {
        Locale locale = new Locale(code);
        Locale.setDefault(locale);
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }
}
