package com.islamyolu.app.monthlyCalender;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.islamyolu.app.Entity.PrayerNames;
import com.islamyolu.app.R;
import com.islamyolu.app.VakitApi.City;
import com.islamyolu.app.VakitApi.NamazVakiti;
import com.islamyolu.app.VakitApi.VakitNoApi;
import com.islamyolu.app.main.MainActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MonthlyPrayerActivity extends AppCompatActivity {
    private TextView cityNameText, hijriDateText, remainingTimeTextMonthly, remainingTimeLabel, ogleTimeMonthly, ogleTimeLabel, imsakTimeMonthly, imsakTimeLabel, gunesTimeMonthly, gunesTimeLabel, aksamTimeMonthly, aksamTimeLabel, ikindiTimeMonthly, ikindiTimeLabel, yatsiTimeMonthly, yatsiTimeLabel;
    private ImageView prayerTimeImage, ogleTimeIcon, imsakTimeIcon, gunesTimeIcon, aksamTimeIcon, ikindiTimeIcon, yatsiTimeIcon;
    private RecyclerView prayerTimesRecycler;
    private Handler handler;
    private Runnable remainingTimeRunnable;
    private VakitNoApi vakitNoApi;
    private static final String TAG = "MonthlyPrayerActivity";
    private NamazVakiti currentNamazVakiti;
    private String getCityNameFromPreferences;
    private Dialog mDialog;
    private City city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_prayer);

        initializeViews();
        setupRecyclerView();
        loadCityData();
        if (city != null) {
            startRemainingTimeUpdate();
            updatePrayerTimeImage();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.uyariLabel))
                    .setMessage(getString(R.string.namazVakitHesaplamaHata))
                    .setPositiveButton(getString(R.string.tamam), (dialog, which) -> finish())
                    .show();
        }
    }

    private void initializeViews() {
        cityNameText = findViewById(R.id.city_name);
        hijriDateText = findViewById(R.id.hijri_date);
        remainingTimeTextMonthly = findViewById(R.id.remaining_time_monthly);
        remainingTimeLabel = findViewById(R.id.remaining_time_label);
        prayerTimeImage = findViewById(R.id.prayer_time_image);
        prayerTimesRecycler = findViewById(R.id.prayer_times_recycler);

        ogleTimeIcon = findViewById(R.id.ogle_time_icon);
        ogleTimeMonthly = findViewById(R.id.ogle_time_monthly);
        ogleTimeLabel = findViewById(R.id.ogle_time_label);

        imsakTimeIcon = findViewById(R.id.imsak_time_icon);
        imsakTimeMonthly = findViewById(R.id.imsak_time_monthly);
        imsakTimeLabel = findViewById(R.id.imsak_time_label);

        gunesTimeIcon = findViewById(R.id.gunes_time_icon);
        gunesTimeMonthly = findViewById(R.id.gunes_time_monthly);
        gunesTimeLabel = findViewById(R.id.gunes_time_label);

        aksamTimeIcon = findViewById(R.id.aksam_time_icon);
        aksamTimeMonthly = findViewById(R.id.aksam_time_monthly);
        aksamTimeLabel = findViewById(R.id.aksam_time_label);

        ikindiTimeIcon = findViewById(R.id.ikindi_time_icon);
        ikindiTimeMonthly = findViewById(R.id.ikindi_time_monthly);
        ikindiTimeLabel = findViewById(R.id.ikindi_time_label);

        yatsiTimeIcon = findViewById(R.id.yatsi_time_icon);
        yatsiTimeMonthly = findViewById(R.id.yatsi_time_monthly);
        yatsiTimeLabel = findViewById(R.id.yatsi_time_label);


        handler = new Handler(Looper.getMainLooper());
        vakitNoApi = new VakitNoApi();
        hijriDateText.setText(NamazVakiti.getInstance().getHicriDate());

    }

    private void setupRecyclerView() {
        prayerTimesRecycler.setLayoutManager(new LinearLayoutManager(this));
        // Adapter setup will be done when data is loaded
    }

    private void loadCityData() {
        SharedPreferences locationPrefs = getSharedPreferences(MainActivity.LOCATION_PREFS, MODE_PRIVATE);
        getCityNameFromPreferences = locationPrefs.getString("city_name", "");

        if (!getCityNameFromPreferences.isEmpty()) {
            cityNameText.setText(getCityNameFromPreferences);
            city = City.fromCityName(getCityNameFromPreferences);
            if (city != null) {
                loadMonthlyPrayerTimes(city);
            }
        }
    }

    private void loadMonthlyPrayerTimes(City city) {
        List<NamazVakiti> monthlyTimes = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        dailyTimesLoad(city, calendar);


        calendar.add(Calendar.DAY_OF_MONTH, 1);

        for (int i = 0; i < 30; i++) {
            NamazVakiti times = vakitNoApi.localMountlyPrayerTimes(
                    city.getLatitude(),
                    city.getLongitude(),
                    calendar
            );
            monthlyTimes.add(times);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        MonthlyPrayerAdapter adapter = new MonthlyPrayerAdapter(monthlyTimes);
        prayerTimesRecycler.setAdapter(adapter);
    }

    private void dailyTimesLoad(City city, Calendar calendar) {
        NamazVakiti dailyTimes = vakitNoApi.localMountlyPrayerTimes(
                city.getLatitude(),
                city.getLongitude(),
                calendar
        );
        imsakTimeMonthly.setText(dailyTimes.getImsak());
        gunesTimeMonthly.setText(dailyTimes.getGunes());
        ogleTimeMonthly.setText(dailyTimes.getOgle());
        ikindiTimeMonthly.setText(dailyTimes.getIkindi());
        aksamTimeMonthly.setText(dailyTimes.getAksam());
        yatsiTimeMonthly.setText(dailyTimes.getYatsi());
    }

    private void startRemainingTimeUpdate() {
        remainingTimeRunnable = new Runnable() {
            @Override
            public void run() {
                updateRemainingTime();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(remainingTimeRunnable);
    }

    private void updateRemainingTime() {
        // Implement remaining time calculation similar to MainActivity
        // Use the current prayer time and calculate remaining time
        currentNamazVakiti = vakitNoApi.getLocalPrayerTimesByCityName(getCityNameFromPreferences);
        remainingTimeLabel.setText(currentNamazVakiti.getGecerliVakit().getValue());
        remainingTimeTextMonthly.setText(currentNamazVakiti.getNextPrayerTimeRemainingTimeFormatted(true));
    }

    private void updatePrayerTimeImage() {
        // Update prayer time image based on current prayer time
        // Example:
        int imageResource;

        if (NamazVakiti.getInstance().getNextPrayer() == null) {
            imageResource = R.drawable.imsak_mountly;
            prayerTimeImage.setImageResource(imageResource);
            return;
        }

        if (PrayerNames.IMSAK.getValue().equals(NamazVakiti.getInstance().getNextPrayer().getName())) {
            imageResource = R.drawable.imsak_mountly;
            imsakTimeIcon.setColorFilter(ContextCompat.getColor(this, R.color.current_namaz_time), PorterDuff.Mode.SRC_IN);
            imsakTimeMonthly.setTextColor(ContextCompat.getColor(this, R.color.current_namaz_time));
            imsakTimeLabel.setTextColor(ContextCompat.getColor(this, R.color.current_namaz_time));
        } else if (PrayerNames.GUNES.getValue().equals(NamazVakiti.getInstance().getNextPrayer().getName())) {
            imageResource = R.drawable.gunes_mountly;
            gunesTimeIcon.setColorFilter(ContextCompat.getColor(this, R.color.current_namaz_time), PorterDuff.Mode.SRC_IN);
            gunesTimeMonthly.setTextColor(ContextCompat.getColor(this, R.color.current_namaz_time));
            gunesTimeLabel.setTextColor(ContextCompat.getColor(this, R.color.current_namaz_time));
        } else if (PrayerNames.OGLE.getValue().equals(NamazVakiti.getInstance().getNextPrayer().getName())) {
            imageResource = R.drawable.ogle_mountly;
            ogleTimeIcon.setColorFilter(ContextCompat.getColor(this, R.color.current_namaz_time), PorterDuff.Mode.SRC_IN);
            ogleTimeMonthly.setTextColor(ContextCompat.getColor(this, R.color.current_namaz_time));
            ogleTimeLabel.setTextColor(ContextCompat.getColor(this, R.color.current_namaz_time));
        } else if (PrayerNames.IKINDI.getValue().equals(NamazVakiti.getInstance().getNextPrayer().getName())) {
            imageResource = R.drawable.ikindi_mountly;
            ikindiTimeIcon.setColorFilter(ContextCompat.getColor(this, R.color.current_namaz_time), PorterDuff.Mode.SRC_IN);
            ikindiTimeMonthly.setTextColor(ContextCompat.getColor(this, R.color.current_namaz_time));
            ikindiTimeLabel.setTextColor(ContextCompat.getColor(this, R.color.current_namaz_time));
        } else if (PrayerNames.AKSAM.getValue().equals(NamazVakiti.getInstance().getNextPrayer().getName())) {
            imageResource = R.drawable.aksam_mountly;
            aksamTimeIcon.setColorFilter(ContextCompat.getColor(this, R.color.current_namaz_time), PorterDuff.Mode.SRC_IN);
            aksamTimeMonthly.setTextColor(ContextCompat.getColor(this, R.color.current_namaz_time));
            aksamTimeLabel.setTextColor(ContextCompat.getColor(this, R.color.current_namaz_time));

        } else if (PrayerNames.YATSI.getValue().equals(NamazVakiti.getInstance().getNextPrayer().getName())) {
            imageResource = R.drawable.yatsi_mountly;
            yatsiTimeIcon.setColorFilter(ContextCompat.getColor(this, R.color.current_namaz_time), PorterDuff.Mode.SRC_IN);
            yatsiTimeMonthly.setTextColor(ContextCompat.getColor(this, R.color.current_namaz_time));
            yatsiTimeLabel.setTextColor(ContextCompat.getColor(this, R.color.current_namaz_time));
        } else {
            imageResource = R.drawable.imsak_mountly;
        }

        prayerTimeImage.setImageResource(imageResource);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && remainingTimeRunnable != null) {
            handler.removeCallbacks(remainingTimeRunnable);
        }
    }
} 