package com.islamyolu.app.prayertimes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * times/{cityId}.json içindeki tek bir günün satırı.
 * Vakitler şehrin yerel saatidir ("HH:mm"); kutup bölgelerinde bir vakit null olabilir.
 * Tarih projede tek temsil olarak "yyyy-MM-dd" String tutulur.
 */
public final class PrayerDayDto {

    private final String date;
    @Nullable private final String fajr;
    @Nullable private final String sunrise;
    @Nullable private final String dhuhr;
    @Nullable private final String asr;
    @Nullable private final String maghrib;
    @Nullable private final String isha;
    @Nullable private final String hijri;
    @Nullable private final String qibla;
    private final int gmtOffsetMinutes;

    public PrayerDayDto(@NonNull String date,
                        @Nullable String fajr, @Nullable String sunrise, @Nullable String dhuhr,
                        @Nullable String asr, @Nullable String maghrib, @Nullable String isha,
                        @Nullable String hijri, @Nullable String qibla, int gmtOffsetMinutes) {
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

    @NonNull
    public String getDate() {
        return date;
    }

    /** İmsak */
    @Nullable
    public String getFajr() {
        return fajr;
    }

    /** Güneş */
    @Nullable
    public String getSunrise() {
        return sunrise;
    }

    /** Öğle */
    @Nullable
    public String getDhuhr() {
        return dhuhr;
    }

    /** İkindi */
    @Nullable
    public String getAsr() {
        return asr;
    }

    /** Akşam */
    @Nullable
    public String getMaghrib() {
        return maghrib;
    }

    /** Yatsı */
    @Nullable
    public String getIsha() {
        return isha;
    }

    /** Hicri tarih "gün.ay.yıl" */
    @Nullable
    public String getHijri() {
        return hijri;
    }

    /** Kıble saati */
    @Nullable
    public String getQibla() {
        return qibla;
    }

    /** O gün o şehirde geçerli UTC farkı (dakika). Yaz saatinde değişir, kesirli olabilir. */
    public int getGmtOffsetMinutes() {
        return gmtOffsetMinutes;
    }

    /** Günün altı vakti sırasıyla: İmsak, Güneş, Öğle, İkindi, Akşam, Yatsı. */
    @NonNull
    public String[] getPrayerTimes() {
        return new String[]{fajr, sunrise, dhuhr, asr, maghrib, isha};
    }
}
