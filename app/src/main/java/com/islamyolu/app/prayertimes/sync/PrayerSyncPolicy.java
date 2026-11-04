package com.islamyolu.app.prayertimes.sync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.islamyolu.app.prayertimes.PrayerTimesValidator;
import com.islamyolu.app.prayertimes.db.PrayerCityEntity;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Arka plan senkronizasyonunun indirme yapıp yapmayacağına karar verir (§4). Saf Java; birim testlidir.
 * İndirme yalnızca gerektiğinde yapılır: veri yok, şehir değişti, şema değişti veya 60 günden az kaldı.
 */
public final class PrayerSyncPolicy {

    /** Room'daki son gün bugünden bu kadar günden az ileride ise yenilenir. */
    public static final int MIN_REMAINING_DAYS = 60;

    public enum Reason {
        /** İndirmeye gerek yok */
        UP_TO_DATE,
        /** Seçili şehrin Room'da kaydı yok (ilk kurulum veya şehir değişti) */
        NO_DATA,
        /** Kayıt farklı şema sürümüyle yazılmış */
        SCHEMA_CHANGED,
        /** Son gün 60 günden az ileride */
        RUNNING_OUT
    }

    private PrayerSyncPolicy() {
    }

    /**
     * @param city     seçili şehrin prayer_city kaydı (yoksa null)
     * @param lastDate seçili şehrin prayer_day tablosundaki son günü ("yyyy-MM-dd"; yoksa null)
     * @param today    bugün; 60 günlük eşikte bir günlük saat dilimi farkı önemsiz olduğundan UTC tarihi yeterli
     */
    @NonNull
    public static Reason evaluate(@Nullable PrayerCityEntity city, @Nullable String lastDate, @NonNull LocalDate today) {
        if (city == null || lastDate == null) {
            return Reason.NO_DATA;
        }
        if (city.getSchemaVersion() != PrayerTimesValidator.SUPPORTED_VERSION) {
            return Reason.SCHEMA_CHANGED;
        }
        LocalDate last;
        try {
            last = LocalDate.parse(lastDate);
        } catch (DateTimeParseException e) {
            return Reason.NO_DATA; // bozuk kayıt: yeniden indirilir
        }
        return ChronoUnit.DAYS.between(today, last) < MIN_REMAINING_DAYS ? Reason.RUNNING_OUT : Reason.UP_TO_DATE;
    }
}
