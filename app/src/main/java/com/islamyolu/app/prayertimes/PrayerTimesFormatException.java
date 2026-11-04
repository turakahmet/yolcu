package com.islamyolu.app.prayertimes;

/**
 * times/{cityId}.json dosyası beklenen biçimde değilse fırlatılır
 * (geçersiz JSON, eksik zorunlu alan, yanlış tip). Dosya bütünüyle reddedilir.
 */
public class PrayerTimesFormatException extends Exception {

    public PrayerTimesFormatException(String message) {
        super(message);
    }

    public PrayerTimesFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
