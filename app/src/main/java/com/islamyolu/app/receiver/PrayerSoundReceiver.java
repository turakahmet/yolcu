package com.islamyolu.app.receiver;

import static com.islamyolu.app.main.MainActivity.SETTINGS_PREFS;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.PowerManager;
import android.util.Log;

import com.islamyolu.app.Service.PrayerSoundControlService;
import com.islamyolu.app.Service.SharedPrefsHelperService;
import com.islamyolu.app.Service.SoundService;

public class PrayerSoundReceiver extends BroadcastReceiver {
    private static final String TAG = "PrayerSoundReceiver";
    private static PowerManager.WakeLock wakeLock;
    private static boolean isCleaningUp = false;
    private static int originalRingerMode = -1;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "PrayerSoundReceiver.onReceive başladı - Action: " + intent.getAction());

        // Intent'in Action değerini kontrol et
        if (intent.getAction() == null || !intent.getAction().equals("com.myapp.yolcu.ACTION_ALARM_NOTIFICATION")) {
            Log.d(TAG, "Geçersiz veya bilinmeyen Action, ses çalma işlemi başlatılmadı.");
            return;
        }

        try {
            SharedPrefsHelperService prefsHelper = new SharedPrefsHelperService(context, SETTINGS_PREFS);

            // Ses modunu kontrol et ve değiştir
            if (prefsHelper.isVakitSoundModeEnabled()) {
                setVibrationMode(context);
            }

            // Eğer STOP_PRAYER_SOUND action'ı ise sesi durdur
            if (PrayerSoundControlService.ACTION_STOP_SOUND.equals(intent.getAction())) {
                Log.d(TAG, "Stop sound action alındı");
                cleanupSafely();
                return;
            }

            // WakeLock al
            acquireWakeLock(context);

            // Intent ve extras kontrolleri
            if (intent.getExtras() == null) {
                Log.e(TAG, "Intent veya extras null");
                cleanupSafely();
                return;
            }

            // Tüm bildirimler kapalı ise çalma
            if (!prefsHelper.getSwitchState("switchNotificationAll")) {
                Log.d(TAG, "Tüm bildirimler kapalı");
                cleanupSafely();
                return;
            }

            // Intent'ten ezan ve dua tiplerini al
            int ezanType = intent.getIntExtra("ezanType", 2);
            int duaType = intent.getIntExtra("duaType", 2);

            Log.d(TAG, String.format("Intent değerleri - ezanType: %d, duaType: %d", ezanType, duaType));

            // Ses kapalı ise çalma
            if (!prefsHelper.getSwitchState("switchNotificationSound")) {
                Log.d(TAG, "Bildirim sesi kapalı");
                cleanupSafely();
                return;
            }

            // SoundService instance'ı al veya oluştur
            SoundService soundService = SoundService.getInstance(context);

            if (soundService != null) {
                // Completion callback'i ayarla
                soundService.setCompletionCallback(() -> {
                    Log.d(TAG, "CompletionCallback tetiklendi");
                    // Ses modunu geri al buna şimdilik gerek yok
//                    if (prefsHelper.isVakitSoundModeEnabled()) {
//                        restoreRingerMode(context);
//                    }
                    cleanupSafely();
                });

                // Sesi çal
                Log.d(TAG, "playPrayerSound çağrılıyor");
                soundService.playPrayerSound(ezanType, duaType);
            } else {
                Log.e(TAG, "SoundService oluşturulamadı");
                cleanupSafely();
            }

        } catch (Exception e) {
            Log.e(TAG, "Hata oluştu: " + e.getMessage(), e);
            // Hata durumunda ses modunu geri al
            restoreRingerMode(context);
            cleanupSafely();
        }
    }

    private synchronized void cleanupSafely() {
        if (isCleaningUp) {
            Log.d(TAG, "Cleanup zaten devam ediyor, tekrar çağrılmadı");
            return;
        }

        try {
            isCleaningUp = true;
            SoundService service = SoundService.getInstance();
            if (service != null) {
                service.cleanup();
            }
            releaseWakeLock();
            Log.d(TAG, "Cleanup başarıyla tamamlandı");
        } catch (Exception e) {
            Log.e(TAG, "Cleanup sırasında hata: " + e.getMessage());
        } finally {
            isCleaningUp = false;
        }
    }

    private String getEzanTypeName(int type) {
        switch (type) {
            case 0:
                return "Ezan";
            case 1:
                return "Kısa ezan";
            case 2:
                return "Bildirim";
            case 3:
                return "Kapalı";
            default:
                return "Bilinmiyor";
        }
    }

    private String getDuaTypeName(int type) {
        switch (type) {
            case 0:
                return "Ezan dua";
            case 1:
                return "Ezan dua ve meal";
            case 2:
                return "Kapalı";
            default:
                return "Bilinmiyor";
        }
    }

    private void acquireWakeLock(Context context) {
        try {
            if (wakeLock == null) {
                PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "MyApp:PrayerSoundWakeLock"
                );
            }
            if (!wakeLock.isHeld()) {
                wakeLock.acquire(15 * 60 * 1000L); // 15 dakika
                Log.d(TAG, "WakeLock alındı");
            }
        } catch (Exception e) {
            Log.e(TAG, "WakeLock alınırken hata: " + e.getMessage(), e);
        }
    }

    private void releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "WakeLock serbest bırakıldı");
            }
        } catch (Exception e) {
            Log.e(TAG, "WakeLock serbest bırakılırken hata: " + e.getMessage(), e);
        }
    }

    private void setVibrationMode(Context context) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                // Mevcut ses modunu kaydet
                originalRingerMode = audioManager.getRingerMode();
                Log.d(TAG, "Orijinal ses modu kaydedildi: " + getRingerModeName(originalRingerMode));

                // Titreşim moduna al
                audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                Log.d(TAG, "Ses modu titreşime alındı");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ses modu değiştirilirken hata: " + e.getMessage());
        }
    }

    private void restoreRingerMode(Context context) {
        try {
            if (originalRingerMode != -1) {
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    audioManager.setRingerMode(originalRingerMode);
                    Log.d(TAG, "Ses modu geri alındı: " + getRingerModeName(originalRingerMode));
                    originalRingerMode = -1;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ses modu geri alınırken hata: " + e.getMessage());
        }
    }

    private String getRingerModeName(int mode) {
        switch (mode) {
            case AudioManager.RINGER_MODE_SILENT:
                return "Sessiz";
            case AudioManager.RINGER_MODE_VIBRATE:
                return "Titreşim";
            case AudioManager.RINGER_MODE_NORMAL:
                return "Normal";
            default:
                return "Bilinmiyor";
        }
    }
} 
