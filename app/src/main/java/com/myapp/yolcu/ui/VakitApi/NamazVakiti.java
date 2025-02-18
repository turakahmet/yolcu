package com.myapp.yolcu.ui.VakitApi;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Locale;

import android.icu.util.IslamicCalendar;
import android.util.Log;

import com.myapp.yolcu.ui.Entity.PrayerNames;

public class NamazVakiti {
    private static final String TAG = "NamazVakiti";
    private static volatile NamazVakiti instance;
    private static final Object lock = new Object();

    private String imsak, gunes, ogle, ikindi, aksam, yatsi;
    private GecerliVakit gecerliVakit;
    private NextPrayerTime nextPrayerTime;

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
        IMSAK,
        GUNES,
        OGLE,
        IKINDI,
        AKSAM,
        YATSI
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
            IslamicCalendar hijriCalendar = new IslamicCalendar();
            int day = hijriCalendar.get(IslamicCalendar.DAY_OF_MONTH);
            int month = hijriCalendar.get(IslamicCalendar.MONTH) + 1;
            int year = hijriCalendar.get(IslamicCalendar.YEAR);

            String[] hijriMonths = {
                    "Muharrem", "Safer", "Rebiülevvel", "Rebiülahir", "Cemaziyelevvel",
                    "Cemaziyelahir", "Recep", "Şaban", "Ramazan", "Şevval", "Zilkade", "Zilhicce"
            };

            return day + " " + hijriMonths[month - 1] + " " + year;
        } catch (Exception e) {
            return "Hicri tarih hesaplanamadı";
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


    public String getNextPrayerTimeRemainingTimeFormatted(boolean saatDakikaSaniyeSeklinde) {
        try {
            NextPrayerTime nextPrayer = getNextPrayer();
            if (nextPrayer == null || nextPrayer.getTime() == null || nextPrayer.getName() == null) {
                Log.w(TAG, "Sonraki namaz vakti bilgisi bulunamadı");
                return "Süre hesaplanamadı";
            }

            Calendar now = Calendar.getInstance();
            Calendar prayerTime = getPrayerCalendar(nextPrayer.getTime());
            if (prayerTime == null) {
                Log.w(TAG, "Namaz vakti takvimi oluşturulamadı");
                return "Süre hesaplanamadı";
            }

            boolean geceYarisiGecildiMi = now.get(Calendar.HOUR_OF_DAY) == 0;

            // Eğer yatsıdan sonra İmsak vakti hesaplanıyorsa ve tarih yanlışsa düzeltilir
            if (!geceYarisiGecildiMi && nextPrayer.getName().equals(PrayerNames.IMSAK.getValue())) {
                prayerTime.add(Calendar.DAY_OF_MONTH, 1);
            }

            long minDiff = prayerTime.getTimeInMillis() - now.getTimeInMillis();
            return formatRemainingTime(minDiff, saatDakikaSaniyeSeklinde);

        } catch (Exception e) {
            Log.e(TAG, "Kalan süre hesaplama hatası: " + e.getMessage(), e);
            return "Süre hesaplanamadı";
        }
    }


    private Calendar getPrayerCalendar(String time) {
        if (time == null) {
            Log.e(TAG, "Namaz vakti null");
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

    private String formatRemainingTime(long minDiff, boolean saatDakikaSaniyeSeklinde) {
        long hours = minDiff / (60 * 60 * 1000);
        long minutes = (minDiff / (60 * 1000)) % 60;
        long seconds = (minDiff / 1000) % 60;
        if (saatDakikaSaniyeSeklinde)
            return String.format(Locale.getDefault(), "%02d:%02d:%02d",
                    hours, minutes, seconds);
        else {
            if (hours == 0 && minutes == 0)
                return String.format(Locale.getDefault(), "%02d:%02d",
                        minutes, seconds);
            else
                return String.format(Locale.getDefault(), "%02d:%02d",
                        hours, minutes);
        }
    }

}
