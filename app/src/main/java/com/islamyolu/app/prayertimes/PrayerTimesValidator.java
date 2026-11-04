package com.islamyolu.app.prayertimes;

import androidx.annotation.NonNull;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * İndirilen vakit dosyasını Room'a yazmadan önce bütünüyle doğrular.
 * Tek bir hata bile varsa dosya yazılmaz; mevcut Room verisine dokunulmaz.
 */
public final class PrayerTimesValidator {

    public static final int SUPPORTED_VERSION = 2;
    public static final int MIN_DAYS = 60;

    /** java.time.ZoneOffset sınırı: ±18 saat */
    private static final int MAX_OFFSET_MINUTES = 18 * 60;
    private static final Pattern TIME_PATTERN = Pattern.compile("([01]\\d|2[0-3]):[0-5]\\d");

    public enum Error {
        UNSUPPORTED_VERSION,
        CITY_ID_MISMATCH,
        NO_DAYS,
        TOO_FEW_DAYS,
        INVALID_DATE,
        DATES_NOT_CONSECUTIVE,
        INVALID_TIME,
        INVALID_GMT_OFFSET,
        INVALID_VALIDITY,
        EXPIRED
    }

    public static final class Result {
        private final Set<Error> errors;

        Result(Set<Error> errors) {
            this.errors = Collections.unmodifiableSet(errors);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        @NonNull
        public Set<Error> getErrors() {
            return errors;
        }

        @NonNull
        @Override
        public String toString() {
            return isValid() ? "Geçerli" : "Geçersiz: " + errors;
        }
    }

    /**
     * @param expectedCityId istenen şehir
     * @param today          şehrin yerel "bugün" tarihi (bkz. {@link PrayerClock#cityToday})
     */
    @NonNull
    public Result validate(@NonNull CityTimesDto file, int expectedCityId, @NonNull LocalDate today) {
        Set<Error> errors = EnumSet.noneOf(Error.class);

        if (file.getVersion() != SUPPORTED_VERSION) {
            errors.add(Error.UNSUPPORTED_VERSION);
        }
        if (file.getCityId() != expectedCityId) {
            errors.add(Error.CITY_ID_MISMATCH);
        }

        validateDays(file, errors);

        LocalDate validTo = parseDate(file.getValidTo());
        if (validTo == null || parseDate(file.getValidFrom()) == null) {
            errors.add(Error.INVALID_VALIDITY);
        } else if (!validTo.isAfter(today)) {
            errors.add(Error.EXPIRED);
        }

        return new Result(errors);
    }

    private void validateDays(CityTimesDto file, Set<Error> errors) {
        if (file.getDays().isEmpty()) {
            errors.add(Error.NO_DAYS);
            return;
        }
        if (file.getDays().size() < MIN_DAYS) {
            errors.add(Error.TOO_FEW_DAYS);
        }

        LocalDate previous = null;
        for (PrayerDayDto day : file.getDays()) {
            LocalDate date = parseDate(day.getDate());
            if (date == null) {
                errors.add(Error.INVALID_DATE);
            } else {
                // Artan sıra dışı, tekrar eden veya boşluklu tarihler aynı hatayı verir
                if (previous != null && !date.equals(previous.plusDays(1))) {
                    errors.add(Error.DATES_NOT_CONSECUTIVE);
                }
                previous = date;
            }

            for (String time : day.getPrayerTimes()) {
                if (time != null && !TIME_PATTERN.matcher(time).matches()) {
                    errors.add(Error.INVALID_TIME);
                }
            }

            if (Math.abs(day.getGmtOffsetMinutes()) > MAX_OFFSET_MINUTES) {
                errors.add(Error.INVALID_GMT_OFFSET);
            }
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            // Hata kodu olarak çağırana bildirilir (INVALID_DATE / INVALID_VALIDITY)
            return null;
        }
    }
}
