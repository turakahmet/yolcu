package com.islamyolu.app;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.islamyolu.app.data.AppDatabase;
import com.islamyolu.app.prayertimes.PrayerTimesParser;
import com.islamyolu.app.prayertimes.PrayerTimesRepository;
import com.islamyolu.app.prayertimes.PrayerTimesValidator;
import com.islamyolu.app.prayertimes.remote.PrayerTimesApiClient;

import java.time.Clock;

/**
 * Uygulama düzeyinde paylaşılan nesneler (gereksiz singleton yerine tek örnek burada tutulur).
 * Veritabanı ilk ihtiyaç anında oluşturulur; açılışta disk işlemi yapılmaz.
 */
public class IslamYoluApplication extends Application {

    private volatile AppDatabase database;
    private volatile PrayerTimesRepository prayerTimesRepository;

    @NonNull
    public static IslamYoluApplication from(@NonNull Context context) {
        return (IslamYoluApplication) context.getApplicationContext();
    }

    @NonNull
    public AppDatabase getDatabase() {
        AppDatabase result = database;
        if (result == null) {
            synchronized (this) {
                result = database;
                if (result == null) {
                    result = AppDatabase.create(this);
                    database = result;
                }
            }
        }
        return result;
    }

    @NonNull
    public PrayerTimesRepository getPrayerTimesRepository() {
        PrayerTimesRepository result = prayerTimesRepository;
        if (result == null) {
            synchronized (this) {
                result = prayerTimesRepository;
                if (result == null) {
                    result = new PrayerTimesRepository(
                            PrayerTimesApiClient.create(),
                            getDatabase().prayerTimesDao(),
                            new PrayerTimesParser(),
                            new PrayerTimesValidator(),
                            Clock.systemUTC());
                    prayerTimesRepository = result;
                }
            }
        }
        return result;
    }
}
