package com.islamyolu.app.VakitApi;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.islamyolu.app.widget.PrayerTimesWidget;

/**
 * Yedek katman: birkaç saatte bir alarmları yeniden kurar (asıl zincir {@link PrayerAlarmScheduler}
 * içindeki gece yarısı alarmıdır). Kurulum idempotent olduğu için sık çalışması zararsızdır.
 */
public class DailyPrayerWorker extends Worker {
    private static final String TAG = "DailyPrayerWorker";

    public DailyPrayerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        try {
            PrayerAlarmScheduler.reschedule(context);
        } catch (RuntimeException e) {
            Log.e(TAG, "Alarmlar yeniden kurulamadı", e);
            return Result.retry();
        }
        if (isWidgetActive(context)) {
            PrayerTimesWidget widgetProvider = new PrayerTimesWidget();
            widgetProvider.scheduleNextUpdate(context);
            widgetProvider.scheduleWeatherUpdate(context);
        }
        return Result.success();
    }

    private static boolean isWidgetActive(Context context) {
        int[] ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, PrayerTimesWidget.class));
        return ids != null && ids.length > 0;
    }
}
