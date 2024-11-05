package com.myapp.yolcu.ui.VakitApi;

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

import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.HashMap;

public class VakitNoApi {
    HashMap<String, String> prayerTimesList = new HashMap<>();

    private HashMap<String, String> localPrayerTimes(Double latitude, Double longitude) {
        // Günümüz tarihini almak için Calendar kullanılır
        Calendar calendar = Calendar.getInstance();
        // Yıl, ay ve günü Calendar'dan alarak DateComponents nesnesi oluşturuyoruz
        DateComponents date = new DateComponents(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,  // Java'da aylar 0'dan başladığı için 1 ekleniyor
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        CalculationParameters params = CalculationMethod.TURKEY.getParameters().copy(18, 17, 0, CalculationMethod.TURKEY, Madhab.SHAFI, null, new PrayerAdjustments(), new PrayerAdjustments(0, -7, 5, 5, 8, 1), Rounding.NEAREST, Shafaq.GENERAL);
        // Hanefi mezhebi için ayarla

        // PrayerTimes nesnesini oluşturmak için Koordinatlar ve Parametreler
        Coordinates coordinates = new Coordinates(latitude, longitude);
        PrayerTimes prayerTimes = new PrayerTimes(coordinates, date, params);

        prayerTimesList.put("imsakTime", formatPrayerTime(String.valueOf(prayerTimes.getFajr())));
        prayerTimesList.put("gunesTime", formatPrayerTime(String.valueOf(prayerTimes.getSunrise())));
        prayerTimesList.put("ogleTime", formatPrayerTime(String.valueOf(prayerTimes.getDhuhr())));
        prayerTimesList.put("ikindiTime", formatPrayerTime(String.valueOf(prayerTimes.getAsr())));
        prayerTimesList.put("aksamTime", formatPrayerTime(String.valueOf(prayerTimes.getMaghrib())));
        prayerTimesList.put("yatsiTime", formatPrayerTime(String.valueOf(prayerTimes.getIsha())));

        return prayerTimesList;
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

    public HashMap<String, String> localPrayerTimesByPlakaNo(String plakaKodu) {
       City city= City.fromPlateCode(plakaKodu);
        assert city != null;
        return localPrayerTimes(city.getLatitude(),city.getLongitude());
    }

    public HashMap<String, String> localPrayerTimesByCityName(String cityName) {
        City city= City.fromCityName(cityName);
        if (city!=null) {
            return localPrayerTimes(city.getLatitude(),city.getLongitude());
        }
        else
            return new HashMap<>();
    }

}
