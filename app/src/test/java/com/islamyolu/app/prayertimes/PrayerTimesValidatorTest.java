package com.islamyolu.app.prayertimes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/** Ayrıştırıcı (fields eşlemesi) ve doğrulayıcı testleri. */
public class PrayerTimesValidatorTest {

    private static final int CITY_ID = 9146;
    private static final LocalDate START = LocalDate.of(2026, 11, 1);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);
    private static final String FIELDS =
            "[\"date\",\"fajr\",\"sunrise\",\"dhuhr\",\"asr\",\"maghrib\",\"isha\",\"hijri\",\"qibla\",\"gmt\"]";

    private final PrayerTimesParser parser = new PrayerTimesParser();
    private final PrayerTimesValidator validator = new PrayerTimesValidator();

    // --- Ayrıştırıcı ---

    @Test
    public void parse_alanlarAdlaEslenir() throws Exception {
        CityTimesDto file = parser.parse(json(2, CITY_ID, "2027-11-01", FIELDS,
                Collections.singletonList(
                        "[\"2026-11-01\",\"05:34\",\"06:55\",\"12:27\",\"15:22\",\"17:49\",\"19:05\",\"21.5.1448\",\"11:34\",3]")));

        assertEquals(CITY_ID, file.getCityId());
        assertEquals("ADANA", file.getName());
        PrayerDayDto day = file.getDays().get(0);
        assertEquals("2026-11-01", day.getDate());
        assertEquals("05:34", day.getFajr());
        assertEquals("06:55", day.getSunrise());
        assertEquals("12:27", day.getDhuhr());
        assertEquals("15:22", day.getAsr());
        assertEquals("17:49", day.getMaghrib());
        assertEquals("19:05", day.getIsha());
        assertEquals("21.5.1448", day.getHijri());
        assertEquals("11:34", day.getQibla());
        assertEquals(180, day.getGmtOffsetMinutes());
    }

    @Test
    public void parse_farkliSiraVeBilinmeyenAlanDesteklenir() throws Exception {
        String fields = "[\"gmt\",\"isha\",\"date\",\"fajr\",\"sunrise\",\"dhuhr\",\"asr\",\"maghrib\",\"yeniAlan\"]";
        CityTimesDto file = parser.parse(json(2, CITY_ID, "2027-11-01", fields,
                Collections.singletonList(
                        "[2,\"19:05\",\"2026-11-01\",\"05:34\",\"06:55\",\"12:27\",\"15:22\",\"17:49\",{\"x\":1}]")));

        PrayerDayDto day = file.getDays().get(0);
        assertEquals("2026-11-01", day.getDate());
        assertEquals("19:05", day.getIsha());
        assertEquals("05:34", day.getFajr());
        assertEquals(120, day.getGmtOffsetMinutes());
        assertNull(day.getHijri());
        assertNull(day.getQibla());
    }

    @Test
    public void parse_nullVakitVeKesirliGmt() throws Exception {
        CityTimesDto file = parser.parse(json(2, CITY_ID, "2027-11-01", FIELDS,
                Collections.singletonList(
                        "[\"2026-11-01\",null,\"03:10\",\"12:27\",\"16:40\",\"21:50\",null,null,null,3.5]")));

        PrayerDayDto day = file.getDays().get(0);
        assertNull(day.getFajr());
        assertNull(day.getIsha());
        assertEquals(210, day.getGmtOffsetMinutes());
    }

    @Test
    public void parse_zorunluAlanEksikseReddedilir() {
        String fields = "[\"date\",\"fajr\",\"sunrise\",\"dhuhr\",\"asr\",\"maghrib\",\"gmt\"]";
        assertFormatError(json(2, CITY_ID, "2027-11-01", fields,
                Collections.singletonList("[\"2026-11-01\",\"05:34\",\"06:55\",\"12:27\",\"15:22\",\"17:49\",3]")));
    }

    @Test
    public void parse_gmtSayiDegilseReddedilir() {
        assertFormatError(json(2, CITY_ID, "2027-11-01", FIELDS,
                Collections.singletonList(
                        "[\"2026-11-01\",\"05:34\",\"06:55\",\"12:27\",\"15:22\",\"17:49\",\"19:05\",null,null,\"3\"]")));
    }

    @Test
    public void parse_eksikSutunluSatirReddedilir() {
        assertFormatError(json(2, CITY_ID, "2027-11-01", FIELDS,
                Collections.singletonList("[\"2026-11-01\",\"05:34\",\"06:55\"]")));
    }

    @Test
    public void parse_gecersizJsonReddedilir() {
        assertFormatError("{\"version\":2,");
    }

    // --- Doğrulayıcı ---

    @Test
    public void validate_gecerliDosya() throws Exception {
        PrayerTimesValidator.Result result = validate(json(2, CITY_ID, "2027-11-01", FIELDS, days(START, 60)));
        assertTrue(result.toString(), result.isValid());
    }

    @Test
    public void validate_nullVakitGecerlidir() throws Exception {
        List<String> rows = days(START, 60);
        rows.set(10, row(START.plusDays(10), null, 180));
        assertTrue(validate(json(2, CITY_ID, "2027-11-01", FIELDS, rows)).isValid());
    }

    @Test
    public void validate_yanlisSurum() throws Exception {
        assertErrors(validate(json(1, CITY_ID, "2027-11-01", FIELDS, days(START, 60))),
                PrayerTimesValidator.Error.UNSUPPORTED_VERSION);
    }

    @Test
    public void validate_yanlisCityId() throws Exception {
        assertErrors(validate(json(2, 9999, "2027-11-01", FIELDS, days(START, 60))),
                PrayerTimesValidator.Error.CITY_ID_MISMATCH);
    }

    @Test
    public void validate_60GundenAz() throws Exception {
        assertErrors(validate(json(2, CITY_ID, "2027-11-01", FIELDS, days(START, 59))),
                PrayerTimesValidator.Error.TOO_FEW_DAYS);
    }

    @Test
    public void validate_bosGunler() throws Exception {
        assertErrors(validate(json(2, CITY_ID, "2027-11-01", FIELDS, new ArrayList<>())),
                PrayerTimesValidator.Error.NO_DAYS);
    }

    @Test
    public void validate_eksikGun() throws Exception {
        List<String> rows = days(START, 61);
        rows.remove(30);
        assertErrors(validate(json(2, CITY_ID, "2027-11-01", FIELDS, rows)),
                PrayerTimesValidator.Error.DATES_NOT_CONSECUTIVE);
    }

    @Test
    public void validate_tekrarEdenGun() throws Exception {
        List<String> rows = days(START, 60);
        rows.add(31, rows.get(30));
        assertErrors(validate(json(2, CITY_ID, "2027-11-01", FIELDS, rows)),
                PrayerTimesValidator.Error.DATES_NOT_CONSECUTIVE);
    }

    @Test
    public void validate_hataliSaatBicimi() throws Exception {
        List<String> rows = days(START, 60);
        rows.set(5, row(START.plusDays(5), "5:34", 180));
        assertErrors(validate(json(2, CITY_ID, "2027-11-01", FIELDS, rows)),
                PrayerTimesValidator.Error.INVALID_TIME);
    }

    @Test
    public void validate_validToBugunVeyaGecmisse() throws Exception {
        assertErrors(validate(json(2, CITY_ID, TODAY.toString(), FIELDS, days(START, 60))),
                PrayerTimesValidator.Error.EXPIRED);
    }

    // --- Yardımcılar ---

    private PrayerTimesValidator.Result validate(String json) throws PrayerTimesFormatException {
        return validator.validate(parser.parse(json), CITY_ID, TODAY);
    }

    private static void assertErrors(PrayerTimesValidator.Result result, PrayerTimesValidator.Error expected) {
        assertEquals(EnumSet.of(expected), result.getErrors());
    }

    private void assertFormatError(String json) {
        try {
            parser.parse(json);
            fail("PrayerTimesFormatException bekleniyordu");
        } catch (PrayerTimesFormatException expected) {
            // beklenen
        }
    }

    private static List<String> days(LocalDate start, int count) {
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(row(start.plusDays(i), "05:34", 180));
        }
        return rows;
    }

    private static String row(LocalDate date, String fajr, int gmtMinutes) {
        String fajrJson = fajr == null ? "null" : "\"" + fajr + "\"";
        return "[\"" + date + "\"," + fajrJson
                + ",\"06:55\",\"12:27\",\"15:22\",\"17:49\",\"19:05\",\"21.5.1448\",\"11:34\","
                + (gmtMinutes / 60.0) + "]";
    }

    private static String json(int version, int cityId, String validTo, String fields, List<String> rows) {
        return "{\"version\":" + version
                + ",\"cityId\":" + cityId
                + ",\"name\":\"ADANA\",\"state\":\"ADANA\",\"stateId\":500"
                + ",\"country\":\"TÜRKİYE\",\"countryId\":2"
                + ",\"updatedAt\":\"2026-11-02T00:41:10+00:00\""
                + ",\"validFrom\":\"2026-11-01\",\"validTo\":\"" + validTo + "\""
                + ",\"fields\":" + fields
                + ",\"days\":[" + String.join(",", rows) + "]}";
    }
}
