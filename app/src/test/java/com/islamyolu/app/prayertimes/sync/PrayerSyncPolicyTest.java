package com.islamyolu.app.prayertimes.sync;

import static org.junit.Assert.assertEquals;

import com.islamyolu.app.prayertimes.db.PrayerCityEntity;

import org.junit.Test;

import java.time.LocalDate;

/** §4: arka plan senkronu yalnızca gerektiğinde indirir. */
public class PrayerSyncPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    @Test
    public void veriYoksaIndirilir() {
        assertEquals(PrayerSyncPolicy.Reason.NO_DATA, PrayerSyncPolicy.evaluate(null, null, TODAY));
        // Şehir değişti: yeni şehrin kaydı yok
        assertEquals(PrayerSyncPolicy.Reason.NO_DATA, PrayerSyncPolicy.evaluate(null, "2027-11-01", TODAY));
        // Meta var ama günler silinmiş
        assertEquals(PrayerSyncPolicy.Reason.NO_DATA, PrayerSyncPolicy.evaluate(city(2), null, TODAY));
    }

    @Test
    public void semaDegistiyseIndirilir() {
        assertEquals(PrayerSyncPolicy.Reason.SCHEMA_CHANGED, PrayerSyncPolicy.evaluate(city(1), "2027-11-01", TODAY));
    }

    @Test
    public void altmisGundenAzKaldiysaIndirilir() {
        assertEquals(PrayerSyncPolicy.Reason.RUNNING_OUT,
                PrayerSyncPolicy.evaluate(city(2), TODAY.plusDays(59).toString(), TODAY));
        assertEquals(PrayerSyncPolicy.Reason.RUNNING_OUT,
                PrayerSyncPolicy.evaluate(city(2), TODAY.minusDays(3).toString(), TODAY));
    }

    @Test
    public void yeterliVeriVarsaIndirilmez() {
        assertEquals(PrayerSyncPolicy.Reason.UP_TO_DATE,
                PrayerSyncPolicy.evaluate(city(2), TODAY.plusDays(60).toString(), TODAY));
        assertEquals(PrayerSyncPolicy.Reason.UP_TO_DATE,
                PrayerSyncPolicy.evaluate(city(2), "2027-11-01", TODAY));
    }

    @Test
    public void bozukTarihYenidenIndirilir() {
        assertEquals(PrayerSyncPolicy.Reason.NO_DATA, PrayerSyncPolicy.evaluate(city(2), "2027/11/01", TODAY));
    }

    private static PrayerCityEntity city(int schemaVersion) {
        return new PrayerCityEntity(9147, "ALADAĞ", "ADANA", 500, "TÜRKİYE", 2, schemaVersion,
                "2026-11-02T13:38:13+00:00", "2026-11-01", "2027-11-01", 0L, "\"etag\"");
    }
}
