package com.islamyolu.app.Service;

import static com.islamyolu.app.main.MainActivity.sharedPrefsHelperService;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;

import com.islamyolu.app.R;

public class SoundService {
    private static final String TAG = "SoundService";
    private static volatile SoundService instance;
    private MediaPlayer mediaPlayer;
    private Context context;
    private CompletionCallback completionCallback;
    private boolean isPlaying = false;
    private boolean isCleaningUp = false;

    public static synchronized SoundService getInstance(Context context) {
        if (instance == null) {
            synchronized (SoundService.class) {
                if (instance == null) {
                    instance = new SoundService(context);
                }
            }
        }
        return instance;
    }

    public static synchronized SoundService getInstance() {
        if (instance == null) {
            Log.w(TAG, "SoundService instance null, önce context ile getInstance çağrılmalı");
        }
        return instance;
    }

    public SoundService(Context context) {
        Log.d(TAG, "SoundService constructor çağrıldı");
        this.context = context.getApplicationContext(); // Application context kullan
        context.startService(new Intent(context, PrayerSoundControlService.class));
    }

    // Kendi callback interface'imizi tanımlayalım
    public interface CompletionCallback {
        void onAllSoundsCompleted();
    }

    public void setCompletionCallback(CompletionCallback callback) {
        this.completionCallback = callback;
    }

    // Ses oynatma metodu - uzun sesler için
    private void playLongSound(int soundResourceId, MediaPlayer.OnCompletionListener completionListener) {
        try {
            if (sharedPrefsHelperService == null)
                return;
            else if (!sharedPrefsHelperService.getSwitchState("switchNotificationSound"))
                return;

            Log.d(TAG, "playLongSound başlatılıyor - soundResourceId: " + soundResourceId);

            // Daha önce bir MediaPlayer nesnesi çalışıyorsa, önce durdur ve serbest bırak
            if (mediaPlayer != null) {
                Log.d(TAG, "Önceki MediaPlayer temizleniyor");
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }

            // Yeni MediaPlayer nesnesi başlat
            mediaPlayer = MediaPlayer.create(context, soundResourceId);
            if (mediaPlayer == null) {
                Log.e(TAG, "MediaPlayer oluşturulamadı");
                return;
            }

            // Ses dosyasının süresini al
            int duration = mediaPlayer.getDuration();
            Log.d(TAG, "Ses dosyası süresi: " + duration + " ms");

            // Tamamlanma listener'ını ayarla
            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Ses dosyası tamamlandı");
                isPlaying = false;
                if (completionListener != null) {
                    completionListener.onCompletion(mp);
                }
            });

            // Hata listener'ı ekle
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer hatası - what: " + what + ", extra: " + extra);
                isPlaying = false;
                return false;
            });

            // Hazırlık listener'ı ekle
            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer hazır, çalmaya başlıyor");
                isPlaying = true;
                mp.start();
            });

            // Ses durumu değişikliği listener'ı
            mediaPlayer.setOnInfoListener((mp, what, extra) -> {
                Log.d(TAG, "MediaPlayer bilgisi - what: " + what + ", extra: " + extra);
                return false;
            });

            Log.d(TAG, "Ses dosyası çalmaya başlıyor");
            mediaPlayer.start();
            isPlaying = true;

        } catch (Exception e) {
            Log.e(TAG, "Ses çalma hatası: " + e.getMessage(), e);
            isPlaying = false;
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
        }
    }

    // Kısa sesler için orijinal playSound metodu
    private void playSound(int soundResourceId) {
        if (sharedPrefsHelperService == null)
            return;
        else if (!sharedPrefsHelperService.getSwitchState("switchNotificationSound"))
            return;

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(context, soundResourceId);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.release();
            mediaPlayer = null;
        });
    }

    // Ses durdurma metodu
    public synchronized void stopAndCleanup() {
        Log.d(TAG, "stopAndCleanup çağrıldı");
        if (isCleaningUp) {
            Log.d(TAG, "Cleanup zaten devam ediyor, tekrar çağrılmadı");
            return;
        }

        try {
            isCleaningUp = true;

            if (mediaPlayer != null) {
                try {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                        Log.d(TAG, "MediaPlayer durduruldu");
                    }
                    mediaPlayer.release();
                    Log.d(TAG, "MediaPlayer release edildi");
                } catch (Exception e) {
                    Log.e(TAG, "MediaPlayer işlemleri sırasında hata: " + e.getMessage());
                } finally {
                    mediaPlayer = null;
                }
            }
            isPlaying = false;

            // CompletionCallback'i güvenli şekilde çağır
            CompletionCallback callback = completionCallback;
            completionCallback = null; // Önce referansı temizle
            if (callback != null) {
                try {
                    callback.onAllSoundsCompleted();
                    Log.d(TAG, "Completion callback başarıyla çağrıldı");
                } catch (Exception e) {
                    Log.e(TAG, "Callback çağrılırken hata: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "stopAndCleanup sırasında hata: " + e.getMessage());
        } finally {
            isCleaningUp = false;
        }
    }

    public void cleanup() {
        Log.d(TAG, "cleanup çağrıldı");
        try {
            stopAndCleanup();
            if (context != null) {
                try {
                    context.stopService(new Intent(context, PrayerSoundControlService.class));
                    Log.d(TAG, "PrayerSoundControlService durduruldu");
                } catch (Exception e) {
                    Log.e(TAG, "Service durdurulurken hata: " + e.getMessage());
                }
            }
            instance = null;
            Log.d(TAG, "SoundService instance temizlendi");
        } catch (Exception e) {
            Log.e(TAG, "Cleanup sırasında hata: " + e.getMessage());
        }
    }

    public void successAnswer() {
        playSound(R.raw.success);
    }

    public void wrongAnswer() {
        playSound(R.raw.failure);
    }

    public void wgHintJokerEffect() {
        playSound(R.raw.wg_hint_joker);
    }

    public void jokerEffect() {
        playSound(R.raw.joker);
    }

    public void noUsedJokerEffect() {
        playSound(R.raw.no_used_joker);
    }

    public void timeoutEffect() {
        playSound(R.raw.timeout);
    }

    public void winGameEffect() {
        playSound(R.raw.win_game);
    }

    public void playGameButtonEffect() {
        playSound(R.raw.play_game);
    }

    public void commonButtonEffect() {
        playSound(R.raw.common_button);
    }


    public void successRozet() {
        playSound(R.raw.rozet_success);
    }

    public void matchesSuccessButtonEffect() {
        playSound(R.raw.matches_success);
    }

    public void matchesFailButtonEffect() {
        playSound(R.raw.matches_fail);
    }

    public void playPrayerSound(int ezanType, int duaType) {
        Log.d(TAG, String.format("playPrayerSound başlatılıyor - ezanType: %d, duaType: %d", ezanType, duaType));

        if (ezanType == 4 && duaType == 2) {
            Log.d(TAG, "Her iki ses de kapalı, işlem sonlandırılıyor");
            if (completionCallback != null) {
                completionCallback.onAllSoundsCompleted();
            }
            return;
        }

        if (ezanType != 4) {
            int soundResourceId;
            switch (ezanType) {
                case 0:
                    soundResourceId = R.raw.ezan_uzun;
                    Log.d(TAG, "Uzun ezan seçildi");
                    break;
                case 1:
                    soundResourceId = R.raw.ezan_kisa;
                    Log.d(TAG, "Kısa ezan seçildi");
                    break;
                case 2:
                    soundResourceId = R.raw.ezan_bildirim;
                    Log.d(TAG, "Bildirim 1 sesi seçildi");
                    break;
                case 3:
                    soundResourceId = R.raw.ezan_bildirim_2;
                    Log.d(TAG, "Bildirim 2 sesi seçildi");
                    break;
                default:
                    Log.d(TAG, "Geçersiz ezan tipi");
                    if (completionCallback != null) {
                        completionCallback.onAllSoundsCompleted();
                    }
                    return;
            }

            Log.d(TAG, "Ezan sesi başlatılıyor");
            playLongSound(soundResourceId, mp -> {
                Log.d(TAG, "Ezan sesi tamamlandı, dua kontrolü yapılıyor");
                if (duaType != 2) {
                    Log.d(TAG, "Dua sesi başlatılacak");
                    playDuaSound(duaType);
                } else {
                    Log.d(TAG, "Dua kapalı, işlem tamamlanıyor");
                    if (completionCallback != null) {
                        completionCallback.onAllSoundsCompleted();
                    }
                }
            });
        } else if (duaType != 2) {
            Log.d(TAG, "Sadece dua sesi çalınacak");
            playDuaSound(duaType);
        }
    }

    private void playDuaSound(int duaType) {
        int soundResourceId;
        switch (duaType) {
            case 0:
                soundResourceId = R.raw.ezan_dua;
                break;
            case 1:
                soundResourceId = R.raw.ezan_dua_meal;
                break;
            default:
                if (completionCallback != null) {
                    completionCallback.onAllSoundsCompleted();
                }
                return;
        }
        Log.d(TAG, "Dua tipi " + duaType + " oynatılıyor");
        playLongSound(soundResourceId, mp -> {
            if (completionCallback != null) {
                completionCallback.onAllSoundsCompleted();
            }
        });
    }

    // Ses çalma durumunu kontrol etmek için yeni metod
    public boolean isPlaying() {
        return isPlaying && mediaPlayer != null && mediaPlayer.isPlaying();
    }

    // Diğer ses çalma metodları için interface
    public interface OnCompletionListener {
        void onCompletion(MediaPlayer mp);
    }

    @Override
    protected void finalize() throws Throwable {
        cleanup();
        super.finalize();
    }
}

