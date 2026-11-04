package com.islamyolu.app.prayertimes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.islamyolu.app.prayertimes.db.PrayerDayEntity;

/** Günün altı vakti, gün içindeki sırasıyla. */
public enum PrayerSlot {
    IMSAK,
    GUNES,
    OGLE,
    IKINDI,
    AKSAM,
    YATSI;

    /** Bu vaktin o günkü "HH:mm" değeri; kutup bölgelerinde null olabilir. */
    @Nullable
    public String timeOf(@NonNull PrayerDayEntity day) {
        switch (this) {
            case IMSAK:
                return day.getFajr();
            case GUNES:
                return day.getSunrise();
            case OGLE:
                return day.getDhuhr();
            case IKINDI:
                return day.getAsr();
            case AKSAM:
                return day.getMaghrib();
            default:
                return day.getIsha();
        }
    }
}
