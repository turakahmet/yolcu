package com.islamyolu.app.prayertimes.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;

/**
 * prayer_day: bir şehrin tek günlük vakitleri.
 * Vakitler şehrin yerel saatidir ("HH:mm"); kutup bölgelerinde null olabilir.
 * Kesin an için {@link com.islamyolu.app.prayertimes.PrayerClock} kullanılır.
 */
@Entity(
        tableName = "prayer_day",
        primaryKeys = {"cityId", "date"},
        indices = {@Index("date")}
)
public class PrayerDayEntity {

    private final int cityId;
    /** "yyyy-MM-dd" (projedeki tek tarih temsili; metin olarak sıralanabilir) */
    @NonNull private final String date;
    @Nullable private final String fajr;
    @Nullable private final String sunrise;
    @Nullable private final String dhuhr;
    @Nullable private final String asr;
    @Nullable private final String maghrib;
    @Nullable private final String isha;
    @Nullable private final String hijri;
    @Nullable private final String qibla;
    /** O gün geçerli UTC farkı (dakika) */
    private final int gmtOffsetMinutes;

    public PrayerDayEntity(int cityId, @NonNull String date,
                           @Nullable String fajr, @Nullable String sunrise, @Nullable String dhuhr,
                           @Nullable String asr, @Nullable String maghrib, @Nullable String isha,
                           @Nullable String hijri, @Nullable String qibla, int gmtOffsetMinutes) {
        this.cityId = cityId;
        this.date = date;
        this.fajr = fajr;
        this.sunrise = sunrise;
        this.dhuhr = dhuhr;
        this.asr = asr;
        this.maghrib = maghrib;
        this.isha = isha;
        this.hijri = hijri;
        this.qibla = qibla;
        this.gmtOffsetMinutes = gmtOffsetMinutes;
    }

    public int getCityId() {
        return cityId;
    }

    @NonNull
    public String getDate() {
        return date;
    }

    @Nullable
    public String getFajr() {
        return fajr;
    }

    @Nullable
    public String getSunrise() {
        return sunrise;
    }

    @Nullable
    public String getDhuhr() {
        return dhuhr;
    }

    @Nullable
    public String getAsr() {
        return asr;
    }

    @Nullable
    public String getMaghrib() {
        return maghrib;
    }

    @Nullable
    public String getIsha() {
        return isha;
    }

    @Nullable
    public String getHijri() {
        return hijri;
    }

    @Nullable
    public String getQibla() {
        return qibla;
    }

    public int getGmtOffsetMinutes() {
        return gmtOffsetMinutes;
    }
}
