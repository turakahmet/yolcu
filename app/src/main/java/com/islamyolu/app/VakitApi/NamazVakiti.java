package com.islamyolu.app.VakitApi;

import android.icu.util.IslamicCalendar;
import android.util.Log;

import com.islamyolu.app.Entity.PrayerNames;

import java.time.LocalTime;
import java.util.Calendar;
import java.util.Locale;

public class NamazVakiti {
    private static final String TAG = "NamazVakiti";
    private static volatile NamazVakiti instance;
    private static final Object lock = new Object();

    private String imsak, gunes, ogle, ikindi, aksam, yatsi;
    private GecerliVakit gecerliVakit;
    private String monthlyDay;

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

    // TODO: 24.02.2025 eğer burası düzgün çalışmaz ise rolback et başka birsey yapmadan
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

//            Log.d(TAG, String.format(
//                "Zaman hesaplanıyor:\n" +
//                "Şu an: %02d:%02d\n" +
//                "Sonraki vakit: %s - %02d:%02d",
//                now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE),
//                nextPrayer.getName(), prayerTime.get(Calendar.HOUR_OF_DAY), prayerTime.get(Calendar.MINUTE)
//            ));

            // Yatsı vaktinden sonra İmsak vakti hesaplanıyorsa
            if (nextPrayer.getName().equals(PrayerNames.IMSAK.getValue())) {
                Calendar yatsiVakti = getPrayerCalendar(getPrayerTime(PrayerNames.YATSI.getValue()));

                if (yatsiVakti != null) {
                    // Şu an yatsı vaktinden sonra mı kontrol et
                    if (now.after(yatsiVakti)) {
                        Log.d(TAG, "Yatsı vaktinden sonra, İmsak için yarının tarihi kullanılacak");
                        prayerTime.add(Calendar.DAY_OF_MONTH, 1);
                    } else {
                        Log.d(TAG, "Henüz yatsı vakti gelmedi, bugünün İmsak vakti kullanılacak");
                    }
                }
            }

            long timeDiff = prayerTime.getTimeInMillis() - now.getTimeInMillis();

            // Negatif süre kontrolü
            if (timeDiff < 0) {
                Log.w(TAG, "Negatif süre hesaplandı, yarının vakti kullanılacak");
                prayerTime.add(Calendar.DAY_OF_MONTH, 1);
                timeDiff = prayerTime.getTimeInMillis() - now.getTimeInMillis();
            }

            return formatRemainingTime(timeDiff, saatDakikaSaniyeSeklinde);

        } catch (Exception e) {
            Log.e(TAG, "Kalan süre hesaplama hatası: " + e.getMessage(), e);
            return "Süre hesaplanamadı";
        }
    }

    private String getPrayerTime(String prayerName) {
        try {
            switch (prayerName) {
                case "Imsak":
                    return imsak;
                case "Gunes":
                    return gunes;
                case "Ogle":
                    return ogle;
                case "Ikindi":
                    return ikindi;
                case "Aksam":
                    return aksam;
                case "Yatsi":
                    return yatsi;
                default:
                    return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Namaz vakti alınırken hata: " + e.getMessage());
            return null;
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

    private String formatRemainingTime(long minDiff, boolean saatDakikaSaniyeSeklinde) {
        long hours = minDiff / (60 * 60 * 1000);
        long minutes = (minDiff / (60 * 1000)) % 60;
        long seconds = (minDiff / 1000) % 60;
        if (saatDakikaSaniyeSeklinde)
            return String.format(Locale.getDefault(), "%02d:%02d:%02d",
                    hours, minutes, seconds);
        else {
            return String.format(Locale.getDefault(), "%02d:%02d",
                    hours, minutes);
        }
    }

}
