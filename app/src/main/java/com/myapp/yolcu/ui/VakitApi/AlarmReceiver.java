package com.myapp.yolcu.ui.VakitApi;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.myapp.yolcu.ui.main.MainActivity;
import com.myapp.yolcu.R;
import com.myapp.yolcu.ui.Service.SharedPrefsHelperService;

import static com.myapp.yolcu.ui.main.MainActivity.SETTINGS_PREFS;

import java.time.LocalDate;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "namaz_vakitleri_channel";
    public static final String ACTION_ALARM_NOTIFICATION = "com.myapp.yolcu.ACTION_ALARM_NOTIFICATION";
    private static final String ACTION_CANCEL_NOTIFICATION = "com.myapp.yolcu.ACTION_CANCEL_NOTIFICATION";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlarmReceiver", "onReceive called with action: " + intent.getAction());
        
        if (intent != null) {
            // Action kontrolünü düzelt
            if (intent.getAction() != null && 
                intent.getAction().equals("com.myapp.yolcu.ACTION_ALARM_NOTIFICATION")) {
                
                String notificationId = intent.getStringExtra("notificationId");
                
                // Bildirim daha önce gönderilmemiş ise gönder
                if (!isNotificationSent(context, notificationId)) {
                    showNotification(context, intent);
                    // Bildirimi gönderildi olarak işaretle
                    markNotificationAsSent(context, notificationId);
                }
            } else if (ACTION_CANCEL_NOTIFICATION.equals(intent.getAction())) {
                cancelNotification(context, intent.getIntExtra("notificationId", -1));
            }
        }
    }

    private void showNotification(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        
        Log.d("AlarmReceiver", "Showing notification - Title: " + title + ", Message: " + message);
        
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Notification Channel oluştur (Android 8.0 ve üzeri için)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Namaz Vakitleri",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Namaz vakti bildirimleri");
            channel.enableLights(true);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        // Ana aktiviteye yönlendirecek PendingIntent
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        int notificationId = generateNotificationId(title, message);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Bildirimi oluştur
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.crescent)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Bildirimi göster
        notificationManager.notify(notificationId, builder.build());
        Log.d("AlarmReceiver", "Notification shown with ID: " + notificationId);
    }

    private int generateNotificationId(String title, String message) {
        // Aynı namaz vakti için aynı ID'yi üret
        String prayerType = extractPrayerType(title); // Örn: "IMSAK", "YATSI" vs.
        String uniqueString = prayerType + "_" + LocalDate.now();
        return Math.abs(uniqueString.hashCode());
    }

    private String extractPrayerType(String title) {
        // Başlıktan namaz vaktini çıkar
        if (title.contains("Imsak") || title.contains("İmsak")) return "IMSAK";
        if (title.contains("Gunes") || title.contains("Güneş")) return "GUNES";
        if (title.contains("Ogle") || title.contains("Öğle")) return "OGLE";
        if (title.contains("Ikindi") || title.contains("İkindi")) return "IKINDI";
        if (title.contains("Aksam") || title.contains("Akşam")) return "AKSAM";
        if (title.contains("Yatsi") || title.contains("Yatsı")) return "YATSI";
        return title;
    }

    private void scheduleNotificationRemoval(Context context, int notificationId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        SharedPrefsHelperService sharedPrefsHelperService = new SharedPrefsHelperService(context, SETTINGS_PREFS);

        Intent cancelIntent = new Intent(context, AlarmReceiver.class);
        cancelIntent.setAction(ACTION_CANCEL_NOTIFICATION);
        cancelIntent.putExtra("notificationId", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Kullanıcının seçtiği süreyi al (dakika cinsinden)
        int userSelectedMinutes = sharedPrefsHelperService.getRadioButtonState("radioGroupVakit");
        
        // Bildirimi namaz vaktinde kaldır
        long triggerTime = System.currentTimeMillis() + (userSelectedMinutes * 60 * 1000);
        
        if (alarmManager != null) {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            );
            Log.d("AlarmReceiver", "Bildirim kaldırma alarmı kuruldu: " + notificationId + 
                    " - " + userSelectedMinutes + " dakika sonra kaldırılacak");
        }
    }

    private void cancelNotification(Context context, int notificationId) {
        if (notificationId != -1) {
            NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
            Log.d("AlarmReceiver", "Bildirim kaldırıldı: " + notificationId);
        }
    }

    private boolean isNotificationSent(Context context, String notificationId) {
        SharedPreferences prefs = context.getSharedPreferences(
            "SentNotifications", 
            Context.MODE_PRIVATE
        );
        return prefs.getBoolean(notificationId, false);
    }

    private void markNotificationAsSent(Context context, String notificationId) {
        SharedPreferences prefs = context.getSharedPreferences(
            "SentNotifications", 
            Context.MODE_PRIVATE
        );
        prefs.edit().putBoolean(notificationId, true).apply();
    }
}
