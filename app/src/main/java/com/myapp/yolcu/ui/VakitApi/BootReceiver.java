package com.myapp.yolcu.ui.VakitApi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.myapp.yolcu.ui.Service.PrayerNotificationService;
import com.myapp.yolcu.ui.Service.SharedPrefsHelperService;
import com.myapp.yolcu.ui.main.MainActivity;

import java.util.Objects;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final long DELAY_BEFORE_NOTIFICATION_SERVICE = 5000; // 5 saniye

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "Cihaz yeniden başlatıldı");
            
            SharedPrefsHelperService prefs = new SharedPrefsHelperService(context, MainActivity.SETTINGS_PREFS);
            
            // Önce DailyPrayerWorkerReceiver'ı başlat
            Intent alarmService = new Intent(context, DailyPrayerWorkerReceiver.class);
            context.sendBroadcast(alarmService);
            Log.d(TAG, "DailyPrayerWorkerReceiver başlatıldı");

            // Bildirim servisi aktif mi kontrol et
            if (prefs.getSwitchState("switchNotificationVakitHerzamanGoster")) {
                // DailyPrayerWorkerReceiver'ın işini tamamlaması için bekle
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Log.d(TAG, "PrayerNotificationService başlatılıyor");
                    Intent serviceIntent = new Intent(context, PrayerNotificationService.class);
                    try {
                        context.startForegroundService(serviceIntent);
                        Log.d(TAG, "PrayerNotificationService başarıyla başlatıldı");
                    } catch (Exception e) {
                        Log.e(TAG, "PrayerNotificationService başlatılırken hata: " + e.getMessage());
                    }
                }, DELAY_BEFORE_NOTIFICATION_SERVICE);
            } else {
                Log.d(TAG, "Bildirim servisi kullanıcı tarafından devre dışı bırakılmış");
            }
        }
    }
} 