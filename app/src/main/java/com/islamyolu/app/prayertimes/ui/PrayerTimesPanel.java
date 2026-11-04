package com.islamyolu.app.prayertimes.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.islamyolu.app.R;
import com.islamyolu.app.Service.SharedPrefsHelperService;
import com.islamyolu.app.prayertimes.PrayerDisplay;
import com.islamyolu.app.prayertimes.PrayerSlot;
import com.islamyolu.app.prayertimes.PrayerTimeline;
import com.islamyolu.app.prayertimes.db.PrayerDayEntity;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

/**
 * Ana ekrandaki vakit şeridi ve geri sayım (eski PrayerTimesFetcher'ın yerini alır).
 * Davranış korunur: sıradaki vakit vurgulanır, son 20/10 dakikada renk değişir, kerahat vakti
 * (ayar açıksa) öğle/akşamdan önceki 45 dakikada ve güneş doğduktan sonraki 45 dakikada gösterilir.
 * Hesaplar Room'daki vakitlerle, şehrin gmt farkına göre yapılır (cihaz saat diliminden bağımsız).
 */
public class PrayerTimesPanel {

    /** Kerahat süresi (dakika) — önceki sürümdeki değer */
    private static final int KERAHAT_MINUTES = 45;

    public static final class Views {
        public final Map<PrayerSlot, TextView> times = new EnumMap<>(PrayerSlot.class);
        public final Map<PrayerSlot, LinearLayout> layouts = new EnumMap<>(PrayerSlot.class);
        public TextView remainingLabel;
        public TextView remainingTime;
        public LinearLayout remainingLayout;
        public TextView cityName;
        public View refreshLayout;
    }

    private final Context context;
    private final Views views;
    private final SharedPrefsHelperService settings;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (renderCountdown()) {
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Nullable private PrayerTimesViewModel.State state;
    @Nullable private PrayerSlot highlighted;

    public PrayerTimesPanel(@NonNull Context context, @NonNull Views views, @NonNull SharedPrefsHelperService settings) {
        this.context = context;
        this.views = views;
        this.settings = settings;
    }

    public void render(@NonNull PrayerTimesViewModel.State newState) {
        state = newState;
        highlighted = null;
        views.cityName.setText(newState.selection != null
                ? newState.selection.getCityName()
                : context.getString(R.string.prayer_choose_city));

        showTodayTimes(newState.status == PrayerTimesViewModel.Status.OK
                ? newState.timeline.today(Instant.now()) : null);
        resetHighlights();

        if (newState.status == PrayerTimesViewModel.Status.OK) {
            views.refreshLayout.setVisibility(View.GONE);
            start();
        } else {
            stop();
            views.refreshLayout.setVisibility(View.VISIBLE);
            views.remainingTime.setText("");
            views.remainingLabel.setText(statusMessage(newState.status));
            views.remainingLayout.setBackgroundColor(color(R.color.remain_namaz_time_1));
        }
    }

    public void start() {
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    public void stop() {
        handler.removeCallbacks(tick);
    }

    private void showTodayTimes(@Nullable PrayerDayEntity today) {
        for (PrayerSlot slot : PrayerSlot.values()) {
            views.times.get(slot).setText(today != null ? PrayerDisplay.time(slot.timeOf(today)) : PrayerDisplay.EMPTY_TIME);
        }
    }

    /** @return geri sayım sürsün mü */
    private boolean renderCountdown() {
        if (state == null) return false;
        Instant now = Instant.now();
        PrayerTimeline timeline = state.timeline;
        PrayerTimeline.Moment next = timeline.next(now);
        PrayerDayEntity today = timeline.today(now);
        if (next == null || today == null) {
            // Eldeki veri bitti; ekran öne gelince ViewModel yeniden yükler ve durumu belirler
            views.remainingTime.setText("");
            views.remainingLabel.setText(statusMessage(PrayerTimesViewModel.Status.OUTDATED_OFFLINE));
            return false;
        }
        if (next.slot != highlighted) {
            // Vakit değişti (gece yarısı yeni gün de dahil): bugünün vakitleri ve vurgu yenilenir
            showTodayTimes(today);
            highlighted = next.slot;
            resetHighlights();
            views.layouts.get(next.slot).setBackgroundColor(color(R.color.current_namaz_time));
        }

        Duration remaining = Duration.between(now, next.instant);
        views.remainingTime.setText(PrayerDisplay.remaining(remaining, true));
        String label = context.getString(PrayerDisplay.remainingLabelRes(next.slot));
        LinearLayout nextLayout = views.layouts.get(next.slot);
        long minutes = remaining.toMinutes();
        boolean kerahatEnabled = settings.getSwitchState("switchNotificationKerahat");

        if (minutes < 10) {
            setCountdownColor(nextLayout, R.color.remain_namaz_time_3, true);
            views.remainingLabel.setText(label);
        } else if (minutes < 20) {
            setCountdownColor(nextLayout, R.color.remain_namaz_time_2, true);
            views.remainingLabel.setText(label);
        } else if (kerahatEnabled && minutes < KERAHAT_MINUTES
                && (next.slot == PrayerSlot.OGLE || next.slot == PrayerSlot.AKSAM)) {
            setCountdownColor(nextLayout, R.color.remain_kerahat_time, true);
            views.remainingLabel.setText(context.getString(R.string.prayer_remaining_with_kerahat,
                    label, context.getString(R.string.kerahat_in_progress)));
        } else if (kerahatEnabled && next.slot == PrayerSlot.OGLE && isAfterSunriseKerahat(timeline, next.date, now)) {
            setCountdownColor(nextLayout, R.color.remain_kerahat_time, true);
            views.remainingLabel.setText(R.string.kerahat_in_progress);
        } else {
            setCountdownColor(nextLayout, R.color.remain_namaz_time_1, false);
            views.remainingLabel.setText(label);
        }
        return true;
    }

    /** Güneş doğduktan sonraki 45 dakika */
    private static boolean isAfterSunriseKerahat(PrayerTimeline timeline, LocalDate date, Instant now) {
        Instant sunrise = timeline.instantOf(date, PrayerSlot.GUNES);
        return sunrise != null && now.isAfter(sunrise)
                && now.isBefore(sunrise.plus(Duration.ofMinutes(KERAHAT_MINUTES)));
    }

    private void setCountdownColor(LinearLayout nextLayout, @ColorRes int colorRes, boolean alsoNextLayout) {
        views.remainingLayout.setBackgroundColor(color(colorRes));
        nextLayout.setBackgroundColor(color(alsoNextLayout ? colorRes : R.color.current_namaz_time));
    }

    private void resetHighlights() {
        for (LinearLayout layout : views.layouts.values()) {
            layout.setBackground(null);
        }
    }

    private int statusMessage(PrayerTimesViewModel.Status status) {
        switch (status) {
            case NO_CITY:
                return R.string.prayer_status_no_city;
            case UPDATING:
                return R.string.prayer_status_updating;
            default:
                return R.string.prayer_status_outdated_offline;
        }
    }

    private int color(@ColorRes int res) {
        return ContextCompat.getColor(context, res);
    }
}
