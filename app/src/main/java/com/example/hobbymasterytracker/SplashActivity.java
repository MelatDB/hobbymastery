package com.example.hobbymasterytracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

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

        // Disable Android 12+ system splash screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            getSplashScreen().setOnExitAnimationListener(
                    splashScreenView -> splashScreenView.remove());
        }

        setContentView(R.layout.activity_splash);

        ImageView icon = findViewById(R.id.splashIcon);
        TextView title = findViewById(R.id.splashTitle);
        TextView version = findViewById(R.id.splashVersion);

        // Fade + scale in the icon
        icon.animate()
                .alpha(1f)
                .scaleX(1.1f).scaleY(1.1f)
                .setDuration(600)
                .withEndAction(() ->
                    icon.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(200)
                        .start())
                .start();

        // Fade in title after icon
        title.animate()
                .alpha(1f)
                .translationYBy(-20f)
                .setStartDelay(400)
                .setDuration(500)
                .start();

        // Fade in version
        version.animate()
                .alpha(1f)
                .setStartDelay(600)
                .setDuration(400)
                .start();

        // Navigate after 2 seconds
        icon.postDelayed(() -> {
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 2000);
    }
}
