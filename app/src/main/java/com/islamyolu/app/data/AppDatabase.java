package com.islamyolu.app.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.islamyolu.app.prayertimes.db.PrayerCityEntity;
import com.islamyolu.app.prayertimes.db.PrayerDayEntity;
import com.islamyolu.app.prayertimes.db.PrayerTimesDao;

/**
 * Uygulamanın tek Room veritabanı. Örnek {@link com.islamyolu.app.IslamYoluApplication}
 * tarafından bir kez oluşturulur.
 * Şema değişikliğinde sürüm artırılır, Migration yazılır ve app/schemas altındaki JSON
 * sürüm kontrolüne eklenir. Destructive migration ve ana thread sorgusu kullanılmaz.
 */
@Database(
        entities = {PrayerDayEntity.class, PrayerCityEntity.class},
        version = 1,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "islamyolu.db";

    public abstract PrayerTimesDao prayerTimesDao();

    /** Yalnızca uygulama sınıfı çağırır; ekranlar kendi örneğini oluşturmaz. */
    @NonNull
    public static AppDatabase create(@NonNull Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DATABASE_NAME)
                .build();
    }
}
