package com.islamyolu.app.prayertimes;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.islamyolu.app.IslamYoluApplication;
import com.islamyolu.app.Service.SharedPrefsHelperService;
import com.islamyolu.app.main.MainActivity;

import java.time.Instant;
import java.util.Collections;

/**
 * Widget, kalıcı bildirim ve alarm kurulumunun vakitleri okuduğu tek yer: seçili şehir + Room'daki
 * birkaç günlük vakit. Ekranlar bunun yerine LiveData ile okur (PrayerTimesViewModel).
 */
public final class CurrentPrayerTimes {

    /** Seçili şehir; seçilmemişse null */
    @Nullable public final PrayerCitySelection selection;
    @NonNull public final PrayerTimeline timeline;

    private CurrentPrayerTimes(@Nullable PrayerCitySelection selection, @NonNull PrayerTimeline timeline) {
        this.selection = selection;
        this.timeline = timeline;
    }

    /** Disk okur; ana thread'de çağrılmaz. */
    @WorkerThread
    @NonNull
    public static CurrentPrayerTimes load(@NonNull Context context, @NonNull Instant now) {
        PrayerCitySelection selection =
                new SharedPrefsHelperService(context, MainActivity.LOCATION_PREFS).getPrayerCitySelection();
        if (selection == null) {
            return new CurrentPrayerTimes(null, PrayerTimeline.of(Collections.emptyList()));
        }
        PrayerTimeline timeline = IslamYoluApplication.from(context).getPrayerTimesRepository()
                .loadTimeline(selection.getCityId(), now);
        return new CurrentPrayerTimes(selection, timeline);
    }
}
