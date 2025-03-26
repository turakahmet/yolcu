package com.islamyolu.app.VakitApi;

import android.annotation.SuppressLint;

import com.batoulapps.adhan2.CalculationMethod;
import com.batoulapps.adhan2.CalculationParameters;
import com.batoulapps.adhan2.Coordinates;
import com.batoulapps.adhan2.Madhab;
import com.batoulapps.adhan2.PrayerAdjustments;
import com.batoulapps.adhan2.PrayerTimes;
import com.batoulapps.adhan2.data.DateComponents;
import com.batoulapps.adhan2.model.Rounding;
import com.batoulapps.adhan2.model.Shafaq;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Locale;

public class VakitNoApi {

    private NamazVakiti namazVakiti;

    public NamazVakiti localPrayerTimes(Double latitude, Double longitude) {
        // Günümüz tarihini almak için Calendar kullanılır
        Calendar calendar = Calendar.getInstance();
        // Yıl, ay ve günü Calendar'dan alarak DateComponents nesnesi oluşturuyoruz
        DateComponents date = new DateComponents(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,  // Java'da aylar 0'dan başladığı için 1 ekleniyor
                calendar.get(Calendar.DAY_OF_MONTH)
        );


        CalculationParameters params = CalculationMethod.TURKEY.getParameters().copy(18, 17, 0, CalculationMethod.TURKEY, Madhab.SHAFI, null, new PrayerAdjustments(), new PrayerAdjustments(0, -7, 5, 4, 6, 0), Rounding.NEAREST, Shafaq.GENERAL);
        // Hanefi mezhebi için ayarla

        // PrayerTimes nesnesini oluşturmak için Koordinatlar ve Parametreler
        Coordinates coordinates = new Coordinates(latitude, longitude);
        PrayerTimes prayerTimes = new PrayerTimes(coordinates, date, params);

        namazVakiti = NamazVakiti.getInstance();
        namazVakiti.setImsak(formatPrayerTime(String.valueOf(prayerTimes.getFajr())));
        namazVakiti.setGunes(formatPrayerTime(String.valueOf(prayerTimes.getSunrise())));
        namazVakiti.setOgle(formatPrayerTime(String.valueOf(prayerTimes.getDhuhr())));
        namazVakiti.setIkindi(formatPrayerTime(String.valueOf(prayerTimes.getAsr())));
        namazVakiti.setAksam(formatPrayerTime(String.valueOf(prayerTimes.getMaghrib())));
        namazVakiti.setYatsi(formatPrayerTime(String.valueOf(prayerTimes.getIsha())));
//        namazVakiti=new NamazVakiti(
//                formatPrayerTime(String.valueOf(prayerTimes.getFajr())),
//                formatPrayerTime(String.valueOf(prayerTimes.getSunrise())),
//                formatPrayerTime(String.valueOf(prayerTimes.getDhuhr())),
//                formatPrayerTime(String.valueOf(prayerTimes.getAsr())),
//                formatPrayerTime(String.valueOf(prayerTimes.getMaghrib())),
//                formatPrayerTime(String.valueOf(prayerTimes.getIsha()))
//                );
        // geçerli vakiti bul
        LocalTime currentTime = LocalTime.now();
        if (currentTime.isAfter(parseTime(namazVakiti.getImsak())) && currentTime.isBefore(parseTime(namazVakiti.getGunes()))) {
            namazVakiti.setGecerliVakit(NamazVakiti.GecerliVakit.GUNES);
        } else if (currentTime.isAfter(parseTime(namazVakiti.getGunes())) && currentTime.isBefore(parseTime(namazVakiti.getOgle()))) {
            namazVakiti.setGecerliVakit(NamazVakiti.GecerliVakit.OGLE);
        } else if (currentTime.isAfter(parseTime(namazVakiti.getOgle())) && currentTime.isBefore(parseTime(namazVakiti.getIkindi()))) {
            namazVakiti.setGecerliVakit(NamazVakiti.GecerliVakit.IKINDI);
        } else if (currentTime.isAfter(parseTime(namazVakiti.getIkindi())) && currentTime.isBefore(parseTime(namazVakiti.getAksam()))) {
            namazVakiti.setGecerliVakit(NamazVakiti.GecerliVakit.AKSAM);
        } else if (currentTime.isAfter(parseTime(namazVakiti.getAksam())) && currentTime.isBefore(parseTime(namazVakiti.getYatsi()))) {
            namazVakiti.setGecerliVakit(NamazVakiti.GecerliVakit.YATSI);
        } else if (currentTime.isAfter(parseTime(namazVakiti.getYatsi()))) {
            namazVakiti.setGecerliVakit(NamazVakiti.GecerliVakit.IMSAK);
        } else if (currentTime.isAfter(LocalTime.MIDNIGHT)) {
            namazVakiti.setGecerliVakit(NamazVakiti.GecerliVakit.IMSAK);
        }

        return namazVakiti;
    }

    @SuppressLint("DefaultLocale")
    private String formatPrayerTime(String timeString) {
        Instant instantTime = Instant.parse(timeString);
        // ZonedDateTime'a dönüştürme
        ZonedDateTime zonedDateTime = instantTime.atZone(ZoneId.of("Europe/Istanbul"));
        // Formatlamak için
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return zonedDateTime.format(formatter);
    }

    public NamazVakiti localPrayerTimesByPlakaNo(String plakaKodu) {
        City city = City.fromPlateCode(plakaKodu);
        assert city != null;
        return localPrayerTimes(city.getLatitude(), city.getLongitude());
    }

    public NamazVakiti getLocalPrayerTimesByCityName(String cityName) {
        City city = City.fromCityName(cityName);
        if (city != null) {
            return localPrayerTimes(city.getLatitude(), city.getLongitude());
        } else
            return NamazVakiti.getInstance();
    }
//
//    public NamazVakiti getPrayerTimesForDate(String city, Date date) {
//        City cityInfo = City.fromCityName(city);
//        if (cityInfo != null) {
//            Calendar calendar = Calendar.getInstance();
//            calendar.setTime(date);
//
//            DateComponents dateComponents = new DateComponents(
//                calendar.get(Calendar.YEAR),
//                calendar.get(Calendar.MONTH) + 1,
//                calendar.get(Calendar.DAY_OF_MONTH)
//            );
//
//            Coordinates coordinates = new Coordinates(cityInfo.getLatitude(), cityInfo.getLongitude());
//            CalculationParameters params = CalculationMethod.TURKEY.getParameters()
//                .copy(18, 17, 0, CalculationMethod.TURKEY, Madhab.SHAFI, null,
//                    new PrayerAdjustments(), new PrayerAdjustments(0, -7, 5, 4, 6, 0),
//                    Rounding.NEAREST, Shafaq.GENERAL);
//
//            PrayerTimes prayerTimes = new PrayerTimes(coordinates, dateComponents, params);
//
//            namazVakiti = NamazVakiti.getInstance();
//            namazVakiti.setImsak(formatPrayerTime(String.valueOf(prayerTimes.getFajr())));
//            namazVakiti.setGunes(formatPrayerTime(String.valueOf(prayerTimes.getSunrise())));
//            namazVakiti.setOgle(formatPrayerTime(String.valueOf(prayerTimes.getDhuhr())));
//            namazVakiti.setIkindi(formatPrayerTime(String.valueOf(prayerTimes.getAsr())));
//            namazVakiti.setAksam(formatPrayerTime(String.valueOf(prayerTimes.getMaghrib())));
//            namazVakiti.setYatsi(formatPrayerTime(String.valueOf(prayerTimes.getIsha())));
//
//            return namazVakiti;
//        }
//        return null;
//    }

    private LocalTime parseTime(String time) {
        try {
            // String zamanını "HH:mm" formatına göre LocalTime'a dönüştür
            return LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return null;  // Eğer bir hata oluşursa null döndür
        }
    }

    public NamazVakiti localMountlyPrayerTimes(double latitude, double longitude, Calendar calendar) {

        DateComponents date = new DateComponents(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,  // Java'da aylar 0'dan başladığı için 1 ekleniyor
                calendar.get(Calendar.DAY_OF_MONTH)
        );


        CalculationParameters params = CalculationMethod.TURKEY.getParameters().copy(18, 17, 0, CalculationMethod.TURKEY, Madhab.SHAFI, null, new PrayerAdjustments(), new PrayerAdjustments(0, -7, 5, 4, 6, 0), Rounding.NEAREST, Shafaq.GENERAL);
        // Hanefi mezhebi için ayarla

        // PrayerTimes nesnesini oluşturmak için Koordinatlar ve Parametreler
        Coordinates coordinates = new Coordinates(latitude, longitude);
        PrayerTimes prayerTimes = new PrayerTimes(coordinates, date, params);

        namazVakiti = new NamazVakiti();
        namazVakiti.setImsak(formatPrayerTime(String.valueOf(prayerTimes.getFajr())));
        namazVakiti.setGunes(formatPrayerTime(String.valueOf(prayerTimes.getSunrise())));
        namazVakiti.setOgle(formatPrayerTime(String.valueOf(prayerTimes.getDhuhr())));
        namazVakiti.setIkindi(formatPrayerTime(String.valueOf(prayerTimes.getAsr())));
        namazVakiti.setAksam(formatPrayerTime(String.valueOf(prayerTimes.getMaghrib())));
        namazVakiti.setYatsi(formatPrayerTime(String.valueOf(prayerTimes.getIsha())));
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM", new Locale("tr"));
        namazVakiti.setMonthlyDay(sdf.format(calendar.getTime()));
        return namazVakiti;
    }
}
