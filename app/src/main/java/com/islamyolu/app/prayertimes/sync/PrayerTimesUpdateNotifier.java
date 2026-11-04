package com.islamyolu.app.prayertimes.sync;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.islamyolu.app.Utils.AppLogger;
import com.islamyolu.app.VakitApi.DailyPrayerWorker;
import com.islamyolu.app.widget.PrayerTimesWidget;

/**
 * Namaz vakitleri Room'a yeni yazıldığında bildirim/alarm ve widget'ları yenilemenin TEK giriş noktası.
 * Senkronizasyon worker'ı ve şehir seçimi yalnızca bunu çağırır; alarm/widget detayını bilmez.
 * <p>
 * Şu an bağlı olduğu yerler (geçiş dönemi):
 * <ul>
 *   <li>Alarmlar: {@link DailyPrayerWorker} — ezan/bildirim alarmlarını yeniden kurar. Henüz vakitleri
 *       Room'dan değil, eski "city_name" + adhan2 hesabından alır.</li>
 *   <li>Widget: {@link PrayerTimesWidget} — APPWIDGET_UPDATE ile yeniden çizilir. Henüz o da adhan2 ile hesaplar.</li>
 *   <li>Kalıcı bildirim servisi kendi döngüsüyle saniyede bir güncellendiği için ayrıca tetiklenmez.</li>
 * </ul>
 * Okuma tarafı Room'a geçtiğinde yalnızca bu sınıfın içi değişecek; çağıranlar aynı kalır.
 */
public final class PrayerTimesUpdateNotifier {

    private static final String TAG = "PrayerTimesUpdate";

    private PrayerTimesUpdateNotifier() {
    }

    public static void onPrayerTimesUpdated(@NonNull Context context) {
        Context app = context.getApplicationContext();
        rescheduleAlarms(app);
        refreshWidgets(app);
        AppLogger.info(TAG, "Vakitler güncellendi; alarmlar ve widget'lar yenileniyor");
    }

    private static void rescheduleAlarms(Context context) {
        // DailyPrayerWorkerReceiver ile aynı tetikleme: worker alarmları kurar ve gece yarısı için kendini planlar
        WorkManager.getInstance(context).enqueue(new OneTimeWorkRequest.Builder(DailyPrayerWorker.class).build());
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
