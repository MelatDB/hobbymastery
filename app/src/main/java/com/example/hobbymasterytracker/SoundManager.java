package com.example.hobbymasterytracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

public class SoundManager {

    private static final String PREFS = "app_prefs";
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public SoundManager(Context context) {
        this.context = context.getApplicationContext();
    }

    private boolean isSoundEnabled() {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("sound_effects", true);
    }

    /** Short ding when logging +1 hour */
    public void playPracticeSound() {
        if (!isSoundEnabled()) return;
        playTone(ToneGenerator.TONE_PROP_BEEP, 120);
    }

    /**
     * Clapping + fanfare for level up:
     * Rapid clap bursts followed by ascending celebration tones
     */
    public void playLevelUpSound() {
        if (!isSoundEnabled()) return;

        // Simulate clapping: rapid short bursts at clap rhythm
        int[] clapDelays = {0, 120, 240, 360, 480, 600, 700, 800};
        for (int delay : clapDelays) {
            handler.postDelayed(() ->
                playTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 80), delay);
        }

        // After claps, play ascending fanfare
        handler.postDelayed(() -> playTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 150), 1000);
        handler.postDelayed(() -> playTone(ToneGenerator.TONE_PROP_BEEP, 150), 1200);
        handler.postDelayed(() -> playTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500), 1400);
    }

    /** Soft click for FAB */
    public void playClickSound() {
        if (!isSoundEnabled()) return;
        playTone(ToneGenerator.TONE_PROP_ACK, 60);
    }

    /** Warning tone for delete */
    public void playWarningSound() {
        if (!isSoundEnabled()) return;
        playTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 200);
    }

    private void playTone(int tone, int durationMs) {
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 90);
            tg.startTone(tone, durationMs);
            handler.postDelayed(tg::release, durationMs + 150);
        } catch (Exception ignored) {}
    }
}
