package com.islamyolu.app.sound;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;
import android.util.SparseIntArray;

import androidx.annotation.NonNull;

import com.islamyolu.app.IslamYoluApplication;
import com.islamyolu.app.preferences.AppPreferences;

import java.util.EnumMap;
import java.util.Map;

/**
 * Uygulamadaki TÜM seslerin tek giriş noktası (uygulama düzeyinde tek örnek: {@link #from}).
 * <ul>
 *   <li>Kısa efekt sesleri: {@link #play(AppSound)} — "Uygulama sesleri" ayarına uyar.</li>
 *   <li>Vakit ezanı/duası: {@link #startPrayerAudio} — {@link PrayerAudioService} ön plan servisinde çalar.</li>
 *   <li>Ayarlardaki önizleme: {@link #newPreviewPlayer}.</li>
 * </ul>
 * Efektler SoundPool ile çalınır: kısa sesler için tasarlanmıştır, ilk yüklemeden sonra gecikmesizdir ve
 * ekran başına ayrı MediaPlayer açılmaz (eskiden her ekran kendi ses nesnesini oluşturuyordu).
 */
public final class SoundManager {

    private static final String TAG = "SoundManager";
    private static final int MAX_STREAMS = 3;

    private final Context context;
    private final SoundPool pool;
    private final Map<AppSound, Integer> soundIds = new EnumMap<>(AppSound.class);
    /** Yüklemesi bitmeden istenen sesler: yüklenince çalınır (soundId → 1) */
    private final SparseIntArray pendingPlay = new SparseIntArray();
    private final SparseIntArray loaded = new SparseIntArray();

    /** Yalnızca {@link IslamYoluApplication} oluşturur. */
    public SoundManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        pool = new SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        pool.setOnLoadCompleteListener((soundPool, soundId, status) -> {
            synchronized (this) {
                if (status != 0) {
                    Log.w(TAG, "Ses yüklenemedi: " + soundId);
                    return;
                }
                loaded.put(soundId, 1);
                if (pendingPlay.get(soundId) == 1) {
                    pendingPlay.delete(soundId);
                    soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
                }
            }
        });
    }

    @NonNull
    public static SoundManager from(@NonNull Context context) {
        return IslamYoluApplication.from(context).getSoundManager();
    }

    // --- Efekt sesleri ---

    /** Kısa efekt sesi; "Uygulama sesleri" kapalıysa çalmaz. Ses ilk istekte yüklenir. */
    public synchronized void play(@NonNull AppSound sound) {
        if (!AppPreferences.from(context).settings().isNotificationSoundOn()) {
            return;
        }
        Integer soundId = soundIds.get(sound);
        if (soundId == null) {
            soundId = pool.load(context, sound.res, 1);
            soundIds.put(sound, soundId);
        }
        if (loaded.get(soundId) == 1) {
            pool.play(soundId, 1f, 1f, 1, 0, 1f);
        } else {
            pendingPlay.put(soundId, 1);
        }
    }

    // --- Vakit ezanı ---

    /**
     * Ezan (seçiliyse) ve ardından dua (açıksa) çalar; ses kısma tuşu veya bildirimdeki Durdur ile susar.
     *
     * @return servis başlatılamadıysa false (Android 12+'da tam zamanlı alarm izni kapalıyken arka plandan)
     */
    public boolean startPrayerAudio(int ezanType, int duaType) {
        return PrayerAudioService.start(context, PrayerSound.ezanRes(ezanType), PrayerSound.duaRes(duaType));
    }

    // --- Önizleme ---

    /** Ayarlardaki ezan/dua önizlemesi; ekran kapanınca {@link SoundPreviewPlayer#release()} çağrılmalı. */
    @NonNull
    public SoundPreviewPlayer newPreviewPlayer() {
        return new SoundPreviewPlayer(context);
    }
}
