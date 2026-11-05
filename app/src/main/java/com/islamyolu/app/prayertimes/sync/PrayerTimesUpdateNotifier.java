package com.islamyolu.app.prayertimes.sync;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import com.islamyolu.app.Service.PrayerNotificationService;
import com.islamyolu.app.Utils.AppLogger;
import com.islamyolu.app.VakitApi.PrayerAlarmScheduler;
import com.islamyolu.app.widget.PrayerTimesWidget;

/**
 * Namaz vakitleri Room'a yeni yazıldığında bildirim/alarm ve widget'ları yenilemenin TEK giriş noktası.
 * Senkronizasyon worker'ı ve şehir seçimi yalnızca bunu çağırır; alarm/widget detayını bilmez.
 * <ul>
 *   <li>Alarmlar: {@link PrayerAlarmScheduler} — ezan/bildirim alarmlarını Room'daki vakitlerle yeniden kurar.</li>
 *   <li>Widget: {@link PrayerTimesWidget} — APPWIDGET_UPDATE ile yeniden çizilir.</li>
 *   <li>Kalıcı bildirim: {@link PrayerNotificationService#requestRefresh} — çalışıyorsa yeni veriyle yenilenir.</li>
 * </ul>
 */
public final class PrayerTimesUpdateNotifier {

    private static final String TAG = "PrayerTimesUpdate";

    private PrayerTimesUpdateNotifier() {
    }

    public static void onPrayerTimesUpdated(@NonNull Context context) {
        Context app = context.getApplicationContext();
        rescheduleAlarms(app);
        refreshWidgets(app);
        // Kalıcı bildirim artık saniyede bir değil, değişince yenilenir: yeni veriyi hemen göstersin
        PrayerNotificationService.requestRefresh(app);
        AppLogger.info(TAG, "Vakitler güncellendi; alarmlar, widget'lar ve kalıcı bildirim yenileniyor");
    }

    private static void rescheduleAlarms(Context context) {
        // Arka planda hemen kurulur (WorkManager gibi ertelenmez)
        PrayerAlarmScheduler.rescheduleAsync(context, null);
    }

    private static void refreshWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (manager == null) {
            return;
        }
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, PrayerTimesWidget.class));
        if (ids == null || ids.length == 0) {
            return; // ana ekranda widget yok
        }
        Intent intent = new Intent(context, PrayerTimesWidget.class)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }
}
