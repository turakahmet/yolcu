package com.islamyolu.app.prayertimes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.islamyolu.app.prayertimes.db.PrayerDayEntity;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Room'daki birkaç günlük vakitten "bugün", sıradaki vakit ve kalan süreyi hesaplar (§5). Saf Java.
 * Vakitler şehrin yerel saatidir; kesin an her günün kendi gmt farkıyla bulunur (PrayerClock).
 * Cihazın saat dilimi kullanılmaz. Boş (null) vakitler atlanır.
 */
public final class PrayerTimeline {

    /** Belirli bir günün belirli vakti ve kesin anı. */
    public static final class Moment {
        @NonNull public final PrayerSlot slot;
        @NonNull public final LocalDate date;
        @NonNull public final String time;
        @NonNull public final Instant instant;

        Moment(@NonNull PrayerSlot slot, @NonNull LocalDate date, @NonNull String time, @NonNull Instant instant) {
            this.slot = slot;
            this.date = date;
            this.time = time;
            this.instant = instant;
        }
    }

    private final Map<LocalDate, PrayerDayEntity> byDate = new HashMap<>();
    private final List<Moment> moments = new ArrayList<>();
    private final List<PrayerDayEntity> days;

    private PrayerTimeline(List<PrayerDayEntity> days) {
        this.days = Collections.unmodifiableList(new ArrayList<>(days));
        for (PrayerDayEntity day : days) {
            LocalDate date;
            try {
                date = LocalDate.parse(day.getDate());
            } catch (DateTimeParseException e) {
                continue; // doğrulanmış veride olmaz
            }
            byDate.put(date, day);
            for (PrayerSlot slot : PrayerSlot.values()) {
                String time = slot.timeOf(day);
                Instant instant = time != null ? safeInstant(date, time, day.getGmtOffsetMinutes()) : null;
                if (instant != null) {
                    moments.add(new Moment(slot, date, time, instant));
                }
            }
        }
        Collections.sort(moments, (a, b) -> a.instant.compareTo(b.instant));
    }

    @NonNull
    public static PrayerTimeline of(@NonNull List<PrayerDayEntity> days) {
        return new PrayerTimeline(days);
    }

    public boolean isEmpty() {
        return byDate.isEmpty();
    }

    /** Eldeki günler, verildiği sırayla (Room sorgusu tarihe göre sıralıdır). */
    @NonNull
    public List<PrayerDayEntity> days() {
        return days;
    }

    /** Şehrin yerel bugünü; eldeki günler şu anı kapsamıyorsa null (veri bitmiş/eksik). */
    @Nullable
    public LocalDate cityToday(@NonNull Instant now) {
        return PrayerClock.cityToday(now, date -> {
            PrayerDayEntity day = byDate.get(date);
            return day != null ? day.getGmtOffsetMinutes() : null;
        });
    }

    /** Şehrin bugünkü vakitleri; veri yoksa null. */
    @Nullable
    public PrayerDayEntity today(@NonNull Instant now) {
        LocalDate today = cityToday(now);
        return today != null ? byDate.get(today) : null;
    }

    @Nullable
    public PrayerDayEntity day(@NonNull LocalDate date) {
        return byDate.get(date);
    }

    /** Şu andan sonraki ilk dolu vakit (gece yarısından sonra ertesi günün imsakı dahil); yoksa null. */
    @Nullable
    public Moment next(@NonNull Instant now) {
        for (Moment moment : moments) {
            if (moment.instant.isAfter(now)) {
                return moment;
            }
        }
        return null;
    }

    /** İçinde bulunulan vakit (şu andan önceki son dolu vakit); yoksa null. */
    @Nullable
    public Moment current(@NonNull Instant now) {
        Moment last = null;
        for (Moment moment : moments) {
            if (moment.instant.isAfter(now)) {
                break;
            }
            last = moment;
        }
        return last;
    }

    /** Sıradaki vakte kalan süre; sıradaki vakit yoksa null. */
    @Nullable
    public Duration remaining(@NonNull Instant now) {
        Moment next = next(now);
        return next != null ? Duration.between(now, next.instant) : null;
    }

    /** Belirli günün belirli vaktinin kesin anı; vakit boşsa veya gün yoksa null. */
    @Nullable
    public Instant instantOf(@NonNull LocalDate date, @NonNull PrayerSlot slot) {
        PrayerDayEntity day = byDate.get(date);
        if (day == null) return null;
        String time = slot.timeOf(day);
        return time != null ? safeInstant(date, time, day.getGmtOffsetMinutes()) : null;
    }

    @Nullable
    private static Instant safeInstant(LocalDate date, String time, int offsetMinutes) {
        try {
            return PrayerClock.toInstant(date, time, offsetMinutes);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
