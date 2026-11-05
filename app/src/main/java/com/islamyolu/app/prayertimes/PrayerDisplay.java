package com.islamyolu.app.prayertimes;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.github.msarhan.ummalqura.calendar.UmmalquraCalendar;
import com.islamyolu.app.R;

import java.util.Calendar;

/**
 * Namaz vakitlerinin ekranda gösterimi için ortak biçimlendirme: ana ekran, aylık takvim,
 * widget ve kalıcı bildirim aynı kuralları kullanır (§5). Metinler strings.xml'den (TR/EN) gelir.
 */
public final class PrayerDisplay {

    /** Vakit oluşmayan gün (kutup bölgesi) için gösterim */
    public static final String EMPTY_TIME = "—";

    private PrayerDisplay() {
    }

    /** "HH:mm" ya da null için "—" (§5). */
    @NonNull
    public static String time(@Nullable String hhmm) {
        return hhmm != null ? hhmm : EMPTY_TIME;
    }

    @StringRes
    public static int nameRes(@NonNull PrayerSlot slot) {
        switch (slot) {
            case IMSAK:
                return R.string.imsak;
            case GUNES:
                return R.string.gunes;
            case OGLE:
                return R.string.ogle;
            case IKINDI:
                return R.string.ikindi;
            case AKSAM:
                return R.string.aksam;
            default:
                return R.string.yatsi;
        }
    }

    /** "Öğle vaktine kalan" gibi geri sayım etiketi */
    @StringRes
    public static int remainingLabelRes(@NonNull PrayerSlot slot) {
        switch (slot) {
            case IMSAK:
                return R.string.imsak_kalan;
            case GUNES:
                return R.string.gunes_kalan;
            case OGLE:
                return R.string.ogle_kalan;
            case IKINDI:
                return R.string.ikindi_kalan;
            case AKSAM:
                return R.string.aksam_kalan;
            default:
                return R.string.yatsi_kalan;
        }
    }

    /**
     * Diyanet'in hicri tarihi ("21.5.1448") → "21 Cemaziyelevvel 1448" (ay adı strings.xml'den).
     * Room'da o gün yoksa veya biçim beklenmedikse hesaplanan hicri tarihe düşer.
     */
    @NonNull
    public static String hijri(@NonNull Context context, @Nullable String diyanetHijri) {
        if (diyanetHijri != null) {
            String[] parts = diyanetHijri.split("\\.");
            if (parts.length == 3) {
                try {
                    int month = Integer.parseInt(parts[1]);
                    String[] months = context.getResources().getStringArray(R.array.hijri_months);
                    if (month >= 1 && month <= months.length) {
                        return Integer.parseInt(parts[0]) + " " + months[month - 1] + " " + Integer.parseInt(parts[2]);
                    }
                } catch (NumberFormatException e) {
                    // aşağıdaki hesaplanan tarihe düşülür
                }
            }
        }
        return hijriTodayCalculated(context);
    }

    /** Diyanet verisi olmadığında (ör. şehir seçilmemiş) Ümmülkura takvimiyle bugünün hicri tarihi */
    @NonNull
    public static String hijriTodayCalculated(@NonNull Context context) {
        UmmalquraCalendar calendar = new UmmalquraCalendar();
        String[] months = context.getResources().getStringArray(R.array.hijri_months);
        return calendar.get(Calendar.DAY_OF_MONTH) + " " + months[calendar.get(Calendar.MONTH)] + " "
                + calendar.get(Calendar.YEAR);
    }
}
