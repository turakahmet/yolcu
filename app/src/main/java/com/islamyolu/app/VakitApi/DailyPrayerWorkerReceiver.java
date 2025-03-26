package com.islamyolu.app.VakitApi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class DailyPrayerWorkerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("DailyPrayerWorkerReceiver", "Alarm tetiklendi, DailyPrayerWorker çalıştırılıyor");

        // DailyPrayerWorker'i manuel olarak başlat
        WorkManager.getInstance(context).enqueue(new OneTimeWorkRequest.Builder(DailyPrayerWorker.class).build());
    }
}
