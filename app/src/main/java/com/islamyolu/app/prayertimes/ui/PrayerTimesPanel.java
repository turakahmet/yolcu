package com.islamyolu.app.prayertimes.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.islamyolu.app.R;
import com.islamyolu.app.preferences.SettingsPreferences;
import com.islamyolu.app.prayertimes.PrayerCountdown;
import com.islamyolu.app.prayertimes.PrayerDisplay;
import com.islamyolu.app.prayertimes.Kerahat;
import com.islamyolu.app.prayertimes.PrayerSlot;
import com.islamyolu.app.prayertimes.PrayerTimeline;
import com.islamyolu.app.prayertimes.db.PrayerDayEntity;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Ana ekrandaki vakit şeridi ve geri sayım (eski PrayerTimesFetcher'ın yerini alır).
 * Davranış korunur: sıradaki vakit vurgulanır, son 20/10 dakikada renk değişir, kerahat vakti
 * (ayar açıksa) öğle/akşamdan önceki 45 dakikada ve güneş doğduktan sonraki 45 dakikada gösterilir.
 * Hesaplar Room'daki vakitlerle, şehrin gmt farkına göre yapılır (cihaz saat diliminden bağımsız).
 */
public class PrayerTimesPanel {


    public static final class Views {
        public final Map<PrayerSlot, TextView> times = new EnumMap<>(PrayerSlot.class);
        public final Map<PrayerSlot, LinearLayout> layouts = new EnumMap<>(PrayerSlot.class);
        public TextView remainingLabel;
        /**
         * İsteğe bağlı: kerahat uyarısı vakit etiketinin üstünde ayrı satırda. Yoksa uyarı vakit
         * etiketine eklenir ("Öğle vaktine kalan / KERAHAT VAKTİ İÇİNDESİNİZ").
         */
        @Nullable
        public TextView kerahatLabel;
        public TextView remainingTime;
        /** İsteğe bağlı: şehir adını gösteren alan (Ana Sayfa'da şehir hava kartında, kendi okur) */
        @Nullable
        public TextView cityName;
        public View refreshLayout;
        /** İsteğe bağlı: Ana Sayfa'da saat / dakika / saniye ayrı kutularda */
        @Nullable public TextView remainingHours;
        @Nullable public TextView remainingMinutes;
        @Nullable public TextView remainingSeconds;
        /** İsteğe bağlı: sıradaki vaktin ikonu (şeritteki ikonun kopyası) */
        @Nullable public ImageView nextIcon;
    }

    private final Context context;
    private final Views views;
    private final SettingsPreferences settings;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            long delay = renderCountdown();
            if (delay > 0) {
                // Gösterilen saniyenin değiştiği anda çizilir (sabit 1 sn aralık kayıp saniye atlıyordu)
                handler.postDelayed(this, delay + TICK_SLACK_MS);
            }
        }
    };

    /** Saniye sınırının hemen sonrasına düşmek için küçük pay */
    private static final long TICK_SLACK_MS = 15;
    private static final long RELOAD_THROTTLE_MS = 60_000;
    private long lastReloadRequestMs;

    @Nullable private PrayerTimesViewModel.State state;
    @Nullable private PrayerSlot highlighted;
    @Nullable private final Runnable onTimelineExhausted;

    /**
     * @param onTimelineExhausted eldeki günler bittiğinde (uygulama günlerce açık kaldıysa) yeniden
     *                            yükleme için çağrılır
     */
    public PrayerTimesPanel(@NonNull Context context, @NonNull Views views, @NonNull SettingsPreferences settings,
                            @Nullable Runnable onTimelineExhausted) {
        this.context = context;
        this.views = views;
        this.settings = settings;
        this.onTimelineExhausted = onTimelineExhausted;
    }

    public void render(@NonNull PrayerTimesViewModel.State newState) {
        state = newState;
        highlighted = null;
        if (views.cityName != null) {
            views.cityName.setText(newState.selection != null
                    ? newState.selection.getCityName()
                    : context.getString(R.string.prayer_choose_city));
        }

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
            showParts(-1);
            showKerahat(false);
            views.remainingLabel.setText(statusMessage(newState.status));
            views.remainingTime.setTextColor(color(R.color.color_on_header));
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

    /** @return bir sonraki çizime kadar ms; geri sayım durduysa 0 */
    private long renderCountdown() {
        if (state == null) return 0;
        Instant now = Instant.now();
        PrayerTimeline timeline = state.timeline;
        PrayerCountdown countdown = PrayerCountdown.of(timeline, now);
        PrayerDayEntity today = timeline.today(now);
        if (countdown == null || today == null) {
            // Eldeki günler bitti (ör. uygulama günlerce açık kaldı): Room'dan yeniden yüklenir;
            // gerçekten veri yoksa ViewModel durumu belirler
            views.remainingTime.setText("");
            showParts(-1);
            showKerahat(false);
            views.remainingLabel.setText(statusMessage(PrayerTimesViewModel.Status.OUTDATED_OFFLINE));
            // Room'da da sonraki gün yoksa yükleme aynı sonucu verir: döngüye girmesin diye dakikada bir
            long nowMs = System.currentTimeMillis();
            if (onTimelineExhausted != null && nowMs - lastReloadRequestMs > RELOAD_THROTTLE_MS) {
                lastReloadRequestMs = nowMs;
                onTimelineExhausted.run();
            }
            return 0;
        }
        PrayerTimeline.Moment next = countdown.next;
        if (next.slot != highlighted) {
            // Vakit değişti (gece yarısı yeni gün de dahil): bugünün vakitleri ve vurgu yenilenir
            showTodayTimes(today);
            highlighted = next.slot;
            resetHighlights();
            setActive(views.layouts.get(next.slot), true);
            showNextIcon(views.layouts.get(next.slot));
        }

        Duration remaining = countdown.remaining();
        views.remainingTime.setText(countdown.text());
        showParts(remaining.getSeconds());
        String label = context.getString(PrayerDisplay.remainingLabelRes(next.slot));
        long minutes = remaining.toMinutes();
        boolean kerahatEnabled = settings.isKerahatOn();

        boolean kerahat = false;
        if (minutes < 10) {
            setCountdownColor(R.color.color_countdown_urgent);
        } else if (minutes < 20) {
            setCountdownColor(R.color.color_countdown_soon);
        } else if (kerahatEnabled && minutes < Kerahat.MINUTES
                && Kerahat.precedes(next.slot)) {
            setCountdownColor(R.color.color_countdown_kerahat);
            kerahat = true;
        } else if (kerahatEnabled && next.slot == PrayerSlot.OGLE && isAfterSunriseKerahat(timeline, next.date, now)) {
            setCountdownColor(R.color.color_countdown_kerahat);
            kerahat = true;
        } else {
            setCountdownColor(R.color.color_on_header);
        }
        showKerahat(kerahat);
        if (kerahat && views.kerahatLabel == null) {
            views.remainingLabel.setText(context.getString(R.string.prayer_remaining_with_kerahat,
                    label, context.getString(R.string.kerahat_in_progress)));
        } else {
            views.remainingLabel.setText(label);
        }
        return countdown.millisUntilTick;
    }

    /** Kerahat uyarısını vakit etiketinin üstünde gösterir/gizler (ayrı etiket varsa). */
    private void showKerahat(boolean visible) {
        if (views.kerahatLabel != null) {
            views.kerahatLabel.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /** Güneş doğduktan sonraki 45 dakika */
    private static boolean isAfterSunriseKerahat(PrayerTimeline timeline, LocalDate date, Instant now) {
        Instant sunrise = timeline.instantOf(date, PrayerSlot.GUNES);
        return sunrise != null && now.isAfter(sunrise)
                && now.isBefore(sunrise.plus(Duration.ofMinutes(Kerahat.MINUTES)));
    }

    /** Uyarı (son 20/10 dk, kerahat) yalnızca geri sayım rakamlarının rengiyle gösterilir. */
    private void setCountdownColor(@ColorRes int colorRes) {
        int value = color(colorRes);
        views.remainingTime.setTextColor(value);
        for (TextView part : new TextView[]{views.remainingHours, views.remainingMinutes, views.remainingSeconds}) {
            if (part != null) {
                part.setTextColor(value);
            }
        }
    }

    /** Kutulara saat, dakika, saniye yazar; {@code totalSeconds < 0} ise boş gösterir. */
    private void showParts(long totalSeconds) {
        if (views.remainingHours == null || views.remainingMinutes == null || views.remainingSeconds == null) {
            return;
        }
        if (totalSeconds < 0) {
            String empty = context.getString(R.string.countdown_box_empty);
            views.remainingHours.setText(empty);
            views.remainingMinutes.setText(empty);
            views.remainingSeconds.setText(empty);
            return;
        }
        views.remainingHours.setText(twoDigits(totalSeconds / 3600));
        views.remainingMinutes.setText(twoDigits((totalSeconds % 3600) / 60));
        views.remainingSeconds.setText(twoDigits(totalSeconds % 60));
    }

    /** Geri sayım metniyle aynı biçim: her dilde Latin rakam, iki hane */
    private static String twoDigits(long value) {
        return String.format(Locale.ROOT, "%02d", value);
    }

    /** Sıradaki vaktin ikonunu şeritteki hücreden kopyalar (ayrı renklendirilebilsin diye kopya). */
    private void showNextIcon(@Nullable LinearLayout cell) {
        if (views.nextIcon == null || cell == null || !(cell.getChildAt(0) instanceof ImageView)) {
            return;
        }
        Drawable icon = ((ImageView) cell.getChildAt(0)).getDrawable();
        Drawable.ConstantState state = icon != null ? icon.getConstantState() : null;
        if (state != null) {
            views.nextIcon.setImageDrawable(state.newDrawable(context.getResources()).mutate());
        }
    }

    private void resetHighlights() {
        for (LinearLayout layout : views.layouts.values()) {
            setActive(layout, false);
        }
    }

    /** Sıradaki vakit: altın dolgulu zemin; yazı ve ikon koyu yeşil (okunaklı kontrast). */
    private void setActive(LinearLayout cell, boolean active) {
        cell.setBackgroundResource(active ? R.drawable.bg_home_prayer_active : 0);
        ColorStateList tint = ColorStateList.valueOf(color(active ? R.color.color_on_prayer_active : R.color.color_on_header));
        for (int i = 0; i < cell.getChildCount(); i++) {
            View child = cell.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(tint);
            } else if (child instanceof ImageView) {
                ImageViewCompat.setImageTintList((ImageView) child, tint);
            }
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
