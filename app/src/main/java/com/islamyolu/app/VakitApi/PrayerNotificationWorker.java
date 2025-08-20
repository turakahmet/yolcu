package com.islamyolu.app.VakitApi;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.islamyolu.app.Service.PrayerNotificationService;

public class PrayerNotificationWorker extends Worker {
    public PrayerNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Intent serviceIntent = new Intent(context, PrayerNotificationService.class);

        context.startForegroundService(serviceIntent);
        return Result.success();
    }
}

