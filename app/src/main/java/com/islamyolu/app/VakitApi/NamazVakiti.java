package com.islamyolu.app.VakitApi;

import android.util.Log;

import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar;
import com.islamyolu.app.Entity.PrayerNames;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;

public class NamazVakiti {
    private static final String TAG = "NamazVakiti";
    private static volatile NamazVakiti instance;
    private static final Object lock = new Object();

    private String imsak, gunes, ogle, ikindi, aksam, yatsi;
    private GecerliVakit gecerliVakit;
    private String monthlyDay;
    private Duration kalanSure;

    public NamazVakiti(String imsak, String gunes, String ogle, String ikindi, String aksam, String yatsi) {
        this.imsak = imsak;
        this.gunes = gunes;
        this.ogle = ogle;
        this.ikindi = ikindi;
        this.aksam = aksam;
        this.yatsi = yatsi;
    }

    public static NamazVakiti getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new NamazVakiti();
                }
            }
        }
        return instance;
    }

    public static void updateInstance(NamazVakiti newInstance) {
        synchronized (lock) {
            instance = newInstance;
        }
    }

    public NamazVakiti() {
    }

    public enum GecerliVakit {
        IMSAK("İmsak"),
        GUNES("Güneş"),
        OGLE("Öğle"),
        IKINDI("İkindi"),
        AKSAM("Akşam"),
        YATSI("Yatsı");


        private final String value;

        GecerliVakit(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class NextPrayerTime {
        private String name;
        private String time;

        NextPrayerTime(String name, String time) {
            this.name = name;
            this.time = time;
        }

        public NextPrayerTime() {

        }

        public String getName() {
            return name;
        }

        public String getTime() {
            return time;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }

    public String getImsak() {
        return imsak;
    }

    public void setImsak(String imsak) {
        this.imsak = imsak;
    }

    public String getGunes() {
        return gunes;
    }

    public void setGunes(String gunes) {
        this.gunes = gunes;
    }

    public String getOgle() {
        return ogle;
    }

    public void setOgle(String ogle) {
        this.ogle = ogle;
    }

    public String getIkindi() {
        return ikindi;
    }

    public void setIkindi(String ikindi) {
        this.ikindi = ikindi;
    }

    public String getAksam() {
        return aksam;
    }

    public void setAksam(String aksam) {
        this.aksam = aksam;
    }

    public String getYatsi() {
        return yatsi;
    }

    public void setYatsi(String yatsi) {
        this.yatsi = yatsi;
    }

    public GecerliVakit getGecerliVakit() {
        return gecerliVakit;
    }

    public void setGecerliVakit(GecerliVakit gecerliVakit) {
        this.gecerliVakit = gecerliVakit;
    }


    public String getMonthlyDay() {
        return monthlyDay;
    }

    public void setMonthlyDay(String monthlyDay) {
        this.monthlyDay = monthlyDay;
    }

    public LocalTime getVakitTime(GecerliVakit vakit) {
        switch (vakit) {
            case IMSAK:
                return parseTime(imsak);
            case GUNES:
                return parseTime(gunes);
            case OGLE:
                return parseTime(ogle);
            case IKINDI:
                return parseTime(ikindi);
            case AKSAM:
                return parseTime(aksam);
            case YATSI:
                return parseTime(yatsi);
            default:
                throw new IllegalArgumentException("Geçersiz vakit: " + vakit);
        }
    }

    private LocalTime parseTime(String time) {
        try {
            return LocalTime.parse(time);
        } catch (Exception e) {
            throw new IllegalArgumentException("Vakit formatı geçersiz: " + time, e);
        }
    }


    public String getHicriDate() {
        try {
            UmmalquraCalendar cal = new UmmalquraCalendar();

            int hijriDay = cal.get(Calendar.DAY_OF_MONTH);
            int hijriMonth = cal.get(Calendar.MONTH); // 0-based
            int hijriYear = cal.get(Calendar.YEAR);

            String[] months = {
                    "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir", "Cemaziyelevvel",
                    "Cemaziyelahir", "Recep", "Şaban", "Ramazan", "Şevval", "Zilkade", "Zilhicce"
            };

            return hijriDay + " " + months[hijriMonth] + " " + hijriYear;
        } catch (Exception e) {
            return "Hicri tarih hesaplanamadı";
        }
    }

    public NextPrayerTime getNextPrayerByLocalTime() {
        LocalDateTime currentTime = LocalDateTime.now();
        try {
            if (currentTime.isAfter(formatStringtoLocalDateTime(getImsak())) && currentTime.isBefore(formatStringtoLocalDateTime(getGunes()))) {
                gecerliVakit = GecerliVakit.GUNES;
                return new NextPrayerTime(PrayerNames.GUNES.getValue(), getGunes());
            } else if (currentTime.isAfter(formatStringtoLocalDateTime(getGunes())) && currentTime.isBefore(formatStringtoLocalDateTime(getOgle()))) {
                gecerliVakit = GecerliVakit.OGLE;
                return new NextPrayerTime(PrayerNames.OGLE.getValue(), getOgle());
            } else if (currentTime.isAfter(formatStringtoLocalDateTime(getOgle())) && currentTime.isBefore(formatStringtoLocalDateTime(getIkindi()))) {
                gecerliVakit = GecerliVakit.IKINDI;
                return new NextPrayerTime(PrayerNames.IKINDI.getValue(), getIkindi());
            } else if (currentTime.isAfter(formatStringtoLocalDateTime(getIkindi())) && currentTime.isBefore(formatStringtoLocalDateTime(getAksam()))) {
                gecerliVakit = GecerliVakit.AKSAM;
                return new NextPrayerTime(PrayerNames.AKSAM.getValue(), getAksam());
            } else if (currentTime.isAfter(formatStringtoLocalDateTime(getAksam())) && currentTime.isBefore(formatStringtoLocalDateTime(getYatsi()))) {
                gecerliVakit = GecerliVakit.YATSI;
                return new NextPrayerTime(PrayerNames.YATSI.getValue(), getYatsi());
            } else if (currentTime.isAfter(formatStringtoLocalDateTime(getYatsi()))) {
                gecerliVakit = GecerliVakit.IMSAK;
                return new NextPrayerTime(PrayerNames.IMSAK.getValue(), getImsak());
            } else {
                gecerliVakit = GecerliVakit.IMSAK;
                return new NextPrayerTime(PrayerNames.IMSAK.getValue(), getImsak());
            }
        } catch (Exception e) {
            Log.e(TAG, "Sonraki namaz vakti hesaplama hatası: " + e.getMessage(), e);
            return null;
        }
    }


    public NextPrayerTime getNextPrayer() {
        if (gecerliVakit == null) {
            Log.w(TAG, "Geçerli vakit null");
            return null;
        }

        try {
            switch (gecerliVakit) {
                case IMSAK:
                    return new NextPrayerTime(PrayerNames.IMSAK.getValue(), getImsak());
                case GUNES:
                    return new NextPrayerTime(PrayerNames.GUNES.getValue(), getGunes());
                case OGLE:
                    return new NextPrayerTime(PrayerNames.OGLE.getValue(), getOgle());
                case IKINDI:
                    return new NextPrayerTime(PrayerNames.IKINDI.getValue(), getIkindi());
                case AKSAM:
                    return new NextPrayerTime(PrayerNames.AKSAM.getValue(), getAksam());
                case YATSI:
                    return new NextPrayerTime(PrayerNames.YATSI.getValue(), getYatsi());
                default:
                    Log.w(TAG, "Bilinmeyen vakit türü: " + gecerliVakit);
                    return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Sonraki namaz vakti hesaplama hatası: " + e.getMessage(), e);
            return null;
        }
    }

    // TODO: 24.02.2025 eğer burası düzgün çalışmaz ise rolback et başka birsey yapmadan
    public String getNextPrayerTimeRemainingTimeFormatted(boolean saatDakikaSaniyeSeklinde) {
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            LocalDateTime nextPrayerTime = formatStringtoLocalDateTime(getNextPrayerByLocalTime().getTime());

            if (PrayerNames.IMSAK.getValue().equals(this.getNextPrayer().getName())) {
                if ((currentTime.isAfter(LocalDateTime.of(currentTime.toLocalDate(), LocalTime.parse(getYatsi()))) && currentTime.isBefore(LocalDateTime.of(currentTime.toLocalDate().plusDays(1), LocalTime.MIDNIGHT)))) {
                    nextPrayerTime = nextPrayerTime.plusDays(1);
                }
            }
            Duration remainingTime = Duration.between(currentTime, nextPrayerTime);
            if (remainingTime.isNegative()) {
                Log.e(TAG, "remainingTime negatif oldu: " + remainingTime);
                return "Yeni Vakit Hesaplanıyor";
            }
            return formatRemainingTime(remainingTime, saatDakikaSaniyeSeklinde);
        } catch (Exception e) {
            Log.e(TAG, "Kalan süre hesaplama hatası: " + e.getMessage(), e);
            return "Süre hesaplanamadı: hata kod N2";
        }
    }


    private Calendar getPrayerCalendar(String time) {
        if (time == null) {
            return null;
        }

        Calendar prayerTime = Calendar.getInstance();
        try {
            String[] timeParts = time.split(":");
            if (timeParts.length != 2) {
                Log.e(TAG, "Geçersiz namaz vakti formatı: " + time);
                return null;
            }
            prayerTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
            prayerTime.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
            prayerTime.set(Calendar.SECOND, 0);
            prayerTime.set(Calendar.MILLISECOND, 0);
            return prayerTime;
        } catch (Exception e) {
            Log.e(TAG, "Namaz vakti ayrıştırma hatası: " + e.getMessage());
            return null;
        }
    }

    private String formatRemainingTime(Duration remainingTime, boolean saatDakikaSaniyeSeklinde) {
        long hours = remainingTime.toHours();
        long minutes = remainingTime.toMinutes() % 60;
        long seconds = remainingTime.getSeconds() % 60;
        if (saatDakikaSaniyeSeklinde) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d",
                    hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d",
                    hours, minutes);
        }
    }

    private LocalDateTime formatStringtoLocalDateTime(String timeString) {
        // Saat ve dakikayı LocalTime'a çevir
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime localTime = LocalTime.parse(timeString, timeFormatter);
        return LocalDateTime.of(LocalDate.now(), localTime);
    }

    public Duration getKalanSure() {
        return kalanSure;
    }

    public void setKalanSure(Duration kalanSure) {
        this.kalanSure = kalanSure;
    }

    public String toString(Calendar calendar) {
        // Tarihi "dd.MM.yyyy" formatına getirmek için SimpleDateFormat kullanalım.
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", new Locale("tr"));
        String tarih = sdf.format(calendar.getTime());

        return String.format("Tarih : %s vakitler imsak : %s, gunes : %s, ogle : %s, ikindi : %s, aksam : %s, yatsi : %s",
                tarih,
                this.imsak,
                this.gunes,
                this.ogle,
                this.ikindi,
                this.aksam,
                this.yatsi
        );
    }
}
