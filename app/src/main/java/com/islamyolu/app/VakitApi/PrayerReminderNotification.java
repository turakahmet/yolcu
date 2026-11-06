package com.islamyolu.app.VakitApi;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.islamyolu.app.R;
import com.islamyolu.app.main.MainActivity;

/**
 * Vakit hatırlatması ve kerahat bildirimlerini gösteren TEK yer. Alarm alıcısı ve Ayarlar'daki
 * "Bildirimi test et" aynı kanalı ve görünümü kullanır; test başarılıysa gerçek bildirim de gelir.
 */
public final class PrayerReminderNotification {

    /** Gösterim sonucu: test ekranı kullanıcıya nedenini söyler. */
    public enum Result {
        SHOWN,
        /** Uygulama bildirimleri kapalı veya Android 13+ izni yok */
        APP_NOTIFICATIONS_OFF,
        /** Kanal kullanıcı tarafından kapatılmış */
        CHANNEL_OFF
    }

    /** Bildirim kanalları: kullanıcı sistem ayarlarından ayrı ayrı yönetebilir. */
    public enum Channel {
        /** "Vakte X dakika kaldı" — yüksek öncelik, ses ve titreşim */
        PRAYER("namaz_vakitleri_channel", NotificationManager.IMPORTANCE_HIGH,
                R.string.prayer_alarm_channel_name, R.string.prayer_alarm_channel_description),
        /** Kerahat başlangıcı — bilgi amaçlı, varsayılan öncelik */
        KERAHAT("kerahat_channel", NotificationManager.IMPORTANCE_DEFAULT,
                R.string.kerahat_channel_name, R.string.kerahat_channel_description);

        public final String id;
        final int importance;
        @StringRes final int nameRes;
        @StringRes final int descriptionRes;

        Channel(String id, int importance, @StringRes int nameRes, @StringRes int descriptionRes) {
            this.id = id;
            this.importance = importance;
            this.nameRes = nameRes;
            this.descriptionRes = descriptionRes;
        }
    }

    private static final String TAG = "PrayerReminderNotif";

    private PrayerReminderNotification() {
    }

    /**
     * @param timeoutMillis bu süre sonra bildirim kendiliğinden kalkar; 0 veya negatifse kalmaz
     */
    @NonNull
    public static Result show(@NonNull Context context, @NonNull Channel channel, int id, @Nullable String title,
                              @Nullable String message, long timeoutMillis) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) {
            Log.w(TAG, "Uygulama bildirimleri kapalı; bildirim gösterilemedi");
            return Result.APP_NOTIFICATIONS_OFF;
        }
        createChannel(context, channel);
        NotificationChannel created = manager.getNotificationChannel(channel.id);
        if (created != null && created.getImportance() == NotificationManager.IMPORTANCE_NONE) {
            Log.w(TAG, "Bildirim kanalı kapalı: " + channel.id);
            return Result.CHANNEL_OFF;
        }

        Intent open = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(context, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel.id)
                .setSmallIcon(R.drawable.crescent)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(channel == Channel.PRAYER
                        ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);
        if (timeoutMillis > 0) {
            builder.setTimeoutAfter(timeoutMillis);
        }
        try {
            manager.notify(id, builder.build());
            return Result.SHOWN;
        } catch (SecurityException e) {
            Log.w(TAG, "Bildirim izni yok", e); // Android 13+: POST_NOTIFICATIONS geri alınmış
            return Result.APP_NOTIFICATIONS_OFF;
        }
    }

    private static void createChannel(Context context, Channel channel) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        // minSdk 27: her sürümde gerekli; tekrar oluşturmak zararsızdır (kullanıcı ayarlarını değiştirmez)
        NotificationChannel nc = new NotificationChannel(channel.id, context.getString(channel.nameRes), channel.importance);
        nc.setDescription(context.getString(channel.descriptionRes));
        nc.enableLights(true);
        nc.enableVibration(true);
        nm.createNotificationChannel(nc);
    }
}
