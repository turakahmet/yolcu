package com.islamyolu.app.main;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.islamyolu.app.R;
import com.islamyolu.app.prayertimes.PrayerDisplay;
import com.islamyolu.app.prayertimes.PrayerTimeline;
import com.islamyolu.app.prayertimes.db.PrayerDayEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Ana Sayfa tarih paneli: miladi tarih + gün adı ve hicri tarih.
 * Şehir verisi varsa "bugün" şehrin yerel tarihidir ve hicri tarih Diyanet'in verisinden gelir (Room);
 * yoksa cihaz tarihi ve hesaplanan (Ümmülkura) hicri tarih kullanılır. Metinler uygulama dilindedir.
 */
final class HomeDatePanel {

    private final TextView gregorian;
    private final TextView weekday;
    private final TextView hijri;
    @Nullable private PrayerTimeline timeline;

    HomeDatePanel(@NonNull View root) {
        gregorian = root.findViewById(R.id.date_gregorian_text);
        weekday = root.findViewById(R.id.date_weekday_text);
        hijri = root.findViewById(R.id.date_hijri_text);
    }

    /** Vakit verisi değiştiğinde (null: veri yok) */
    void setTimeline(@Nullable PrayerTimeline timeline) {
        this.timeline = timeline;
        refresh();
    }

    /** Gün dönümünü yakalamak için ekran her göründüğünde de çağrılır. */
    void refresh() {
        Context context = gregorian.getContext();
        Instant now = Instant.now();
        LocalDate cityToday = timeline != null ? timeline.cityToday(now) : null;
        LocalDate today = cityToday != null ? cityToday : LocalDate.now();
        PrayerDayEntity day = timeline != null ? timeline.today(now) : null;

        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        gregorian.setText(today.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale)));
        weekday.setText(today.format(DateTimeFormatter.ofPattern("EEEE", locale)));
        hijri.setText(day != null
                ? PrayerDisplay.hijri(context, day.getHijri())
                : PrayerDisplay.hijriTodayCalculated(context));
    }
}
