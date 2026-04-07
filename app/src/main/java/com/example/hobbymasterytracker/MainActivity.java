package com.example.hobbymasterytracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context base) {
        // Apply locale before anything is created
        String code = base.getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString("language_code", "en");
        android.content.res.Configuration config = new android.content.res.Configuration(
                base.getResources().getConfiguration());
        java.util.Locale locale = new java.util.Locale(code);
        java.util.Locale.setDefault(locale);
        config.setLocale(locale);
        super.attachBaseContext(base.createConfigurationContext(config));
    }

    private AppDatabase db;
    private HobbyAdapter adapter;

    private LinearLayout headerLayout;
    private RecyclerView recyclerView;
    private FrameLayout fab;
    private View statsView;
    private View rewardsView;
    private View settingsView;

    private TextView txtTotalHoursVal;
    private TextView txtStreakVal;
    private StreakManager streakManager;
    private WeeklyTracker weeklyTracker;
    private SoundManager soundManager;

    private String currentPage = "home";
    private androidx.activity.result.ActivityResultLauncher<String> imagePickerLauncher;
    private androidx.activity.result.ActivityResultLauncher<android.net.Uri> cameraLauncher;
    private android.net.Uri cameraImageUri;
    private boolean launchingCamera = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Image picker for profile avatar
        imagePickerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
            uri -> {
                if (uri == null) return;
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit().putString("avatar_uri", uri.toString()).apply();
                loadAvatar(uri.toString());
            }
        );

        // Camera launcher
        cameraLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.TakePicture(),
            success -> {
                launchingCamera = false;
                // Clear pending URI
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit().remove("pending_camera_uri").apply();
                // Recover URI if activity was recreated
                if (cameraImageUri == null) {
                    String pending = getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .getString("pending_camera_uri", null);
                    if (pending != null) cameraImageUri = android.net.Uri.parse(pending);
                }
                if (success && cameraImageUri != null) {
                    getSharedPreferences("app_prefs", MODE_PRIVATE)
                        .edit().putString("avatar_uri", cameraImageUri.toString()).apply();
                    currentPage = "settings";
                    loadAvatar(cameraImageUri.toString());
                }
            }
        );

        db = AppDatabase.getInstance(this,
                getSharedPreferences("app_prefs", MODE_PRIVATE)
                        .getString("user_name", "default"));

        // Start mascot pulse animation
        android.view.animation.Animation pulse = android.view.animation.AnimationUtils
                .loadAnimation(this, R.anim.mascot_pulse);
        ImageView mascot = findViewById(R.id.mascotIcon);
        if (mascot != null) mascot.startAnimation(pulse);
        headerLayout = findViewById(R.id.headerLayout);
        recyclerView = findViewById(R.id.recyclerView);
        fab = findViewById(R.id.fab);
        statsView = findViewById(R.id.statsView);
        rewardsView = findViewById(R.id.rewardsView);
        settingsView = findViewById(R.id.settingsView);
        txtTotalHoursVal = findViewById(R.id.txtTotalHours);
        txtStreakVal = findViewById(R.id.txtStreakCount);
        streakManager = new StreakManager(this);
        weeklyTracker = new WeeklyTracker(this);
        soundManager = new SoundManager(this);
        NotificationHelper.createChannel(this);
        requestNotificationPermission();



        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HobbyAdapter();
        recyclerView.setAdapter(adapter);

        // Swipe to delete
        new androidx.recyclerview.widget.ItemTouchHelper(
            new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0,
                    androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
                @Override
                public boolean onMove(androidx.recyclerview.widget.RecyclerView rv,
                        androidx.recyclerview.widget.RecyclerView.ViewHolder vh,
                        androidx.recyclerview.widget.RecyclerView.ViewHolder target) { return false; }

                @Override
                public void onSwiped(androidx.recyclerview.widget.RecyclerView.ViewHolder vh, int dir) {
                    Hobby hobby = adapter.getHobbyAt(vh.getAdapterPosition());
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.delete_hobby_title))
                        .setMessage(getString(R.string.delete_hobby_msg))
                        .setPositiveButton(getString(R.string.delete), (d, w) ->
                            Executors.newSingleThreadExecutor().execute(() ->
                                db.hobbyDao().delete(hobby)))
                        .setNegativeButton(getString(R.string.cancel), (d, w) ->
                            adapter.notifyItemChanged(vh.getAdapterPosition()))
                        .show();
                }
            }).attachToRecyclerView(recyclerView);

        db.hobbyDao().getAll().observe(this, hobbies -> {
            adapter.setHobbies(hobbies);
            updateGlobalHeaderStats(hobbies);
            updateStatsPage(hobbies);
            updateRewardsPage(hobbies);
        });

        updateStreakDisplay();

        // Streak card click — show history
        View streakCard = findViewById(R.id.streakCard);
        if (streakCard != null) streakCard.setOnClickListener(v -> showStreakHistory());

        // Restore active page after recreation (e.g. dark mode toggle)
        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getString("current_page", "home");
            launchingCamera = savedInstanceState.getBoolean("launching_camera", false);
            String savedCamUri = savedInstanceState.getString("camera_uri");
            if (savedCamUri != null) cameraImageUri = android.net.Uri.parse(savedCamUri);
        }

        // Recover pending camera URI if activity was killed while camera was open
        String pendingCamUri = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getString("pending_camera_uri", null);
        if (pendingCamUri != null) {
            cameraImageUri = android.net.Uri.parse(pendingCamUri);
            currentPage = "settings";
        }
        showPage(currentPage);
        int navId = R.id.nav_home;
        if (currentPage.equals("stats")) navId = R.id.nav_stats;
        else if (currentPage.equals("rewards")) navId = R.id.nav_rewards;
        else if (currentPage.equals("settings")) navId = R.id.nav_settings;
        findViewById(R.id.bottomNavigation);
        BottomNavigationView bottomNavRestore = findViewById(R.id.bottomNavigation);
        bottomNavRestore.setSelectedItemId(navId);
        if (currentPage.equals("settings")) setupSettingsPage();

        adapter.setOnHobbyClickListener(hobby -> {
            boolean enterMode = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .getBoolean("log_mode_enter", false);
            if (enterMode) {
                // Show input dialog
                android.widget.EditText input = new android.widget.EditText(this);
                input.setHint(getString(R.string.hours_hint));
                input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                input.setBackground(getDrawable(R.drawable.input_bg));
                input.setPadding(40, 20, 40, 20);
                new AlertDialog.Builder(this)
                    .setTitle(hobby.name)
                    .setMessage(getString(R.string.how_many_hours))
                    .setView(input)
                    .setPositiveButton(getString(R.string.add), (dialog, which) -> {
                        String val = input.getText().toString().trim();
                        if (val.isEmpty()) return;
                        try {
                            double hours = Double.parseDouble(val);
                            if (hours <= 0) return;
                            logHours(hobby, hours);
                        } catch (NumberFormatException ignored) {}
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
            } else {
                // Tap mode — add 1 hour directly
                logHours(hobby, 1.0);
            }
        });

        adapter.setOnHobbyLongClickListener(hobby -> {
            soundManager.playWarningSound();
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_hobby_title).replace("hobby", "\"" + hobby.name + "\""))
                .setMessage(getString(R.string.delete_hobby_msg))
                .setPositiveButton(getString(R.string.delete), (dialog, which) ->
                    Executors.newSingleThreadExecutor().execute(() ->
                        db.hobbyDao().delete(hobby)))
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
        });

        fab.setOnClickListener(v -> {
            soundManager.playClickSound();
            showAddHobbyDialog();
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showPage("home");
                return true;
            } else if (id == R.id.nav_stats) {
                showPage("stats");
                return true;
            } else if (id == R.id.nav_rewards) {
                showPage("rewards");
                return true;
            } else if (id == R.id.nav_settings) {
                showPage("settings");
                setupSettingsPage();
                return true;
            }
            return false;
        });
    }

    private void logHours(Hobby hobby, double hours) {
        // Haptic feedback
        android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(40,
                        android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(40);
            }
        }
        int levelBefore = (int) (hobby.totalHours / 10) + 1;
        streakManager.recordPractice();
        weeklyTracker.recordHours((float) hours);
        soundManager.playPracticeSound();
        updateStreakDisplay();
        Executors.newSingleThreadExecutor().execute(() -> {
            hobby.totalHours += hours;
            db.hobbyDao().update(hobby);
            int levelAfter = (int) (hobby.totalHours / 10) + 1;
            if (levelAfter > levelBefore) {
                runOnUiThread(() -> {
                    soundManager.playLevelUpSound();
                    showLevelUpCelebration(levelAfter);
                });
            }
        });
        Toast.makeText(this, "+" + hours + " " + getString(R.string.hrs), Toast.LENGTH_SHORT).show();
    }

    private void launchCamera() {
        try {
            android.content.Intent cameraIntent = new android.content.Intent(
                    android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, 1001);
        } catch (Exception e) {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 1002) {
            if (results.length > 0 && results[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            try {
                android.graphics.Bitmap photo = null;
                if (data != null && data.getExtras() != null) {
                    // Get thumbnail from camera
                    photo = (android.graphics.Bitmap) data.getExtras().get("data");
                }
                if (photo != null) {
                    // Save bitmap to internal storage
                    java.io.File file = new java.io.File(getFilesDir(), "avatar.jpg");
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                    photo.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.close();
                    String uriStr = android.net.Uri.fromFile(file).toString();
                    getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .edit().putString("avatar_uri", uriStr).apply();
                    currentPage = "settings";
                    loadAvatar(uriStr);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Could not save photo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(android.os.Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("current_page", currentPage);
        outState.putBoolean("launching_camera", launchingCamera);
        if (cameraImageUri != null) {
            outState.putString("camera_uri", cameraImageUri.toString());
        }
    }

    private void showPage(String page) {
        currentPage = page;
        headerLayout.setVisibility(page.equals("home") ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(page.equals("home") ? View.VISIBLE : View.GONE);
        fab.setVisibility(page.equals("home") ? View.VISIBLE : View.GONE);
        statsView.setVisibility(page.equals("stats") ? View.VISIBLE : View.GONE);
        rewardsView.setVisibility(page.equals("rewards") ? View.VISIBLE : View.GONE);
        settingsView.setVisibility(page.equals("settings") ? View.VISIBLE : View.GONE);

        TextView topBarTitle = findViewById(R.id.topBarTitle);
        if (topBarTitle != null) {
            switch (page) {
                case "stats":    topBarTitle.setText(getString(R.string.mastery_stats)); break;
                case "rewards":  topBarTitle.setText(getString(R.string.rewards)); break;
                case "settings": topBarTitle.setText(getString(R.string.settings)); break;
                default:         topBarTitle.setText(getString(R.string.title_hobby_mastery)); break;
            }
        }
    }

    private void setupSettingsPage() {
        androidx.appcompat.widget.SwitchCompat sw = settingsView.findViewById(R.id.switchReminder);
        androidx.appcompat.widget.SwitchCompat swDark = settingsView.findViewById(R.id.switchDarkMode);
        androidx.appcompat.widget.SwitchCompat swSound = settingsView.findViewById(R.id.switchSound);
        View rowTime = settingsView.findViewById(R.id.rowReminderTime);
        TextView txtTime = settingsView.findViewById(R.id.txtReminderTime);
        View rowReset = settingsView.findViewById(R.id.rowResetStreak);
        View rowClear = settingsView.findViewById(R.id.rowClearData);
        View rowGoal = settingsView.findViewById(R.id.rowDailyGoal);
        TextView txtGoal = settingsView.findViewById(R.id.txtDailyGoal);
        View rowHelp = settingsView.findViewById(R.id.rowHelp);
        View rowPrivacy = settingsView.findViewById(R.id.rowPrivacy);
        View btnLogOut = settingsView.findViewById(R.id.btnLogOut);

        android.content.SharedPreferences prefs = ReminderScheduler.getPrefs(this);
        android.content.SharedPreferences appPrefs =
                getSharedPreferences("app_prefs", MODE_PRIVATE);

        // Profile avatar tap — open camera or gallery
        View avatarFrame = settingsView.findViewById(R.id.profileAvatarFrame);
        if (avatarFrame != null) {
            // Load saved avatar
            String savedUri = appPrefs.getString("avatar_uri", null);
            if (savedUri != null) loadAvatar(savedUri);

            avatarFrame.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                    .setTitle("Profile Photo")
                    .setItems(new String[]{"📷  Take Photo", "🖼️  Choose from Gallery", "🗑️  Remove Photo"},
                        (d, which) -> {
                            if (which == 0) {
                                // Request camera permission first
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                    checkSelfPermission(android.Manifest.permission.CAMERA)
                                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 1002);
                                } else {
                                    launchCamera();
                                }
                            } else if (which == 1) {
                                // Gallery
                                imagePickerLauncher.launch("image/*");
                            } else {
                                // Remove
                                appPrefs.edit().remove("avatar_uri").apply();
                                ImageView img = settingsView.findViewById(R.id.imgProfileAvatar);
                                TextView emoji = settingsView.findViewById(R.id.txtAvatarEmoji);
                                if (img != null) img.setVisibility(View.GONE);
                                if (emoji != null) emoji.setVisibility(View.VISIBLE);
                            }
                        })
                    .show();
            });
        }

        // Profile name
        TextView txtProfileName = settingsView.findViewById(R.id.txtProfileName);
        String savedName = appPrefs.getString("user_name", null);
        if (savedName != null && !savedName.isEmpty()) {
            txtProfileName.setText(savedName);
        }
        View profileCard = (View) txtProfileName.getParent().getParent();
        profileCard.setOnClickListener(v -> {
            android.widget.EditText input = new android.widget.EditText(this);
            input.setHint(getString(R.string.name_hint));
            input.setText(appPrefs.getString("user_name", ""));
            input.setBackground(getDrawable(R.drawable.input_bg));
            input.setPadding(40, 20, 40, 20);
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.edit_name_title))
                .setView(input)
                .setPositiveButton(getString(R.string.save), (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        appPrefs.edit().putString("user_name", name).apply();
                        txtProfileName.setText(name);
                        saveUserSettings(name);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
        });

        // Notifications
        boolean enabled = prefs.getBoolean(ReminderScheduler.KEY_ENABLED, false);
        int hour = prefs.getInt(ReminderScheduler.KEY_HOUR, 20);
        int minute = prefs.getInt(ReminderScheduler.KEY_MINUTE, 0);
        sw.setChecked(enabled);
        rowTime.setAlpha(enabled ? 1f : 0.4f);
        txtTime.setText(formatTime(hour, minute));
        sw.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked && !ReminderScheduler.canScheduleExact(this)) {
                new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.permission_required))
                    .setMessage(getString(R.string.alarm_permission_msg))
                    .setPositiveButton(getString(R.string.open_settings), (d, w) ->
                        ReminderScheduler.openExactAlarmSettings(this))
                    .setNegativeButton(getString(R.string.cancel), (d, w) -> sw.setChecked(false))
                    .show();
                return;
            }
            int h = prefs.getInt(ReminderScheduler.KEY_HOUR, 20);
            int m = prefs.getInt(ReminderScheduler.KEY_MINUTE, 0);
            ReminderScheduler.saveAndSchedule(this, isChecked, h, m);
            rowTime.setAlpha(isChecked ? 1f : 0.4f);
        });
        rowTime.setOnClickListener(v -> {
            if (!sw.isChecked()) return;
            int h = prefs.getInt(ReminderScheduler.KEY_HOUR, 20);
            int m = prefs.getInt(ReminderScheduler.KEY_MINUTE, 0);
            new android.app.TimePickerDialog(this, (tp, selHour, selMin) -> {
                ReminderScheduler.saveAndSchedule(this, true, selHour, selMin);
                txtTime.setText(formatTime(selHour, selMin));
            }, h, m, false).show();
        });

        // Dark mode
        swDark.setChecked(appPrefs.getBoolean("dark_mode", false));
        swDark.setOnCheckedChangeListener((btn, isChecked) -> {
            appPrefs.edit().putBoolean("dark_mode", isChecked).apply();
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                isChecked
                    ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                    : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Sound effects
        swSound.setChecked(appPrefs.getBoolean("sound_effects", true));
        swSound.setOnCheckedChangeListener((btn, isChecked) ->
                appPrefs.edit().putBoolean("sound_effects", isChecked).apply());

        // Daily goal
        int goal = appPrefs.getInt("daily_goal", 1);
        txtGoal.setText(goal + " hr / day");
        rowGoal.setOnClickListener(v -> {
            String[] options = {
                "1 hr / day", "2 hrs / day", "3 hrs / day", "4 hrs / day", "5 hrs / day"
            };
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.daily_goal_title))
                .setItems(options, (d, which) -> {
                    appPrefs.edit().putInt("daily_goal", which + 1).apply();
                    txtGoal.setText(options[which]);
                }).show();
        });

        // Log Mode
        View rowLogMode = settingsView.findViewById(R.id.rowLogMode);
        TextView txtLogMode = settingsView.findViewById(R.id.txtLogMode);
        boolean enterMode = appPrefs.getBoolean("log_mode_enter", false);
        txtLogMode.setText(enterMode ? getString(R.string.log_mode_enter) : getString(R.string.log_mode_tap));
        rowLogMode.setOnClickListener(v -> {
            String[] options = {getString(R.string.log_mode_tap), getString(R.string.log_mode_enter)};
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.log_mode))
                .setItems(options, (d, which) -> {
                    boolean isEnter = which == 1;
                    appPrefs.edit().putBoolean("log_mode_enter", isEnter).apply();
                    txtLogMode.setText(options[which]);
                }).show();
        });

        // Language
        View rowLanguage = settingsView.findViewById(R.id.rowLanguage);
        TextView txtCurrentLang = settingsView.findViewById(R.id.txtCurrentLanguage);
        String[] langNames = {"English", "Français", "Español", "中文", "አማርኛ", "Kiswahili"};
        String[] langCodes = {"en", "fr", "es", "zh", "am", "sw"};
        String currentCode = LocaleHelper.getSavedCode(this);
        for (int i = 0; i < langCodes.length; i++) {
            if (langCodes[i].equals(currentCode)) {
                if (txtCurrentLang != null) txtCurrentLang.setText(langNames[i] + "  ›");
                break;
            }
        }
        rowLanguage.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle("Language")
                .setItems(langNames, (d, which) -> {
                    LocaleHelper.setAndSave(this, langCodes[which]);
                    recreate();
                }).show());

        rowHelp.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.help_title))
                .setMessage(getString(R.string.help_msg))
                .setPositiveButton(getString(R.string.got_it), null)
                .show());

        rowPrivacy.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.privacy_title))
                .setMessage(getString(R.string.privacy_msg))
                .setPositiveButton(getString(R.string.ok), null)
                .show());

        rowReset.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.reset_streak_title))
                .setMessage(getString(R.string.reset_streak_msg))
                .setPositiveButton(getString(R.string.reset), (d, w) -> {
                    new StreakManager(this).reset();
                    updateStreakDisplay();
                    Toast.makeText(this, getString(R.string.streak_reset_toast), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show());

        rowClear.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.clear_data_title))
                .setMessage(getString(R.string.clear_data_msg))
                .setPositiveButton(getString(R.string.clear), (d, w) ->
                    Executors.newSingleThreadExecutor().execute(() -> {
                        db.hobbyDao().deleteAll();
                        new StreakManager(this).reset();
                        runOnUiThread(() ->
                            Toast.makeText(this, getString(R.string.data_cleared_toast), Toast.LENGTH_SHORT).show());
                    }))
                .setNegativeButton(getString(R.string.cancel), null)
                .show());

        btnLogOut.setOnClickListener(v ->
            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.logout_title))
                .setMessage(getString(R.string.logout_msg))
                .setPositiveButton(getString(R.string.log_out), (d, w) -> {
                    String currentName = getSharedPreferences("app_prefs", MODE_PRIVATE)
                            .getString("user_name", "");

                    // Save this user's personal settings before logging out
                    saveUserSettings(currentName);

                    getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                            .remove("user_name")
                            .remove("avatar_uri")
                            .putBoolean("is_logged_in", false)
                            .putString("last_user_name", currentName)
                            .apply();
                    AppDatabase.resetInstance();
                    Intent intent = new Intent(this, WelcomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show());
    }

    private String formatTime(int hour, int minute) {
        String amPm = hour >= 12 ? "PM" : "AM";
        int h = hour % 12;
        if (h == 0) h = 12;
        return String.format(Locale.getDefault(), "%d:%02d %s", h, minute, amPm);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }

    private void showStreakHistory() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_streak, null);

        int streak = streakManager.getStreak();
        float[] weekHours = weeklyTracker.getWeeklyHours();

        // Streak number
        TextView txtNum = view.findViewById(R.id.txtStreakNumber);
        txtNum.setText(String.valueOf(streak));

        // Motivational message
        TextView txtMsg = view.findViewById(R.id.txtStreakMsg);
        String msg;
        if (streak == 0) msg = getString(R.string.streak_msg_0);
        else if (streak == 1) msg = getString(R.string.streak_msg_1);
        else if (streak >= 30) msg = getString(R.string.streak_msg_30);
        else if (streak >= 7) msg = getString(R.string.streak_msg_7);
        else if (streak >= 3) msg = getString(R.string.streak_msg_3);
        else msg = getString(R.string.streak_msg_default);
        txtMsg.setText(msg);

        // 7-day dots
        LinearLayout dotsRow = view.findViewById(R.id.weekDots);
        String[] days = {
            getString(R.string.day_mon), getString(R.string.day_tue),
            getString(R.string.day_wed), getString(R.string.day_thu),
            getString(R.string.day_fri), getString(R.string.day_sat),
            getString(R.string.day_sun)
        };

        // Find today's index (0=Mon, 6=Sun)
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int dow = cal.get(java.util.Calendar.DAY_OF_WEEK);
        int todayIndex = (dow == java.util.Calendar.SUNDAY) ? 6 : dow - java.util.Calendar.MONDAY;

        float density = getResources().getDisplayMetrics().density;
        int dotSize = (int) (36 * density);
        int margin = (int) (6 * density);

        for (int i = 0; i < 7; i++) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            colParams.setMargins(margin, 0, margin, 0);
            col.setLayoutParams(colParams);

            // Dot
            View dot = new View(this);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
            dot.setLayoutParams(dotParams);
            if (i == todayIndex) {
                dot.setBackgroundResource(weekHours[i] > 0 ?
                        R.drawable.dot_today : R.drawable.dot_empty);
                // Add green ring for today
                if (weekHours[i] == 0) {
                    android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                    gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                    gd.setColor(0xFFE5E5E5);
                    gd.setStroke((int)(3*density), 0xFF7DC52A);
                    dot.setBackground(gd);
                }
            } else {
                dot.setBackgroundResource(weekHours[i] > 0 ?
                        R.drawable.dot_active : R.drawable.dot_empty);
            }
            col.addView(dot);

            // Day label
            TextView dayTv = new TextView(this);
            dayTv.setText(days[i]);
            dayTv.setTextSize(11);
            dayTv.setTextColor(i == todayIndex ? 0xFF7DC52A : 0xFF777777);
            dayTv.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams lblParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lblParams.topMargin = (int)(4*density);
            dayTv.setLayoutParams(lblParams);
            if (i == todayIndex) dayTv.setTypeface(null, android.graphics.Typeface.BOLD);
            col.addView(dayTv);

            dotsRow.addView(col);
        }

        AlertDialog dialog = new AlertDialog.Builder(this,
                android.R.style.Theme_Material_Light_Dialog_NoActionBar)
                .setView(view)
                .create();
        dialog.setCancelable(true);
        view.findViewById(R.id.btnStreakContinue).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void saveUserSettings(String username) {
        if (username == null || username.isEmpty()) return;
        android.content.SharedPreferences src = getSharedPreferences("app_prefs", MODE_PRIVATE);
        android.content.SharedPreferences dst = getSharedPreferences("user_prefs_" + username, MODE_PRIVATE);
        dst.edit()
            .putString("avatar_uri", src.getString("avatar_uri", null))
            .putString("user_name", username)
            .putBoolean("dark_mode", src.getBoolean("dark_mode", false))
            .putBoolean("sound_effects", src.getBoolean("sound_effects", true))
            .putBoolean("log_mode_enter", src.getBoolean("log_mode_enter", false))
            .putInt("daily_goal", src.getInt("daily_goal", 1))
            .putString("language_code", src.getString("language_code", "en"))
            .apply();
    }

    private void restoreUserSettings(String username) {
        if (username == null || username.isEmpty()) return;
        android.content.SharedPreferences src = getSharedPreferences("user_prefs_" + username, MODE_PRIVATE);
        if (!src.contains("user_name")) return; // no saved settings for this user
        android.content.SharedPreferences dst = getSharedPreferences("app_prefs", MODE_PRIVATE);
        dst.edit()
            .putString("avatar_uri", src.getString("avatar_uri", null))
            .putBoolean("dark_mode", src.getBoolean("dark_mode", false))
            .putBoolean("sound_effects", src.getBoolean("sound_effects", true))
            .putBoolean("log_mode_enter", src.getBoolean("log_mode_enter", false))
            .putInt("daily_goal", src.getInt("daily_goal", 1))
            .putString("language_code", src.getString("language_code", "en"))
            .apply();
    }

    private void loadAvatar(String uriString) {
        try {
            if (settingsView == null) return;
            ImageView img = settingsView.findViewById(R.id.imgProfileAvatar);
            TextView emoji = settingsView.findViewById(R.id.txtAvatarEmoji);
            if (img == null) return;

            android.net.Uri uri = android.net.Uri.parse(uriString);
            img.setImageURI(null); // clear cache
            img.setImageURI(uri);
            img.setVisibility(View.VISIBLE);
            if (emoji != null) emoji.setVisibility(View.GONE);
            img.setClipToOutline(true);
            img.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(android.view.View view, android.graphics.Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
        } catch (Exception ignored) {}
    }

    private void updateStreakDisplay() {
        if (txtStreakVal != null) {
            txtStreakVal.setText(String.valueOf(streakManager.getStreak()));
        }
    }

    private void updateGlobalHeaderStats(List<Hobby> hobbies) {
        double total = 0;
        for (Hobby h : hobbies) total += h.totalHours;
        if (txtTotalHoursVal != null) {
            if (total >= 1000) {
                txtTotalHoursVal.setText(String.format("%.1fK", total / 1000.0));
            } else {
                txtTotalHoursVal.setText(String.valueOf((int) total));
            }
        }

        // Daily goal progress in header
        int dailyGoal = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getInt("daily_goal", 1);
        float todayHours = weeklyTracker.getTodayHours();
        TextView txtGoalProg = findViewById(R.id.txtDailyGoalProgress);
        if (txtGoalProg != null) {
            txtGoalProg.setText(String.format(Locale.getDefault(),
                    "%.1f/%d", todayHours, dailyGoal));
        }
    }

    private void updateStatsPage(List<Hobby> hobbies) {
        LinearLayout container = statsView.findViewById(R.id.weeklyBarsContainer);
        if (container == null) return;

        float[] weekHours = weeklyTracker.getWeeklyHours();
        String[] days = {
            getString(R.string.day_mon), getString(R.string.day_tue),
            getString(R.string.day_wed), getString(R.string.day_thu),
            getString(R.string.day_fri), getString(R.string.day_sat),
            getString(R.string.day_sun)
        };

        // Find max for scaling
        float max = 1f;
        for (float h : weekHours) if (h > max) max = h;

        container.removeAllViews();
        int barMaxHeightDp = 120;
        float density = getResources().getDisplayMetrics().density;
        int barMaxHeightPx = (int) (barMaxHeightDp * density);

        for (int i = 0; i < 7; i++) {
            float hours = weekHours[i];

            // Column: hours label + bar + day label
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                    0, android.view.ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            col.setLayoutParams(colParams);

            // Hours label
            TextView hoursLabel = new TextView(this);
            String hoursText;
            if (hours == 0) {
                hoursText = "";
            } else if (hours == Math.floor(hours)) {
                hoursText = (int) hours + "h";
            } else {
                hoursText = String.format(Locale.getDefault(), "%.1fh", hours);
            }
            hoursLabel.setText(hoursText);
            hoursLabel.setTextSize(11);
            hoursLabel.setTextColor(0xFF3C3C3C);
            hoursLabel.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams hlParams = new LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            hlParams.bottomMargin = (int) (4 * density);
            hoursLabel.setLayoutParams(hlParams);
            col.addView(hoursLabel);

            // Bar
            View bar = new View(this);
            int barHeight = hours == 0 ? (int) (4 * density) :
                    Math.max((int) (barMaxHeightPx * (hours / max)), (int) (8 * density));
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                    (int) (20 * density), barHeight);
            barParams.bottomMargin = (int) (6 * density);
            bar.setLayoutParams(barParams);
            android.graphics.drawable.GradientDrawable barBg =
                    new android.graphics.drawable.GradientDrawable();
            barBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            barBg.setCornerRadius(12 * density);
            barBg.setColor(hours == 0 ? 0xFFE5E5E5 : 0xFF7DC52A);
            bar.setBackground(barBg);
            col.addView(bar);

            // Day label
            TextView dayLabel = new TextView(this);
            // Truncate to 3 chars max to fit in portrait
            String dayText = days[i].length() > 3 ? days[i].substring(0, 3) : days[i];
            dayLabel.setText(dayText);
            dayLabel.setTextSize(10);
            dayLabel.setTextColor(0xFF777777);
            dayLabel.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams dlParams = new LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dayLabel.setLayoutParams(dlParams);
            col.addView(dayLabel);

            container.addView(col);
        }

        // Path to Mastery — per hobby
        android.widget.Spinner spinner = statsView.findViewById(R.id.spinnerHobby);
        TextView txtHobbyHours = statsView.findViewById(R.id.txtHobbyHours);
        TextView txtHobbyPercent = statsView.findViewById(R.id.txtHobbyPercent);

        if (spinner != null && !hobbies.isEmpty()) {
            String[] hobbyNames = new String[hobbies.size()];
            for (int i = 0; i < hobbies.size(); i++) hobbyNames[i] = hobbies.get(i).name;

            android.widget.ArrayAdapter<String> spinnerAdapter = new android.widget.ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, hobbyNames);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(spinnerAdapter);

            android.widget.AdapterView.OnItemSelectedListener listener =
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View v, int pos, long id) {
                        double hrs = hobbies.get(pos).totalHours;
                        txtHobbyHours.setText(String.format(Locale.getDefault(),
                                "%,.0f / 10,000 %s", hrs, getString(R.string.hrs)));
                        int pct = (int) Math.min(hrs / 100, 100);
                        txtHobbyPercent.setText(pct + "%");
                        setMilestone(R.id.milestone100, hrs >= 100);
                        setMilestone(R.id.milestone500, hrs >= 500);
                        setMilestone(R.id.milestone1000, hrs >= 1000);
                        setMilestone(R.id.milestone5000, hrs >= 5000);
                        setMilestone(R.id.milestone10000, hrs >= 10000);
                    }
                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                };
            spinner.setOnItemSelectedListener(listener);
            // Trigger for current selection
            listener.onItemSelected(null, null, spinner.getSelectedItemPosition(), 0);
        } else {
            // No hobbies — reset milestones
            setMilestone(R.id.milestone100, false);
            setMilestone(R.id.milestone500, false);
            setMilestone(R.id.milestone1000, false);
            setMilestone(R.id.milestone5000, false);
            setMilestone(R.id.milestone10000, false);
        }
    }

    private void setMilestone(int viewId, boolean achieved) {
        TextView tv = statsView.findViewById(viewId);
        if (tv == null) return;
        tv.setText(achieved ? "✅" : "🔒");
        tv.setAlpha(achieved ? 1f : 0.4f);
    }

    private void updateRewardsPage(List<Hobby> hobbies) {
        double total = 0;
        for (Hobby h : hobbies) total += h.totalHours;

        TextView txtXP = rewardsView.findViewById(R.id.txtTotalXP);
        if (txtXP != null) txtXP.setText((int) total * 10 + " XP");
        // Streak badges (still global)
        int streak = streakManager.getStreak();
        setBadge(R.id.badge_streak_7, streak >= 7);
        setBadge(R.id.badge_streak_30, streak >= 30);
        setBadge(R.id.badge_streak_100, streak >= 100);

        // Hobby selector for per-hobby badges
        android.widget.Spinner spinner = rewardsView.findViewById(R.id.spinnerRewardHobby);
        if (spinner != null && !hobbies.isEmpty()) {
            String[] names = new String[hobbies.size()];
            for (int i = 0; i < hobbies.size(); i++) names[i] = hobbies.get(i).name;
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                    double hrs = hobbies.get(pos).totalHours;
                    setBadge(R.id.badge_first_hour, hrs >= 1);
                    setBadge(R.id.badge_dedicated, hrs >= 10);
                    setBadge(R.id.badge_century, hrs >= 100);
                    setBadge(R.id.badge_halfway, hrs >= 5000);
                    setBadge(R.id.badge_master, hrs >= 10000);
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
            });
            // Trigger for current selection
            double hrs = hobbies.get(spinner.getSelectedItemPosition()).totalHours;
            setBadge(R.id.badge_first_hour, hrs >= 1);
            setBadge(R.id.badge_dedicated, hrs >= 10);
            setBadge(R.id.badge_century, hrs >= 100);
            setBadge(R.id.badge_halfway, hrs >= 5000);
            setBadge(R.id.badge_master, hrs >= 10000);
        } else {
            setBadge(R.id.badge_first_hour, false);
            setBadge(R.id.badge_dedicated, false);
            setBadge(R.id.badge_century, false);
            setBadge(R.id.badge_halfway, false);
            setBadge(R.id.badge_master, false);
        }

        // Update mastered hobby badges on profile
        updateMasteredBadges(hobbies);
    }

    private void updateMasteredBadges(List<Hobby> hobbies) {
        if (settingsView == null) return;
        android.widget.LinearLayout row = settingsView.findViewById(R.id.masteredBadgesRow);
        if (row == null) return;
        row.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        boolean anyBadge = false;

        for (Hobby h : hobbies) {
            String badge;
            String color;
            if (h.totalHours >= 10000)     { badge = "👑"; color = "#FFC800"; }
            else if (h.totalHours >= 5000) { badge = "💎"; color = "#CE82FF"; }
            else if (h.totalHours >= 1000) { badge = "🥇"; color = "#FFD700"; }
            else if (h.totalHours >= 500)  { badge = "🥈"; color = "#C0C0C0"; }
            else if (h.totalHours >= 100)  { badge = "🥉"; color = "#CD7F32"; }
            else if (h.totalHours >= 10)   { badge = "🔥"; color = "#FF9600"; }
            else if (h.totalHours >= 1)    { badge = "⭐"; color = "#7DC52A"; }
            else continue;

            anyBadge = true;
            android.widget.LinearLayout chip = new android.widget.LinearLayout(this);
            chip.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            chip.setGravity(android.view.Gravity.CENTER_VERTICAL);
            android.widget.LinearLayout.LayoutParams lp =
                    new android.widget.LinearLayout.LayoutParams(
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd((int)(6 * density));
            lp.bottomMargin = (int)(4 * density);
            chip.setLayoutParams(lp);

            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            bg.setCornerRadius(20 * density);
            int col = android.graphics.Color.parseColor(color);
            bg.setColor(android.graphics.Color.argb(30,
                    android.graphics.Color.red(col),
                    android.graphics.Color.green(col),
                    android.graphics.Color.blue(col)));
            chip.setBackground(bg);
            chip.setPadding((int)(8*density), (int)(3*density), (int)(8*density), (int)(3*density));

            TextView tv = new TextView(this);
            tv.setText(badge + " " + h.name);
            tv.setTextColor(col);
            tv.setTextSize(11);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            chip.addView(tv);
            row.addView(chip);
        }

        if (!anyBadge) {
            TextView none = new TextView(this);
            none.setText("🔒");
            none.setTextSize(16);
            none.setAlpha(0.4f);
            row.addView(none);
        }
    }

    private void setBadge(int viewId, boolean earned) {
        View badge = rewardsView.findViewById(viewId);
        if (badge == null) return;
        badge.setAlpha(earned ? 1f : 0.35f);
    }

    private void showLevelUpCelebration(int level) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this,
                android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_level_up, null);

        TextView txtMsg = view.findViewById(R.id.txtLevelReached);
        if (txtMsg != null) txtMsg.setText(getString(R.string.level) + " " + level + "!");

        AlertDialog dialog = builder.setView(view).create();
        dialog.setCancelable(false);
        view.findViewById(R.id.btnContinue).setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        // Play celebration sound
        soundManager.playLevelUpSound();

        // Animate confetti drop
        View confetti = view.findViewById(R.id.txtConfetti);
        if (confetti != null) {
            android.view.animation.Animation confettiAnim =
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.confetti_fall);
            confetti.startAnimation(confettiAnim);
        }

        // Bounce the LEVEL UP title
        View title = view.findViewById(R.id.txtLevelUpTitle);
        if (title != null) {
            android.view.animation.Animation bounce =
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.level_up_bounce);
            bounce.setStartOffset(300);
            title.startAnimation(bounce);
        }

        // Jump the mascot
        ImageView mascot = view.findViewById(R.id.imgMascotLevelUp);
        if (mascot != null) {
            android.view.animation.Animation jump =
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.mascot_jump);
            jump.setStartOffset(600);
            mascot.startAnimation(jump);
        }

        // Stars fade in
        View stars = view.findViewById(R.id.txtStars);
        if (stars != null) {
            android.view.animation.Animation fade =
                android.view.animation.AnimationUtils.loadAnimation(this,
                    android.R.anim.fade_in);
            fade.setStartOffset(800);
            stars.startAnimation(fade);
        }
    }

    private void showAddHobbyDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_hobby, null);
        EditText etName = dialogView.findViewById(R.id.etHobbyName);
        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(getString(R.string.add), (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        Executors.newSingleThreadExecutor().execute(() ->
                                db.hobbyDao().insert(new Hobby(name, "star")));
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }
}
