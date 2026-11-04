package com.islamyolu.app.prayertimes.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Upsert;

import java.time.LocalDate;
import java.util.List;

/**
 * Namaz vakitleri tablolarına erişim. Yalnızca repository katmanı kullanır;
 * UI, widget ve bildirimler doğrudan DAO'ya bağlanmaz. Sorgular ana thread'de çağrılmaz.
 * Tarih parametreleri "yyyy-MM-dd" biçimindedir (metin karşılaştırması tarih sırasıyla aynıdır).
 */
@Dao
public abstract class PrayerTimesDao {

    /** Bugünden geriye saklanan gün sayısı; daha eski satırlar silinir. */
    public static final int KEEP_PAST_DAYS = 2;

    // --- Yazma ---

    /**
     * Doğrulanmış şehir verisini tek işlemde yazar (idempotent):
     * günleri upsert eder, meta bilgiyi günceller, 2 günden eski günleri siler ve
     * en son diğer şehirlerin verisini siler. Yeni veri yazılmadan eski şehir silinmez;
     * herhangi bir adım hata verirse tüm işlem geri alınır ve mevcut veri korunur.
     *
     * @param cityToday şehrin yerel bugünü (bkz. PrayerClock#cityToday)
     */
    @Transaction
    public void replaceCityData(@NonNull PrayerCityEntity city,
                                @NonNull List<PrayerDayEntity> days,
                                @NonNull LocalDate cityToday) {
        int cityId = city.getCityId();
        for (PrayerDayEntity day : days) {
            if (day.getCityId() != cityId) {
                throw new IllegalArgumentException("Gün başka şehre ait: " + day.getCityId());
            }
        }
        upsertDays(days);
        upsertCity(city);
        deleteDaysBefore(cityId, cityToday.minusDays(KEEP_PAST_DAYS).toString());
        deleteDaysOfOtherCities(cityId);
        deleteOtherCities(cityId);
    }

    /** 304 (Not Modified) yanıtında yalnızca senkron zamanı güncellenir. */
    @Query("UPDATE prayer_city SET syncedAt = :syncedAt WHERE cityId = :cityId")
    public abstract int updateSyncedAt(int cityId, long syncedAt);

    @Upsert
    protected abstract void upsertDays(List<PrayerDayEntity> days);

    @Upsert
    protected abstract void upsertCity(PrayerCityEntity city);

    @Query("DELETE FROM prayer_day WHERE cityId = :cityId AND date < :oldestDateToKeep")
    protected abstract void deleteDaysBefore(int cityId, String oldestDateToKeep);

    @Query("DELETE FROM prayer_day WHERE cityId != :cityId")
    protected abstract void deleteDaysOfOtherCities(int cityId);

    @Query("DELETE FROM prayer_city WHERE cityId != :cityId")
    protected abstract void deleteOtherCities(int cityId);

    // --- Okuma ---

    @Nullable
    @Query("SELECT * FROM prayer_city WHERE cityId = :cityId")
    public abstract PrayerCityEntity getCity(int cityId);

    @Nullable
    @Query("SELECT * FROM prayer_day WHERE cityId = :cityId AND date = :date")
    public abstract PrayerDayEntity getDay(int cityId, String date);

    /** Ekranlar için: aralıktaki günler; Room'a yeni veri yazıldığında kendiliğinden güncellenir. */
    @Query("SELECT * FROM prayer_day WHERE cityId = :cityId AND date BETWEEN :fromDate AND :toDate ORDER BY date")
    public abstract LiveData<List<PrayerDayEntity>> observeDays(int cityId, String fromDate, String toDate);

    /** [fromDate, toDate] aralığındaki günler, tarih sırasıyla. */
    @Query("SELECT * FROM prayer_day WHERE cityId = :cityId AND date BETWEEN :fromDate AND :toDate ORDER BY date")
    public abstract List<PrayerDayEntity> getDays(int cityId, String fromDate, String toDate);

    /** Şehrin Room'daki son günü; veri yoksa null (senkron ihtiyacı için). */
    @Nullable
    @Query("SELECT MAX(date) FROM prayer_day WHERE cityId = :cityId")
    public abstract String getLastDate(int cityId);
}
