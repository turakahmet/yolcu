package com.islamyolu.app.VakitApi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Alarmları yeniden kurma tetikleyicisi: uygulama açılışı, ayar değişimi, cihaz açılışı ve şehrin gece
 * yarısı alarmı buraya gelir. Kurulum WorkManager'a bırakılmaz (pil kısıtlı cihazlarda saatlerce
 * ertelenebiliyordu); goAsync ile hemen arka planda yapılır.
 */
public class DailyPrayerWorkerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("DailyPrayerWorkerReceiver", "Alarmlar yeniden kuruluyor");
        PendingResult result = goAsync();
        PrayerAlarmScheduler.rescheduleAsync(context, result::finish);
    }
}
