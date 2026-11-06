package com.islamyolu.app.sound;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.islamyolu.app.R;

import java.util.Locale;

/**
 * Ayarlar'daki ezan/dua sesi önizlemesi. Aynı anda tek ses çalar; başka satırda oynatılınca öncekini
 * durdurur. Ekran kapanınca {@link #release()} çağrılmalı. Vakit ezanını çalan PrayerAudioService'ten bağımsızdır
 * (kullanıcı açıkça dinlemek istediği için "Uygulama sesleri" anahtarına bakmaz).
 */
public final class SoundPreviewPlayer {

    /** Bir önizleme satırının görünümleri (view_sound_preview). */
    public static final class Views {
        public final MaterialButton button;
        public final LinearProgressIndicator progress;
        public final TextView time;

        public Views(@NonNull View root) {
            button = root.findViewById(R.id.previewButton);
            progress = root.findViewById(R.id.previewProgress);
            time = root.findViewById(R.id.previewTime);
        }
    }

    private static final String TAG = "SoundPreviewPlayer";
    private static final long PROGRESS_INTERVAL_MS = 250;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable progressTick = this::updateProgress;

    @Nullable private MediaPlayer player;
    @Nullable private Views active;

    /** {@link SoundManager#newPreviewPlayer()} ile alınır. */
    SoundPreviewPlayer(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    /** Aynı satır çalıyorsa durdurur; değilse öncekini durdurup bu sesi çalar. */
    public void toggle(@RawRes int soundRes, @NonNull Views views) {
        boolean sameRow = active == views;
        stop();
        if (sameRow || soundRes == 0) {
            return;
        }
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA) // ezan alarmı da medya sesini kullanır
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        player = MediaPlayer.create(context, soundRes, attributes, AudioManager.AUDIO_SESSION_ID_GENERATE);
        if (player == null) {
            Log.e(TAG, "Önizleme sesi açılamadı: " + soundRes);
            return;
        }
        active = views;
        player.setOnCompletionListener(mp -> stop());
        player.start();
        showPlaying(views, true);
        updateProgress();
    }

    /** Çalan sesi durdurur ve satırı başlangıç haline getirir. */
    public void stop() {
        handler.removeCallbacks(progressTick);
        if (player != null) {
            player.release();
            player = null;
        }
        if (active != null) {
            showPlaying(active, false);
            reset(active);
            active = null;
        }
    }

    public void release() {
        stop();
    }

    /** Seçim "Kapalı" ise önizleme pasif olur. */
    public static void bindIdle(@NonNull Views views, boolean enabled) {
        views.button.setEnabled(enabled);
        reset(views);
    }

    private void updateProgress() {
        if (player == null || active == null) return;
        int duration = Math.max(1, player.getDuration());
        int position = player.getCurrentPosition();
        active.progress.setProgressCompat(position * 100 / duration, false);
        active.time.setText(context.getString(R.string.settings_preview_time, format(position), format(duration)));
        handler.postDelayed(progressTick, PROGRESS_INTERVAL_MS);
    }

    private static void reset(Views views) {
        views.progress.setProgressCompat(0, false);
        views.time.setText(null);
    }

    private void showPlaying(Views views, boolean playing) {
        views.button.setIconResource(playing ? R.drawable.ic_settings_pause : R.drawable.ic_settings_play);
        views.button.setContentDescription(context.getString(
                playing ? R.string.settings_preview_stop : R.string.settings_preview_play));
    }

    private static String format(int millis) {
        int seconds = millis / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }
}
