package com.islamyolu.app.prayertimes.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Namaz vakti senkronizasyonunun WorkManager planlaması (§4).
 * İş adları mevcut "DailyPrayerWork" (alarm kurulumu) ile çakışmaz.
 */
public final class PrayerTimesSyncScheduler {

    static final String PERIODIC_WORK = "PrayerTimesSyncPeriodic";
    static final String ONE_TIME_WORK = "PrayerTimesSyncOnce";

    private static final long BACKOFF_MINUTES = 15;

    private PrayerTimesSyncScheduler() {
    }

    /**
     * Uygulama açılışında: haftalık periyodik iş (varsa korunur) ve tek seferlik kontrol.
     * Kontrol veri yeterliyse hiçbir şey indirmez; eski sürümden geçen veya ilk kurulumdaki kullanıcıyı kapsar.
     */
    public static void onAppStart(@NonNull Context context) {
        WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, periodicRequest());
        workManager.enqueueUniqueWork(ONE_TIME_WORK, ExistingWorkPolicy.KEEP, oneTimeRequest());
    }

    /** Şehir değiştiğinde: bekleyen tek seferlik iş yeni şehir için yeniden kurulur. */
    public static void onCityChanged(@NonNull Context context) {
        WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
        workManager.enqueueUniqueWork(ONE_TIME_WORK, ExistingWorkPolicy.REPLACE, oneTimeRequest());
        workManager.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, periodicRequest());
    }

    /** Ekranda vakit kalmadığında: veriyi hemen (internet gelince) yenilemeyi dener. */
    public static void requestSyncNow(@NonNull Context context) {
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(ONE_TIME_WORK, ExistingWorkPolicy.REPLACE, oneTimeRequest());
    }

    private static PeriodicWorkRequest periodicRequest() {
        return new PeriodicWorkRequest.Builder(PrayerTimesSyncWorker.class, 7, TimeUnit.DAYS)
                .setConstraints(networkConstraint())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    private static OneTimeWorkRequest oneTimeRequest() {
        return new OneTimeWorkRequest.Builder(PrayerTimesSyncWorker.class)
                .setConstraints(networkConstraint())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    private static Constraints networkConstraint() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
    }
}
