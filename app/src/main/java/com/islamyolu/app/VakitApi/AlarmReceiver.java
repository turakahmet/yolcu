package com.islamyolu.app.VakitApi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.SentNotificationPreferences;

/**
 * "Vakte X dakika kaldı" ve kerahat başlangıcı alarmı. Alarmları {@link PrayerAlarmScheduler} kurar, bildirimi
 * {@link PrayerReminderNotification} gösterir. Her bildirimden sonra alarmlar yeniden kurulur:
 * zincir bir yerde koptuysa kendiliğinden toparlanır.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

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
            int id = notificationId != null ? notificationId.hashCode() : (int) (now / 1000);
            if (id == NotificationConstants.FOREGROUND_NOTIFICATION_ID) {
                id++; // kalıcı bildirimin yerine geçmesin
            }
            // Vakit girince kendiliğinden kalkar
            PrayerReminderNotification.Channel channel =
                    intent.getBooleanExtra(PrayerAlarmScheduler.EXTRA_KERAHAT, false)
                            ? PrayerReminderNotification.Channel.KERAHAT
                            : PrayerReminderNotification.Channel.PRAYER;
            PrayerReminderNotification.Result result = PrayerReminderNotification.show(context, channel, id,
                    intent.getStringExtra(PrayerAlarmScheduler.EXTRA_TITLE),
                    intent.getStringExtra(PrayerAlarmScheduler.EXTRA_MESSAGE),
                    prayerAt > now ? prayerAt - now : 0);
            Log.d(TAG, "Vakit bildirimi: " + notificationId + " → " + result);
            if (notificationId != null && result == PrayerReminderNotification.Result.SHOWN) {
                sent.markSent(notificationId);
            }
        }

        PendingResult pendingResult = goAsync();
        PrayerAlarmScheduler.rescheduleAsync(context, pendingResult::finish);
    }
}
