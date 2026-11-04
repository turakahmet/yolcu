package com.islamyolu.app.prayertimes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.islamyolu.app.prayertimes.db.PrayerDayEntity;

import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

/** §5: bugün, sıradaki vakit ve geri sayım şehrin gmt farkıyla; null vakitler atlanır. */
public class PrayerTimelineTest {

    // Adana, gmt +3
    private static final PrayerTimeline ADANA = PrayerTimeline.of(Arrays.asList(
            day("2026-11-03", "05:35", "06:56", "12:27", "15:22", "17:48", "19:04", 180),
            day("2026-11-04", "05:37", "06:57", "12:27", "15:21", "17:45", "19:03", 180),
            day("2026-11-05", "05:38", "06:58", "12:27", "15:21", "17:44", "19:02", 180)));

    @Test
    public void gunIcindeSiradakiVakit() {
        // 4 Kasım 13:00 yerel = 10:00Z → sıradaki İkindi 15:21
        Instant now = Instant.parse("2026-11-04T10:00:00Z");
        PrayerTimeline.Moment next = ADANA.next(now);
        assertNotNull(next);
        assertEquals(PrayerSlot.IKINDI, next.slot);
        assertEquals("15:21", next.time);
        assertEquals(PrayerSlot.OGLE, ADANA.current(now).slot);
        assertEquals(Duration.ofMinutes(141), ADANA.remaining(now));
        assertEquals(LocalDate.of(2026, 11, 4), ADANA.cityToday(now));
    }

    @Test
    public void yatsidanSonraErtesiGununImsaki() {
        // 4 Kasım 23:30 yerel = 20:30Z → sıradaki 5 Kasım İmsak 05:38
        PrayerTimeline.Moment next = ADANA.next(Instant.parse("2026-11-04T20:30:00Z"));
        assertEquals(PrayerSlot.IMSAK, next.slot);
        assertEquals(LocalDate.of(2026, 11, 5), next.date);
    }

    @Test
    public void geceYarisindanSonraBugunYeniGundur() {
        // 5 Kasım 00:30 yerel = 4 Kasım 21:30Z
        Instant now = Instant.parse("2026-11-04T21:30:00Z");
        assertEquals(LocalDate.of(2026, 11, 5), ADANA.cityToday(now));
        assertEquals("05:38", ADANA.today(now).getFajr());
        assertEquals(PrayerSlot.YATSI, ADANA.current(now).slot);
    }

    @Test
    public void nullVakitAtlanir() {
        // Kutup bölgesi: yatsı ve imsak yok
        PrayerTimeline polar = PrayerTimeline.of(Arrays.asList(
                day("2026-06-21", null, "01:30", "12:45", "17:30", "23:50", null, 120),
                day("2026-06-22", null, "01:31", "12:45", "17:30", "23:50", null, 120)));
        // 21 Haziran 23:55 yerel → sıradaki 22 Haziran Güneş 01:31
        PrayerTimeline.Moment next = polar.next(Instant.parse("2026-06-21T21:55:00Z"));
        assertEquals(PrayerSlot.GUNES, next.slot);
        assertEquals(LocalDate.of(2026, 6, 22), next.date);
        assertNull(polar.instantOf(LocalDate.of(2026, 6, 21), PrayerSlot.YATSI));
    }

    @Test
    public void veriBittiyseBugunYok() {
        Instant later = Instant.parse("2026-11-10T10:00:00Z");
        assertNull(ADANA.cityToday(later));
        assertNull(ADANA.today(later));
        assertNull(ADANA.next(later));
        assertNull(PrayerTimeline.of(Collections.emptyList()).next(later));
    }

    private static PrayerDayEntity day(String date, String fajr, String sunrise, String dhuhr, String asr,
                                       String maghrib, String isha, int gmtMinutes) {
        return new PrayerDayEntity(9146, date, fajr, sunrise, dhuhr, asr, maghrib, isha, "21.5.1448", "11:32", gmtMinutes);
    }
}
