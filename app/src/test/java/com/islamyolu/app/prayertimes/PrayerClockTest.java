package com.islamyolu.app.prayertimes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class PrayerClockTest {

    private TimeZone originalTimeZone;

    @Before
    public void setUp() {
        // Sonuçlar cihaz saat diliminden bağımsız olmalı; bilerek alakasız bir dilim seçiliyor
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    public void toInstant_turkiyeVakti() {
        // Adana 2026-11-01 imsak 05:34, gmt 3
        assertEquals(Instant.parse("2026-11-01T02:34:00Z"),
                PrayerClock.toInstant(LocalDate.of(2026, 11, 1), "05:34", 180));
    }

    @Test
    public void toInstant_kesirliVeNegatifFark() {
        // Hindistan gmt 5.5, Newfoundland gmt -3.5
        assertEquals(Instant.parse("2026-11-01T06:30:00Z"),
                PrayerClock.toInstant(LocalDate.of(2026, 11, 1), "12:00", 330));
        assertEquals(Instant.parse("2026-11-01T15:30:00Z"),
                PrayerClock.toInstant(LocalDate.of(2026, 11, 1), "12:00", -210));
    }

    @Test
    public void toInstant_nullVakitNullDoner() {
        assertNull(PrayerClock.toInstant(LocalDate.of(2026, 6, 21), null, 120));
    }

    @Test
    public void toInstant_yazSaatiGecisindeHerGunKendiFarkiniKullanir() {
        // Almanya 2026: 24 Ekim yaz saati (+2), 25 Ekim kış saati (+1)
        assertEquals(Instant.parse("2026-10-24T04:00:00Z"),
                PrayerClock.toInstant(LocalDate.of(2026, 10, 24), "06:00", 120));
        assertEquals(Instant.parse("2026-10-25T05:00:00Z"),
                PrayerClock.toInstant(LocalDate.of(2026, 10, 25), "06:00", 60));
    }

    @Test
    public void cityToday_turkiyeGeceYarisiSinirlari() {
        Map<LocalDate, Integer> offsets = offsets(LocalDate.of(2026, 11, 1), 180, 180, 180);
        // 21:00Z = 2 Kasım 00:00 yerel; bir saniye öncesi hâlâ 1 Kasım
        assertEquals(LocalDate.of(2026, 11, 2),
                PrayerClock.cityToday(Instant.parse("2026-11-01T21:00:00Z"), offsets::get));
        assertEquals(LocalDate.of(2026, 11, 1),
                PrayerClock.cityToday(Instant.parse("2026-11-01T20:59:59Z"), offsets::get));
    }

    @Test
    public void cityToday_cihazTarihindenFarkliOlabilir() {
        // Los Angeles (-8): UTC'de 2 Kasım 03:00 iken şehirde hâlâ 1 Kasım
        Map<LocalDate, Integer> offsets = offsets(LocalDate.of(2026, 10, 31), -480, -480, -480, -480);
        assertEquals(LocalDate.of(2026, 11, 1),
                PrayerClock.cityToday(Instant.parse("2026-11-02T03:00:00Z"), offsets::get));
    }

    @Test
    public void cityToday_sonbaharGeriAlmaGunu() {
        // 24 Ekim +2, 25 Ekim +1. 22:30Z = 25 Ekim 00:30 yerel (geçiş 03:00'te)
        Map<LocalDate, Integer> offsets = offsets(LocalDate.of(2026, 10, 23), 120, 120, 60, 60);
        assertEquals(LocalDate.of(2026, 10, 25),
                PrayerClock.cityToday(Instant.parse("2026-10-24T22:30:00Z"), offsets::get));
        assertEquals(LocalDate.of(2026, 10, 24),
                PrayerClock.cityToday(Instant.parse("2026-10-24T21:59:59Z"), offsets::get));
        // 25 Ekim 25 saatlik gün: 23:30 yerel (+1) = 22:30Z hâlâ 25 Ekim
        assertEquals(LocalDate.of(2026, 10, 25),
                PrayerClock.cityToday(Instant.parse("2026-10-25T22:30:00Z"), offsets::get));
        assertEquals(LocalDate.of(2026, 10, 26),
                PrayerClock.cityToday(Instant.parse("2026-10-25T23:00:00Z"), offsets::get));
    }

    @Test
    public void cityToday_ilkbaharIleriAlmaGunu() {
        // 28 Mart +1, 29 Mart +2. 23:30Z = 29 Mart 00:30 yerel
        Map<LocalDate, Integer> offsets = offsets(LocalDate.of(2026, 3, 27), 60, 60, 120, 120);
        assertEquals(LocalDate.of(2026, 3, 29),
                PrayerClock.cityToday(Instant.parse("2026-03-28T23:30:00Z"), offsets::get));
        // 29 Mart 23 saatlik gün: 30 Mart 00:00 (+2) = 29 Mart 22:00Z
        assertEquals(LocalDate.of(2026, 3, 30),
                PrayerClock.cityToday(Instant.parse("2026-03-29T22:00:00Z"), offsets::get));
    }

    @Test
    public void nextDayAt_sehrinGeceYarisiniKullanir() {
        // Ankara (+3): 4 Kasım'ın ertesi günü 00:02 = 4 Kasım 21:02Z (cihaz GMT olsa da)
        assertEquals(Instant.parse("2026-11-04T21:02:00Z"),
                PrayerClock.nextDayAt(LocalDate.of(2026, 11, 4), java.time.LocalTime.of(0, 2), 180));
        // Almanya geri alma günü: 25 Ekim 00:02 hâlâ yaz saati (+2) → 24 Ekim 22:02Z
        assertEquals(Instant.parse("2026-10-24T22:02:00Z"),
                PrayerClock.nextDayAt(LocalDate.of(2026, 10, 24), java.time.LocalTime.of(0, 2), 120));
    }

    @Test
    public void alarmAni_yazSaatiGecisGunu() {
        // 25 Ekim Almanya (+1): öğle 12:45 → 11:45Z; bir gün önce (+2) aynı saat 10:45Z
        assertEquals(Instant.parse("2026-10-25T11:45:00Z"),
                PrayerClock.toInstant(LocalDate.of(2026, 10, 25), "12:45", 60));
        assertEquals(Instant.parse("2026-10-24T10:45:00Z"),
                PrayerClock.toInstant(LocalDate.of(2026, 10, 24), "12:45", 120));
    }

    @Test
    public void cityToday_veriYoksaNull() {
        Map<LocalDate, Integer> offsets = offsets(LocalDate.of(2026, 1, 1), 180);
        assertNull(PrayerClock.cityToday(Instant.parse("2026-11-01T12:00:00Z"), offsets::get));
    }

    private static Map<LocalDate, Integer> offsets(LocalDate start, int... minutes) {
        Map<LocalDate, Integer> map = new HashMap<>();
        for (int i = 0; i < minutes.length; i++) {
            map.put(start.plusDays(i), minutes[i]);
        }
        return map;
    }
}
