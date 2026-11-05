package com.islamyolu.app.VakitApi;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.islamyolu.app.R;
import com.islamyolu.app.main.MainActivity;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.SentNotificationPreferences;

/**
 * "Vakte X dakika kaldı" bildirimi. Alarmları {@link PrayerAlarmScheduler} kurar.
 * Her bildirimden sonra alarmlar yeniden kurulur: zincir bir yerde koptuysa kendiliğinden toparlanır.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    private static final String CHANNEL_ID = "namaz_vakitleri_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!PrayerAlarmScheduler.ACTION_PRAYER_ALARM.equals(intent.getAction())) {
            return; // önceki sürümün "bildirimi kaldır" alarmları: artık setTimeoutAfter kullanılıyor
        }
        String notificationId = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_NOTIFICATION_ID);
        long prayerAt = intent.getLongExtra(PrayerAlarmScheduler.EXTRA_PRAYER_AT, 0L);
        long now = System.currentTimeMillis();

        SentNotificationPreferences sent = AppPreferences.from(context).sentNotifications();
        if (notificationId != null && sent.isSent(notificationId)) {
            Log.d(TAG, "Zaten gönderildi: " + notificationId);
        } else if (prayerAt > 0 && now >= prayerAt) {
            // Cihaz alarmı vakit geçtikten sonra teslim etti: "X dakika kaldı" artık yanlış olur
            Log.w(TAG, "Vakit geçmiş, geç gelen bildirim gösterilmedi: " + notificationId);
        } else {
            showNotification(context, intent, notificationId, prayerAt, now);
            if (notificationId != null) {
                sent.markSent(notificationId);
            }
        }

        PendingResult result = goAsync();
        PrayerAlarmScheduler.rescheduleAsync(context, result::finish);
    }

    private void showNotification(Context context, Intent intent, String notificationId, long prayerAt, long now) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) {
            Log.w(TAG, "Uygulama bildirimleri kapalı; vakit bildirimi gösterilemedi");
            return;
        }
        createChannel(context);

        int id = notificationId != null ? notificationId.hashCode() : (int) (now / 1000);
        if (id == NotificationConstants.FOREGROUND_NOTIFICATION_ID) {
            id++; // kalıcı bildirimin yerine geçmesin
        }
        Intent open = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(context, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.crescent)
                .setContentTitle(intent.getStringExtra(PrayerAlarmScheduler.EXTRA_TITLE))
                .setContentText(intent.getStringExtra(PrayerAlarmScheduler.EXTRA_MESSAGE))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);
        if (prayerAt > now) {
            // Vakit girince kendiliğinden kalkar (eskiden ayrı bir alarm kuruluyordu)
            builder.setTimeoutAfter(prayerAt - now);
        }
        try {
            manager.notify(id, builder.build());
            Log.d(TAG, "Vakit bildirimi gösterildi: " + notificationId);
        } catch (SecurityException e) {
            Log.w(TAG, "Bildirim izni yok", e); // Android 13+: POST_NOTIFICATIONS geri alınmış
        }
    }

    private static void createChannel(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        // minSdk 27: her sürümde gerekli; tekrar oluşturmak zararsızdır (kullanıcı ayarlarını değiştirmez)
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.prayer_alarm_channel_name), NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.prayer_alarm_channel_description));
        channel.enableLights(true);
        channel.enableVibration(true);
        nm.createNotificationChannel(channel);
    }
}
