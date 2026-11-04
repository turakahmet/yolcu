package com.islamyolu.app.prayertimes;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Bir şehrin vakit senkronizasyonunun sonucu. */
public final class PrayerSyncResult {

    public enum Status {
        /** Yeni veri doğrulanıp Room'a yazıldı */
        UPDATED,
        /** Sunucu 304 döndü; yalnızca senkron zamanı güncellendi */
        NOT_MODIFIED,
        /** times/{cityId}.json yok (404): Diyanet bu şehir için vakit yayımlamıyor */
        CITY_HAS_NO_DATA,
        /** Bağlantı/zaman aşımı */
        NETWORK_ERROR,
        /** Sunucu hatası (429, 5xx veya beklenmeyen kod) */
        SERVER_ERROR,
        /** Dosya ayrıştırılamadı veya doğrulamadan geçmedi */
        INVALID_DATA,
        /** Room'a yazılamadı (işlem geri alındı) */
        STORAGE_ERROR
    }

    public static final int NO_HTTP_CODE = -1;

    private final Status status;
    private final int cityId;
    private final int httpCode;
    private final Set<PrayerTimesValidator.Error> validationErrors;

    private PrayerSyncResult(Status status, int cityId, int httpCode,
                             Set<PrayerTimesValidator.Error> validationErrors) {
        this.status = status;
        this.cityId = cityId;
        this.httpCode = httpCode;
        this.validationErrors = Collections.unmodifiableSet(validationErrors);
    }

    static PrayerSyncResult of(Status status, int cityId, int httpCode) {
        return new PrayerSyncResult(status, cityId, httpCode, EnumSet.noneOf(PrayerTimesValidator.Error.class));
    }

    static PrayerSyncResult invalid(int cityId, Set<PrayerTimesValidator.Error> errors) {
        Set<PrayerTimesValidator.Error> copy = EnumSet.noneOf(PrayerTimesValidator.Error.class);
        copy.addAll(errors);
        return new PrayerSyncResult(Status.INVALID_DATA, cityId, NO_HTTP_CODE, copy);
    }

    @NonNull
    public Status getStatus() {
        return status;
    }

    public int getCityId() {
        return cityId;
    }

    /** HTTP yanıt kodu; ağ hatasında {@link #NO_HTTP_CODE}. */
    public int getHttpCode() {
        return httpCode;
    }

    @NonNull
    public Set<PrayerTimesValidator.Error> getValidationErrors() {
        return validationErrors;
    }

    /** Room'daki veri güncel (yeni yazıldı veya değişmemiş). */
    public boolean isSuccess() {
        return status == Status.UPDATED || status == Status.NOT_MODIFIED;
    }

    /** Geçici hata: arka planda yeniden denenmeli (WorkManager Result.retry()). */
    public boolean isRetryable() {
        switch (status) {
            case NETWORK_ERROR:
            case STORAGE_ERROR:
                return true;
            case SERVER_ERROR:
                return httpCode == 429 || httpCode >= 500;
            default:
                return false;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "PrayerSyncResult{" + status + ", cityId=" + cityId + ", http=" + httpCode
                + (validationErrors.isEmpty() ? "" : ", errors=" + validationErrors) + "}";
    }
}
