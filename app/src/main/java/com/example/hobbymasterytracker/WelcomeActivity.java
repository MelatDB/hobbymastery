package com.example.hobbymasterytracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context base) {
        String code = base.getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString("language_code", "en");
        android.content.res.Configuration config = new android.content.res.Configuration(
                base.getResources().getConfiguration());
        java.util.Locale locale = new java.util.Locale(code);
        java.util.Locale.setDefault(locale);
        config.setLocale(locale);
        super.attachBaseContext(base.createConfigurationContext(config));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Skip welcome if already logged in
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("is_logged_in", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        EditText etName = findViewById(R.id.etWelcomeName);
        Button btnStart = findViewById(R.id.btnGetStarted);

        btnStart.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, getString(R.string.name_hint), Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences appPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            String lastUser = appPrefs.getString("last_user_name", "");
            boolean isSameUser = name.equalsIgnoreCase(lastUser);

            if (!isSameUser && !lastUser.isEmpty()) {
                // Different user — confirm data will be cleared
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.new_user_title))
                    .setMessage(getString(R.string.new_user_msg))
                    .setPositiveButton(getString(R.string.yes), (d, w) -> loginAs(name, true))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
            } else {
                // Same user or first time — restore/keep data
                loginAs(name, false);
            }
        });
    }

    private void loginAs(String name, boolean clearData) {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit()
                .putString("user_name", name)
                .putBoolean("is_logged_in", true)
                .remove("last_user_name")
                .apply();

        // Restore this user's saved settings (avatar, dark mode, language etc.)
        SharedPreferences userPrefs = getSharedPreferences("user_prefs_" + name, MODE_PRIVATE);
        if (userPrefs.contains("user_name")) {
            prefs.edit()
                .putString("avatar_uri", userPrefs.getString("avatar_uri", null))
                .putBoolean("dark_mode", userPrefs.getBoolean("dark_mode", false))
                .putBoolean("sound_effects", userPrefs.getBoolean("sound_effects", true))
                .putBoolean("log_mode_enter", userPrefs.getBoolean("log_mode_enter", false))
                .putInt("daily_goal", userPrefs.getInt("daily_goal", 1))
                .putString("language_code", userPrefs.getString("language_code", "en"))
                .apply();
        }

        AppDatabase.resetInstance();
        goToMain();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
