package com.islamyolu.app.prayertimes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.islamyolu.app.prayertimes.db.PrayerDayEntity;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;

/** Geri sayım: hiçbir zaman eksi değil, yatsıdan sonra ertesi günün imsakı, vakit anında sonrakine geçiş. */
public class PrayerCountdownTest {

    // Adana, gmt +3
    private static final PrayerTimeline ADANA = PrayerTimeline.of(Arrays.asList(
            day("2026-11-04", "05:37", "06:57", "12:27", "15:21", "17:45", "19:03"),
            day("2026-11-05", "05:38", "06:58", "12:27", "15:21", "17:44", "19:02")));

    @Test
    public void aksamaKalanSure() {
        // 4 Kasım 14:24:49 yerel = 11:24:49Z → sıradaki İkindi 15:21
        PrayerCountdown c = PrayerCountdown.of(ADANA, Instant.parse("2026-11-04T11:24:49Z"));
        assertNotNull(c);
        assertEquals(PrayerSlot.IKINDI, c.next.slot);
        assertEquals("00:56:11", c.text());

        // 4 Kasım 15:24:49 yerel (İkindi'den sonra) → Akşam 17:45'e 02:20:11
        PrayerCountdown aksam = PrayerCountdown.of(ADANA, Instant.parse("2026-11-04T12:24:49Z"));
        assertEquals(PrayerSlot.AKSAM, aksam.next.slot);
        assertEquals("02:20:11", aksam.text());
        assertEquals("02:20", aksam.textWithoutSeconds());
    }

    @Test
    public void yatsidanSonraErtesiGununImsakinaSayar() {
        // 4 Kasım 23:00:00 yerel = 20:00Z → 5 Kasım İmsak 05:38 → 06:38:00
        PrayerCountdown c = PrayerCountdown.of(ADANA, Instant.parse("2026-11-04T20:00:00Z"));
        assertEquals(PrayerSlot.IMSAK, c.next.slot);
        assertEquals(LocalDate.of(2026, 11, 5), c.next.date);
        assertEquals("06:38:00", c.text());
    }

    @Test
    public void geceYarisiniGecinceDeAyniImsakaSayar() {
        // 5 Kasım 00:30 yerel = 4 Kasım 21:30Z → İmsak 05:38 → 05:08:00
        PrayerCountdown c = PrayerCountdown.of(ADANA, Instant.parse("2026-11-04T21:30:00Z"));
        assertEquals(PrayerSlot.IMSAK, c.next.slot);
        assertEquals("05:08:00", c.text());
    }

    @Test
    public void vakitAnindaSonrakiVakteGecerVeEksiOlmaz() {
        // Yatsı tam 19:03:00 yerel = 16:03:00Z
        Instant yatsi = Instant.parse("2026-11-04T16:03:00Z");
        PrayerCountdown before = PrayerCountdown.of(ADANA, yatsi.minusMillis(400));
        assertEquals(PrayerSlot.YATSI, before.next.slot);
        assertEquals("00:00:00", before.text());
        assertEquals("00:00", before.textWithoutSeconds());
        assertEquals(400, before.millisUntilTick);

        PrayerCountdown at = PrayerCountdown.of(ADANA, yatsi);
        assertEquals(PrayerSlot.IMSAK, at.next.slot);
        assertTrue(at.remainingSeconds > 0);
        assertEquals("10:35:00", at.text());
    }

    @Test
    public void saniyeAsagiYuvarlanir() {
        // İkindi 15:21:00 yerel; 1,5 sn kala "00:00:01", 500 ms sonra "00:00:00"
        PrayerCountdown c = PrayerCountdown.of(ADANA, Instant.parse("2026-11-04T12:20:58.500Z"));
        assertEquals("00:00:01", c.text());
        // Widget için sonraki dakika sınırı vakit anının kendisi (1,5 sn sonra)
        assertEquals(1500, c.millisUntilMinuteTick());
        assertEquals(500, c.millisUntilTick);
    }

    @Test
    public void veriBittiyseNull() {
        assertNull(PrayerCountdown.of(ADANA, Instant.parse("2026-11-05T17:00:00Z")));
    }

    @Test
    public void bicimCiftHaneli() {
        assertEquals("00:00:00", PrayerCountdown.format(-5));
        assertEquals("00:59:59", PrayerCountdown.format(3599));
        assertEquals("01:00:00", PrayerCountdown.format(3600));
        assertEquals("03:20:11", PrayerCountdown.format(3 * 3600 + 20 * 60 + 11));
        assertEquals("12:05:09", PrayerCountdown.format(12 * 3600 + 5 * 60 + 9));
        // Widget: saniyesiz, aşağı yuvarlanır
        assertEquals("03:20", PrayerCountdown.formatWithoutSeconds(3 * 3600 + 20 * 60 + 59));
        assertEquals("00:00", PrayerCountdown.formatWithoutSeconds(59));
    }

    @Test
    public void widgetDakikaSinirindaYenilenir() {
        // Akşam 17:45:00 yerel; 17:42:30.250 → kalan 2 dk 29,75 sn → "00:02", 29,75 sn sonra "00:01"
        PrayerCountdown c = PrayerCountdown.of(ADANA, Instant.parse("2026-11-04T14:42:30.250Z"));
        assertEquals("00:02", c.textWithoutSeconds());
        assertEquals(29_750, c.millisUntilMinuteTick());
    }

    @Test
    public void bildirimSaatVeKronometreParcasi() {
        // 15:24:49 yerel → Akşam'a 02:20:11 → "02:" + kronometre 20:11
        PrayerCountdown c = PrayerCountdown.of(ADANA, Instant.parse("2026-11-04T12:24:49Z"));
        assertEquals(2, c.hours());
        assertEquals("02:", c.hoursPrefix());
        assertEquals((20 * 60 + 11) * 1000L, c.millisWithinHour());

        // Bir saatten az: "00:" + kronometre
        PrayerCountdown az = PrayerCountdown.of(ADANA, Instant.parse("2026-11-04T14:42:30.250Z"));
        assertEquals("00:", az.hoursPrefix());
        assertEquals(149_750, az.millisWithinHour());
    }

    private static PrayerDayEntity day(String date, String fajr, String sunrise, String dhuhr, String asr,
                                       String maghrib, String isha) {
        return new PrayerDayEntity(9146, date, fajr, sunrise, dhuhr, asr, maghrib, isha, "21.5.1448", "11:32", 180);
    }
}
