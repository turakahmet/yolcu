package com.islamyolu.app.prayertimes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * Vakitlerin kesin anını ve şehrin yerel "bugün" tarihini hesaplar.
 * Cihazın saat dilimi hiç kullanılmaz; her gün Diyanet verisindeki kendi gmt farkıyla çözülür,
 * böylece yaz saati geçişleri ve yurt dışı şehirler doğru işlenir.
 */
public final class PrayerClock {

    /** Bir tarihin gmt farkını (dakika) verir; o gün için veri yoksa null döner. */
    public interface OffsetLookup {
        @Nullable
        Integer offsetMinutesFor(@NonNull LocalDate date);
    }

    private PrayerClock() {
    }

    @NonNull
    public static ZoneOffset toZoneOffset(int gmtOffsetMinutes) {
        return ZoneOffset.ofTotalSeconds(gmtOffsetMinutes * 60);
    }

    /**
     * tarih + "HH:mm" + o günün gmt farkı → kesin an.
     *
     * @return vakit null ise (ör. kutup bölgesi) null
     * @throws DateTimeParseException vakit "HH:mm" değilse (doğrulanmış veride oluşmaz)
     */
    @Nullable
    public static Instant toInstant(@NonNull LocalDate date, @Nullable String time, int gmtOffsetMinutes) {
        if (time == null) {
            return null;
        }
        return LocalDateTime.of(date, LocalTime.parse(time)).toInstant(toZoneOffset(gmtOffsetMinutes));
    }

    /**
     * Şehrin şu anki yerel tarihi.
     * Bir gün, önceki günün farkıyla gece yarısında başlar ve kendi farkıyla ertesi gece yarısında
     * biter; geçiş günleri (23/25 saatlik günler) böylece boşluksuz ve çakışmasız kalır.
     *
     * @return yakın günler için veri yoksa null
     */
    @Nullable
    public static LocalDate cityToday(@NonNull Instant now, @NonNull OffsetLookup lookup) {
        LocalDate utcDate = now.atOffset(ZoneOffset.UTC).toLocalDate();
        // UTC farkı en fazla ±18 saat olduğundan yerel tarih UTC tarihinin bir gün öncesi/sonrası olabilir
        for (int shift = 1; shift >= -1; shift--) {
            LocalDate candidate = utcDate.plusDays(shift);
            Integer offset = lookup.offsetMinutesFor(candidate);
            if (offset == null) {
                continue;
            }
            Integer previousOffset = lookup.offsetMinutesFor(candidate.minusDays(1));
            int startOffset = previousOffset != null ? previousOffset : offset;

            Instant start = candidate.atStartOfDay().toInstant(toZoneOffset(startOffset));
            Instant end = candidate.plusDays(1).atStartOfDay().toInstant(toZoneOffset(offset));
            if (!now.isBefore(start) && now.isBefore(end)) {
                return candidate;
            }
        }
        return null;
    }
}
