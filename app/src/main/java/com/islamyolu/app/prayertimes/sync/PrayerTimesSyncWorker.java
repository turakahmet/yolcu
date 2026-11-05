package com.islamyolu.app.prayertimes.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.islamyolu.app.IslamYoluApplication;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.Utils.AppLogger;
import com.islamyolu.app.main.MainActivity;
import com.islamyolu.app.prayertimes.PrayerCitySelection;
import com.islamyolu.app.prayertimes.PrayerSyncResult;
import com.islamyolu.app.prayertimes.PrayerTimesRepository;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Seçili şehrin vakitlerini arka planda gerektiğinde yeniler (§4). Diyanet'e değil, GitHub Pages'e bağlanır.
 * Sonuçlar: başarılı/gerek yok → success; 429/5xx/ağ/disk hatası → retry (üstel backoff);
 * 404 (şehrin verisi yok) ve bozuk dosya → failure. Hiçbir hatada mevcut Room verisi silinmez.
 */
public class PrayerTimesSyncWorker extends Worker {

    private static final String TAG = "PrayerTimesSyncWorker";

    public PrayerTimesSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        PrayerCitySelection selection =
                AppPreferences.from(context).location().getPrayerCitySelection();
        if (selection == null) {
            return Result.success(); // henüz şehir seçilmedi; seçim ekranı indirmeyi kendisi yapar
        }
        int cityId = selection.getCityId();
        PrayerTimesRepository repository = IslamYoluApplication.from(context).getPrayerTimesRepository();

        PrayerSyncPolicy.Reason need;
        try {
            need = repository.syncNeed(cityId, LocalDate.now(ZoneOffset.UTC));
        } catch (RuntimeException e) {
            AppLogger.error(TAG, "Senkron ihtiyacı okunamadı, cityId=" + cityId, e);
            return Result.retry();
        }
        if (need == PrayerSyncPolicy.Reason.UP_TO_DATE) {
            return Result.success();
        }

        AppLogger.info(TAG, "İndirme başlıyor, cityId=" + cityId + " neden=" + need);
        PrayerSyncResult result = repository.syncCity(cityId);
        switch (result.getStatus()) {
            case UPDATED:
                PrayerTimesUpdateNotifier.onPrayerTimesUpdated(context);
                return Result.success();
            case NOT_MODIFIED:
                return Result.success();
            case CITY_HAS_NO_DATA:
                // Diyanet bu şehir için yayımlamıyor; tekrar denemek anlamsız
                return Result.failure();
            default:
                // Bozuk dosya tekrar denenmez; periyodik iş bir sonraki çalışmada yeniden bakar
                return result.isRetryable() ? Result.retry() : Result.failure();
        }
    }
}
