package com.islamyolu.app.VakitApi;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.islamyolu.app.R;
import com.islamyolu.app.Service.SharedPrefsHelperService;
import com.islamyolu.app.main.MainActivity;

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "boot_channel")
                            .setSmallIcon(R.drawable.crescent)
                            .setContentTitle("Namaz Bildirim herzaman üstte Servisi Pasif")
                            .setContentText("Cihaz yeniden başladı! Servis maneul olarak uygulama ayarlarından yeniden etkinleştirmek için uygulamayı açın.(Android 15 ve sonrası için gerekli)")
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setAutoCancel(true)
                            .setContentIntent(PendingIntent.getActivity(context, 0,
                                    new Intent(context, MainActivity.class), PendingIntent.FLAG_IMMUTABLE));

                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    NotificationManagerCompat.from(context).notify(1010, builder.build());
                }
//                // DailyPrayerWorkerReceiver'ın işini tamamlaması için bekle
//                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                    Log.d(TAG, "PrayerNotificationService başlatılıyor");
//                    Intent serviceIntent = new Intent(context, PrayerNotificationService.class);
//                    try {
//                        context.startForegroundService(serviceIntent);
//                        Log.d(TAG, "PrayerNotificationService başarıyla başlatıldı");
//                    } catch (Exception e) {
//                        Log.e(TAG, "PrayerNotificationService başlatılırken hata: " + e.getMessage());
//                    }
//                }, DELAY_BEFORE_NOTIFICATION_SERVICE);
                // WorkManager ile servis başlatma isteği gönder
                WorkManager.getInstance(context).enqueue(
                        new OneTimeWorkRequest.Builder(PrayerNotificationWorker.class).build()
                );

            } else {
                Log.d(TAG, "Bildirim servisi kullanıcı tarafından devre dışı bırakılmış");
            }
        }
    }
} 