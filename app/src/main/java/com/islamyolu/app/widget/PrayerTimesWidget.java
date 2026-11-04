package com.islamyolu.app.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;

import androidx.core.content.ContextCompat;

import com.islamyolu.app.R;
import com.islamyolu.app.Service.SharedPrefsHelperService;
import com.islamyolu.app.VakitApi.City;
import com.islamyolu.app.prayertimes.CurrentPrayerTimes;
import com.islamyolu.app.prayertimes.PrayerDisplay;
import com.islamyolu.app.prayertimes.PrayerSlot;
import com.islamyolu.app.prayertimes.PrayerTimeline;
import com.islamyolu.app.prayertimes.db.PrayerDayEntity;
import com.islamyolu.app.main.MainActivity;
import com.islamyolu.app.weather.WeatherData;
import com.islamyolu.app.weather.WeatherIcon;
import com.islamyolu.app.weather.WeatherService;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class PrayerTimesWidget extends AppWidgetProvider {
    private static final String ACTION_UPDATE = "com.myapp.yolcu.UPDATE_WIDGET";
    private static final String WEATHER_UPDATE_ACTION = "com.myapp.yolcu.UPDATE_WEATHER";
    private static final long WEATHER_UPDATE_INTERVAL = 3600000; // 1 saat
    public static final String PACKAGE_NAME = "com.islamyolu.app";
    private RemoteViews views;
    private ImageView widget_logo;
    private static final String ACTION_REFRESH_WEATHER = "com.myapp.yolcu.REFRESH_WEATHER";
    private static final String TAG = "PrayerTimesWidget";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        if (action != null) {
            Log.d(TAG, "Received action: " + action);
            if (ACTION_UPDATE.equals(intent.getAction())) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName thisWidget = new ComponentName(context, PrayerTimesWidget.class);
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
                onUpdate(context, appWidgetManager, appWidgetIds);
            } else if (ACTION_REFRESH_WEATHER.equals(action)) {
                Log.d(TAG, "Weather refresh button clicked");
                int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    updateWidgetInBackground(context, widgetId);
                }
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidgetInBackground(context, appWidgetId);
        }
    }

    private void updateWidgetInBackground(Context context, int appWidgetId) {
        try {
            views = new RemoteViews(context.getPackageName(), R.layout.prayer_times_widget);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            // Önce varsayılan değerleri göster
            setDefaultValues(context, views);
            appWidgetManager.updateAppWidget(appWidgetId, views);

            // Go App butonunu tıklayınca uygulamayı başlatacak intent oluşturuluyor
            Intent goAppIntent = context.getPackageManager().getLaunchIntentForPackage(PACKAGE_NAME);
            if (goAppIntent != null) {
                goAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent goAppPendingIntent = PendingIntent.getActivity(
                        context,
                        0,
                        goAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // Go App butonuna tıklama özelliği ekleniyor
                views.setOnClickPendingIntent(R.id.go_app, goAppPendingIntent);
            } else {
                Log.e(TAG, "Uygulama bulunamadı");
            }
            // Vakitler arka planda Room'dan okunur (§5); hava durumu eski il bilgisinden (city_name)
            new Thread(() -> {
                try {
                    CurrentPrayerTimes current = CurrentPrayerTimes.load(context, Instant.now());
                    String weatherCity = new SharedPrefsHelperService(context, MainActivity.SETTINGS_PREFS)
                            .getCityNameFromPreferences();
                    City weatherLocation = weatherCity != null ? City.fromCityName(weatherCity) : null;

                    new Handler(Looper.getMainLooper()).post(() -> {
                        updateWidgetViews(context, views, current, appWidgetManager, appWidgetId);
                        if (weatherLocation != null) {
                            updateWeatherInfo(context, views, weatherLocation.getLatitude(), weatherLocation.getLongitude(), appWidgetManager, appWidgetId);
                        }
                    });

                } catch (RuntimeException e) {
                    Log.e(TAG, "Background update error: " + e.getMessage(), e);
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Widget update error: " + e.getMessage());
        }
    }

    private void updateWidgetViews(Context context, RemoteViews views, CurrentPrayerTimes current,
                                   AppWidgetManager appWidgetManager, int appWidgetId) {
        try {
            int transparency = WidgetSettingsActivity.loadTransparencyPref(context, appWidgetId);
            // Şeffaflık alpha hesapla
            int alpha = 255 - (transparency * 255 / 100);
            int baseColor = ContextCompat.getColor(context, R.color.widget_background);
            int backgroundColor = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
            views.setInt(R.id.widget_layout, "setBackgroundColor", backgroundColor);

            Instant now = Instant.now();
            PrayerDayEntity today = current.timeline.today(now);
            PrayerTimeline.Moment next = current.timeline.next(now);
            views.setTextViewText(R.id.widget_city_name, current.selection != null
                    ? current.selection.getCityName() : context.getString(R.string.prayer_choose_city));
            views.setTextViewText(R.id.widget_hijri_date, PrayerDisplay.hijri(context, today != null ? today.getHijri() : null));

            resetAllPanelsAndTexts(views, context);
            if (today != null && next != null) {
                // Sonraki vakit ve kalan süre (şehrin gmt farkıyla)
                views.setTextViewText(R.id.widget_next_prayer_label, context.getString(
                        R.string.widget_next_prayer_label, context.getString(PrayerDisplay.nameRes(next.slot))));
                views.setTextViewText(R.id.widget_next_prayer_time,
                        PrayerDisplay.remaining(Duration.between(now, next.instant), false));
                updateActivePanelAndTexts(views, next.slot, context);
            } else {
                // Şehir seçilmemiş veya veri bitmiş (§6)
                views.setTextViewText(R.id.widget_next_prayer_label, context.getString(current.selection == null
                        ? R.string.prayer_status_no_city : R.string.prayer_status_outdated_offline));
                views.setTextViewText(R.id.widget_next_prayer_time, "");
            }

            views.setTextViewText(R.id.widget_imsak_time, timeOf(today, PrayerSlot.IMSAK));
            views.setTextViewText(R.id.widget_gunes_time, timeOf(today, PrayerSlot.GUNES));
            views.setTextViewText(R.id.widget_ogle_time, timeOf(today, PrayerSlot.OGLE));
            views.setTextViewText(R.id.widget_ikindi_time, timeOf(today, PrayerSlot.IKINDI));
            views.setTextViewText(R.id.widget_aksam_time, timeOf(today, PrayerSlot.AKSAM));
            views.setTextViewText(R.id.widget_yatsi_time, timeOf(today, PrayerSlot.YATSI));

            appWidgetManager.updateAppWidget(appWidgetId, views);
            scheduleNextUpdate(context);
        } catch (RuntimeException e) {
            Log.e(TAG, "Widget view update error: " + e.getMessage(), e);
        }
    }

    private static String timeOf(PrayerDayEntity today, PrayerSlot slot) {
        return today != null ? PrayerDisplay.time(slot.timeOf(today)) : PrayerDisplay.EMPTY_TIME;
    }


    private void resetAllPanelsAndTexts(RemoteViews views, Context context) {
        // Panel arkaplanlarını sıfırla
        views.setInt(R.id.panelImsak, "setBackgroundResource", android.R.color.transparent);
        views.setInt(R.id.panelGunes, "setBackgroundResource", android.R.color.transparent);
        views.setInt(R.id.panelOgle, "setBackgroundResource", android.R.color.transparent);
        views.setInt(R.id.panelIkindi, "setBackgroundResource", android.R.color.transparent);
        views.setInt(R.id.panelAksam, "setBackgroundResource", android.R.color.transparent);
        views.setInt(R.id.panelYatsi, "setBackgroundResource", android.R.color.transparent);

        // Text renklerini varsayılana çevir
        int defaultTextColor = context.getResources().getColor(R.color.widget_text_color, context.getTheme());
        views.setTextColor(R.id.widget_imsak, defaultTextColor);
        views.setTextColor(R.id.widget_imsak_time, defaultTextColor);
        views.setTextColor(R.id.widget_gunes, defaultTextColor);
        views.setTextColor(R.id.widget_gunes_time, defaultTextColor);
        views.setTextColor(R.id.widget_ogle, defaultTextColor);
        views.setTextColor(R.id.widget_ogle_time, defaultTextColor);
        views.setTextColor(R.id.widget_ikindi, defaultTextColor);
        views.setTextColor(R.id.widget_ikindi_time, defaultTextColor);
        views.setTextColor(R.id.widget_aksam, defaultTextColor);
        views.setTextColor(R.id.widget_aksam_time, defaultTextColor);
        views.setTextColor(R.id.widget_yatsi, defaultTextColor);
        views.setTextColor(R.id.widget_yatsi_time, defaultTextColor);
    }

    private void updateActivePanelAndTexts(RemoteViews views, PrayerSlot vakit, Context context) {
        resetAllPanelsAndTexts(views, context);

        int panelId = getPanelIdForVakit(vakit);
        int labelId = getLabelIdForVakit(vakit);
        int timeId = getTimeIdForVakit(vakit);

        if (panelId != 0) {
            // Panel arka planını değiştir
            views.setInt(panelId, "setBackgroundResource", R.drawable.bg_prayer_widget);

            // Text renklerini beyaz yap
            views.setTextColor(labelId, ContextCompat.getColor(context, R.color.current_prayer_widget_label));
            views.setTextColor(timeId, ContextCompat.getColor(context, R.color.prayer_next_time_wigdet));
        }

    }

    private int getLabelIdForVakit(PrayerSlot vakit) {
        switch (vakit) {
            case IMSAK:
                return R.id.widget_imsak;
            case GUNES:
                return R.id.widget_gunes;
            case OGLE:
                return R.id.widget_ogle;
            case IKINDI:
                return R.id.widget_ikindi;
            case AKSAM:
                return R.id.widget_aksam;
            case YATSI:
                return R.id.widget_yatsi;
            default:
                return 0;
        }
    }

    private int getTimeIdForVakit(PrayerSlot vakit) {
        switch (vakit) {
            case IMSAK:
                return R.id.widget_imsak_time;
            case GUNES:
                return R.id.widget_gunes_time;
            case OGLE:
                return R.id.widget_ogle_time;
            case IKINDI:
                return R.id.widget_ikindi_time;
            case AKSAM:
                return R.id.widget_aksam_time;
            case YATSI:
                return R.id.widget_yatsi_time;
            default:
                return 0;
        }
    }

    public void scheduleNextUpdate(Context context) {
        Intent intent = new Intent(context, PrayerTimesWidget.class);
        intent.setAction(ACTION_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // 1 dakika (60.000 milisaniye) sonrasını hesapla
        long triggerTime = System.currentTimeMillis() + 60000;
        alarmManager.set(AlarmManager.RTC, triggerTime, pendingIntent);

    }




    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        scheduleNextUpdate(context);
        views = new RemoteViews(context.getPackageName(), R.layout.prayer_times_widget);
        resetAllPanelsAndTexts(views, context);
        Log.d(TAG, "Widget enabled");

        SharedPreferences prefs = context.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("last_update", 0);
        editor.apply();
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Intent intent = new Intent(context, PrayerTimesWidget.class);
        intent.setAction(ACTION_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        Log.d("PrayerTimesWidget", "Widget disabled");
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        Log.d("PrayerTimesWidget", "Widget deleted");
        // Widget silindiğinde servisi etkileme
    }

    private void setDefaultValues(Context context, RemoteViews views) {
        views.setTextViewText(R.id.widget_city_name, context.getString(R.string.widget_loading));
        views.setTextViewText(R.id.widget_imsak_time, PrayerDisplay.EMPTY_TIME);
        views.setTextViewText(R.id.widget_gunes_time, PrayerDisplay.EMPTY_TIME);
        views.setTextViewText(R.id.widget_ogle_time, PrayerDisplay.EMPTY_TIME);
        views.setTextViewText(R.id.widget_ikindi_time, PrayerDisplay.EMPTY_TIME);
        views.setTextViewText(R.id.widget_aksam_time, PrayerDisplay.EMPTY_TIME);
        views.setTextViewText(R.id.widget_yatsi_time, PrayerDisplay.EMPTY_TIME);
    }

    private int getPanelIdForVakit(PrayerSlot vakit) {
        switch (vakit) {
            case IMSAK:
                return R.id.panelImsak;
            case GUNES:
                return R.id.panelGunes;
            case OGLE:
                return R.id.panelOgle;
            case IKINDI:
                return R.id.panelIkindi;
            case AKSAM:
                return R.id.panelAksam;
            case YATSI:
                return R.id.panelYatsi;
            default:
                return 0;
        }
    }

    public void scheduleWeatherUpdate(Context context) {
        Intent intent = new Intent(context, PrayerTimesWidget.class);
        intent.setAction(WEATHER_UPDATE_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                1, // Farklı request code kullanıyoruz
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long triggerTime = System.currentTimeMillis() + WEATHER_UPDATE_INTERVAL;

        if (alarmManager != null) {
            alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC,
                    triggerTime,
                    pendingIntent
            );
            Log.d("PrayerTimesWidget", "Hava durumu güncellemesi planlandı: " + new Date(triggerTime));
        }
    }


    private void updateWeatherInfo(Context context, RemoteViews views, double latitude, double longitude,
                                   AppWidgetManager appWidgetManager, int appWidgetId) {
        if (!isNetworkAvailable(context)) {
            Log.w(TAG, "İnternet bağlantısı yok");
            showRefreshButton(views, context, appWidgetId);
            appWidgetManager.updateAppWidget(appWidgetId, views);
            return;
        }

        WeatherService weatherService = new WeatherService(context);
        weatherService.getDailyWeatherAndForecastByDate(latitude, longitude, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherData weatherData) {
                if (weatherData.getCurrentWeather() != null) {
                    double temp = weatherData.getCurrentWeather().getTemperature();
                    int roundedTemp = (temp % 1.0 >= 0.5) ? (int) Math.ceil(temp) : (int) Math.floor(temp);

                    String weatherDesc = weatherData.getWeatherDescription();
                    views.setTextViewText(R.id.weather_temp,
                            String.format(Locale.getDefault(), "%d°C", roundedTemp));

                    views.setTextViewText(R.id.weather_desc, weatherDesc);

                    // Eğer hava durumu "Bilinmiyor" ise refresh butonunu göster
                    if ("Bilinmiyor".equals(weatherDesc)) {
                        Log.e(TAG, "Hava durumu güncellenemedi weatherDesc Bilinmiyor");
                        showRefreshButton(views, context, appWidgetId);
                        return;
                    } else {
                        views.setViewVisibility(R.id.weather_wifi_icon, View.GONE);
                        views.setViewVisibility(R.id.weather_refresh_icon, View.GONE);
                    }

                    views.setImageViewResource(R.id.weather_icon,
                            WeatherIcon.getWeatherIconResource(
                                    weatherData.getCurrentWeather().getWeatherCode(),
                                    weatherData.getCurrentWeather().isDay()
                            )
                    );
                } else {
                    Log.e(TAG, "Hava durumu güncellenemedi: getCurrentWeather null");
                    showRefreshButton(views, context, appWidgetId);
                }
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Hava durumu güncellenemedi: " + error);
                showRefreshButton(views, context, appWidgetId);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }
        });
    }

    // Refresh butonunu göstermek için yardımcı metod
    private void showRefreshButton(RemoteViews views, Context context, int appWidgetId) {
        views.setViewVisibility(R.id.weather_wifi_icon, View.VISIBLE);
        views.setViewVisibility(R.id.weather_refresh_icon, View.VISIBLE);

        // Refresh butonu için intent oluştur
        Intent refreshIntent = new Intent(context, PrayerTimesWidget.class);
        refreshIntent.setAction(ACTION_REFRESH_WEATHER);
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Refresh butonuna tıklama özelliği ekle
        views.setOnClickPendingIntent(R.id.weather_refresh_icon, refreshPendingIntent);
        Log.d(TAG, "Refresh button shown and click listener set");
    }

    // İnternet bağlantısı kontrolü için yardımcı metod
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager
                    .getNetworkCapabilities(connectivityManager.getActiveNetwork());

            if (capabilities != null) {
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }
        }
        return false;
    }
}


