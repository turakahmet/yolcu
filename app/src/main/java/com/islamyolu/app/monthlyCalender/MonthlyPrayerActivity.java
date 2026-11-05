package com.islamyolu.app.monthlyCalender;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.islamyolu.app.R;
import com.islamyolu.app.prayertimes.PrayerCountdown;
import com.islamyolu.app.prayertimes.PrayerDisplay;
import com.islamyolu.app.prayertimes.PrayerSlot;
import com.islamyolu.app.prayertimes.PrayerTimeline;
import com.islamyolu.app.prayertimes.db.PrayerDayEntity;
import com.islamyolu.app.prayertimes.ui.PrayerTimesViewModel;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Aylık namaz takvimi: bugün ve sonraki 30 gün, Room'dan (§5). Vakitler şehrin yerel saatiyle,
 * geri sayım şehrin gmt farkıyla hesaplanır; internet gerekmez.
 */
public class MonthlyPrayerActivity extends AppCompatActivity {

    private static final int DAYS_AHEAD = 30;

    private TextView cityNameText, hijriDateText, remainingTimeTextMonthly, remainingTimeLabel;
    private ImageView prayerTimeImage;
    private RecyclerView prayerTimesRecycler;
    private final Map<PrayerSlot, TextView> times = new EnumMap<>(PrayerSlot.class);
    private final Map<PrayerSlot, TextView> labels = new EnumMap<>(PrayerSlot.class);
    private final Map<PrayerSlot, ImageView> icons = new EnumMap<>(PrayerSlot.class);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable remainingTimeRunnable = new Runnable() {
        @Override
        public void run() {
            long delay = updateRemainingTime();
            if (delay > 0) {
                // Saniye değiştiği anda (ana ekran ve widget ile aynı değer)
                handler.postDelayed(this, delay + 15);
            }
        }
    };

    private PrayerTimesViewModel viewModel;
    private PrayerTimeline timeline;
    private boolean warningShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_prayer);

        initializeViews();
        prayerTimesRecycler.setLayoutManager(new LinearLayoutManager(this));

        viewModel = new ViewModelProvider(this).get(PrayerTimesViewModel.class);
        viewModel.getState().observe(this, this::render);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refresh(DAYS_AHEAD + 1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(remainingTimeRunnable);
    }

    private void initializeViews() {
        cityNameText = findViewById(R.id.city_name);
        hijriDateText = findViewById(R.id.hijri_date);
        remainingTimeTextMonthly = findViewById(R.id.remaining_time_monthly);
        remainingTimeLabel = findViewById(R.id.remaining_time_label);
        prayerTimeImage = findViewById(R.id.prayer_time_image);
        prayerTimesRecycler = findViewById(R.id.prayer_times_recycler);

        bind(PrayerSlot.IMSAK, R.id.imsak_time_monthly, R.id.imsak_time_label, R.id.imsak_time_icon);
        bind(PrayerSlot.GUNES, R.id.gunes_time_monthly, R.id.gunes_time_label, R.id.gunes_time_icon);
        bind(PrayerSlot.OGLE, R.id.ogle_time_monthly, R.id.ogle_time_label, R.id.ogle_time_icon);
        bind(PrayerSlot.IKINDI, R.id.ikindi_time_monthly, R.id.ikindi_time_label, R.id.ikindi_time_icon);
        bind(PrayerSlot.AKSAM, R.id.aksam_time_monthly, R.id.aksam_time_label, R.id.aksam_time_icon);
        bind(PrayerSlot.YATSI, R.id.yatsi_time_monthly, R.id.yatsi_time_label, R.id.yatsi_time_icon);
    }

    private void bind(PrayerSlot slot, int timeId, int labelId, int iconId) {
        times.put(slot, findViewById(timeId));
        labels.put(slot, findViewById(labelId));
        icons.put(slot, findViewById(iconId));
    }

    private void render(PrayerTimesViewModel.State state) {
        if (state.status == PrayerTimesViewModel.Status.NO_CITY) {
            showWarningAndFinish();
            return;
        }
        cityNameText.setText(state.selection != null ? state.selection.getCityName() : "");
        timeline = state.timeline;

        Instant now = Instant.now();
        PrayerDayEntity today = timeline.today(now);
        hijriDateText.setText(PrayerDisplay.hijri(this, today != null ? today.getHijri() : null));
        for (PrayerSlot slot : PrayerSlot.values()) {
            times.get(slot).setText(today != null ? PrayerDisplay.time(slot.timeOf(today)) : PrayerDisplay.EMPTY_TIME);
        }

        // Bugünden sonraki günler (önceki sürümdeki gibi 30 gün)
        List<PrayerDayEntity> upcoming = new ArrayList<>();
        LocalDate todayDate = timeline.cityToday(now);
        if (todayDate != null) {
            for (PrayerDayEntity day : timeline.days()) {
                LocalDate date = LocalDate.parse(day.getDate());
                if (date.isAfter(todayDate) && upcoming.size() < DAYS_AHEAD) {
                    upcoming.add(day);
                }
            }
        }
        prayerTimesRecycler.setAdapter(new MonthlyPrayerAdapter(upcoming,
                getResources().getConfiguration().getLocales().get(0)));

        if (state.status == PrayerTimesViewModel.Status.OK) {
            updatePrayerTimeImage(timeline.next(now));
            handler.removeCallbacks(remainingTimeRunnable);
            handler.post(remainingTimeRunnable);
        } else {
            handler.removeCallbacks(remainingTimeRunnable);
            remainingTimeLabel.setText(state.status == PrayerTimesViewModel.Status.UPDATING
                    ? R.string.prayer_status_updating : R.string.prayer_status_outdated_offline);
            remainingTimeTextMonthly.setText("");
        }
    }

    /** @return bir sonraki çizime kadar ms; veri bittiyse 0 */
    private long updateRemainingTime() {
        if (timeline == null) return 0;
        PrayerCountdown countdown = PrayerCountdown.of(timeline, Instant.now());
        if (countdown == null) {
            remainingTimeLabel.setText(R.string.prayer_status_outdated_offline);
            remainingTimeTextMonthly.setText("");
            return 0;
        }
        remainingTimeLabel.setText(PrayerDisplay.nameRes(countdown.next.slot));
        remainingTimeTextMonthly.setText(countdown.text());
        return countdown.millisUntilTick;
    }

    /** Sıradaki vaktin görseli ve vurgusu (önceki sürümle aynı) */
    private void updatePrayerTimeImage(PrayerTimeline.Moment next) {
        if (next == null) {
            prayerTimeImage.setImageResource(R.drawable.imsak_mountly);
            return;
        }
        int highlight = ContextCompat.getColor(this, R.color.current_namaz_time);
        icons.get(next.slot).setColorFilter(highlight, PorterDuff.Mode.SRC_IN);
        times.get(next.slot).setTextColor(highlight);
        labels.get(next.slot).setTextColor(highlight);
        prayerTimeImage.setImageResource(imageFor(next.slot));
    }

    @DrawableRes
    private static int imageFor(PrayerSlot slot) {
        switch (slot) {
            case GUNES:
                return R.drawable.gunes_mountly;
            case OGLE:
                return R.drawable.ogle_mountly;
            case IKINDI:
                return R.drawable.ikindi_mountly;
            case AKSAM:
                return R.drawable.aksam_mountly;
            case YATSI:
                return R.drawable.yatsi_mountly;
            default:
                return R.drawable.imsak_mountly;
        }
    }

    private void showWarningAndFinish() {
        if (warningShown) return;
        warningShown = true;
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.uyariLabel))
                .setMessage(getString(R.string.prayer_monthly_needs_city))
                .setPositiveButton(getString(R.string.tamam), (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }
}
