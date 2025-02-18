package com.myapp.yolcu.ui.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RemoteViews;

import com.myapp.yolcu.R;
import com.myapp.yolcu.ui.Service.SharedPrefsHelperService;
import com.myapp.yolcu.ui.VakitApi.City;
import com.myapp.yolcu.ui.VakitApi.NamazVakiti;
import com.myapp.yolcu.ui.VakitApi.VakitNoApi;
import com.myapp.yolcu.ui.main.MainActivity;

import java.util.Date;

import android.os.Handler;
import android.os.Looper;
import android.app.AlarmManager;
import android.app.PendingIntent;

import java.util.Locale;
import java.util.Objects;

import android.graphics.Color;

import androidx.core.content.ContextCompat;

import com.myapp.yolcu.ui.weather.WeatherData;
import com.myapp.yolcu.ui.weather.WeatherService;

public class PrayerTimesWidget extends AppWidgetProvider {
    private static final String ACTION_UPDATE = "com.myapp.yolcu.UPDATE_WIDGET";
    private static final String WEATHER_UPDATE_ACTION = "com.myapp.yolcu.UPDATE_WEATHER";
    private static final long WEATHER_UPDATE_INTERVAL = 3600000; // 1 saat
    private final VakitNoApi vakitNoApi = new VakitNoApi();
    private RemoteViews views;
    private ImageView widget_logo;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_UPDATE.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, PrayerTimesWidget.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
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
            setDefaultValues(views);
            appWidgetManager.updateAppWidget(appWidgetId, views);

            // Arka planda vakitleri yükle
            new Thread(() -> {
                try {
                    SharedPrefsHelperService sharedPrefs = new SharedPrefsHelperService(context, MainActivity.SETTINGS_PREFS);
                    String cityName = sharedPrefs.getCityNameFromPreferences();

                    NamazVakiti namazVakiti = vakitNoApi.localPrayerTimes(Objects.requireNonNull(City.fromCityName(cityName)).getLatitude(), Objects.requireNonNull(City.fromCityName(cityName)).getLongitude());

                    new Handler(Looper.getMainLooper()).post(() -> {
                        updateWidgetViews(context, views, namazVakiti, cityName, appWidgetManager, appWidgetId);
                        updateWeatherInfo(context, views, Objects.requireNonNull(City.fromCityName(cityName)).getLatitude(), Objects.requireNonNull(City.fromCityName(cityName)).getLongitude(), appWidgetManager, appWidgetId);
                    });

                } catch (Exception e) {
                    Log.e("PrayerTimesWidget", "Background update error: " + e.getMessage());
                }
            }).start();

        } catch (Exception e) {
            Log.e("PrayerTimesWidget", "Widget update error: " + e.getMessage());
        }
    }

    private void updateWidgetViews(Context context, RemoteViews views, NamazVakiti namazVakiti,
                                   String cityName, AppWidgetManager appWidgetManager, int appWidgetId) {
        try {
            views.setTextViewText(R.id.widget_city_name, cityName);
            views.setTextViewText(R.id.widget_hijri_date, namazVakiti.getHicriDate());

//            // Context'i resetAllPanelsAndTexts metoduna geçir
            String remainingTime = namazVakiti.getNextPrayerTimeRemainingTimeFormatted(false);
//            Log.d("resetAllPanelsAndTexts", "Pane

            // Sonraki vakit bilgilerini göster
            if (namazVakiti.getGecerliVakit() != null) {
                String nextPrayerName = getVakitDisplayName(namazVakiti.getGecerliVakit());

                views.setTextViewText(R.id.widget_next_prayer_label, "~"+nextPrayerName + " Vaktine~");
                views.setTextViewText(R.id.widget_next_prayer_time, remainingTime);

                // Yaklaşan vaktin panel ve text renklerini güncelle
                    updateActivePanelAndTexts(views, namazVakiti.getGecerliVakit(), context);
            }

            // Diğer vakitleri güncelle
            views.setTextViewText(R.id.widget_imsak_time, namazVakiti.getImsak());
            views.setTextViewText(R.id.widget_gunes_time, namazVakiti.getGunes());
            views.setTextViewText(R.id.widget_ogle_time, namazVakiti.getOgle());
            views.setTextViewText(R.id.widget_ikindi_time, namazVakiti.getIkindi());
            views.setTextViewText(R.id.widget_aksam_time, namazVakiti.getAksam());
            views.setTextViewText(R.id.widget_yatsi_time, namazVakiti.getYatsi());

            appWidgetManager.updateAppWidget(appWidgetId, views);
            scheduleNextUpdate(context);
        } catch (Exception e) {
            Log.e("PrayerTimesWidget", "Widget view update error: " + e.getMessage());
        }
    }



    private void resetAllPanelsAndTexts(RemoteViews views,Context context) {
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

    private void updateActivePanelAndTexts(RemoteViews views, NamazVakiti.GecerliVakit vakit, Context context) {
        resetAllPanelsAndTexts(views, context);

        int panelId = getPanelIdForVakit(vakit);
        int labelId = getLabelIdForVakit(vakit);
        int timeId = getTimeIdForVakit(vakit);

        if (panelId != 0) {
            // Panel arka planını değiştir
            views.setInt(panelId, "setBackgroundResource", R.drawable.bg_prayer_widget);

            // Text renklerini beyaz yap
            views.setTextColor(labelId, Color.WHITE);
            views.setTextColor(timeId, ContextCompat.getColor(context, R.color.prayer_next_time_wigdet));
        }

    }

    private int getLabelIdForVakit(NamazVakiti.GecerliVakit vakit) {
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

    private int getTimeIdForVakit(NamazVakiti.GecerliVakit vakit) {
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




    private String getVakitDisplayName(NamazVakiti.GecerliVakit vakit) {
        switch (vakit) {
            case IMSAK:
                return "İmsak";
            case GUNES:
                return "Güneş";
            case OGLE:
                return "Öğle";
            case IKINDI:
                return "İkindi";
            case AKSAM:
                return "Akşam";
            case YATSI:
                return "Yatsı";
            default:
                return "Bilinmiyor";
        }
    }


    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        scheduleNextUpdate(context);
        views = new RemoteViews(context.getPackageName(), R.layout.prayer_times_widget);
        // Önce tüm panelleri sıfırla
        resetAllPanelsAndTexts(views, context);
        Log.d("PrayerTimesWidget", "Widget enabled");
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

    private void setDefaultValues(RemoteViews views) {
        views.setTextViewText(R.id.widget_city_name, "Yükleniyor...");
        views.setTextViewText(R.id.widget_imsak_time, "--:--");
        views.setTextViewText(R.id.widget_gunes_time, "--:--");
        views.setTextViewText(R.id.widget_ogle_time, "--:--");
        views.setTextViewText(R.id.widget_ikindi_time, "--:--");
        views.setTextViewText(R.id.widget_aksam_time, "--:--");
        views.setTextViewText(R.id.widget_yatsi_time, "--:--");
    }

    private int getPanelIdForVakit(NamazVakiti.GecerliVakit vakit) {
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

    private enum WeatherIcon {
        CLEAR_DAY(R.drawable.day_clear, 0, true),
        CLEAR_NIGHT(R.drawable.night_clear, 0, false),
        FEW_CLOUDS_DAY(R.drawable.day_few_clouds, 1, true),
        FEW_CLOUDS_NIGHT(R.drawable.night_few_clouds, 1, false),
        PARTLY_CLOUDY_DAY(R.drawable.day_partly_cloudy, 2, true),
        PARTLY_CLOUDY_NIGHT(R.drawable.night_partly_cloudy, 2, false),
        CLOUDY(R.drawable.cloudy, 3, true), // Gündüz/gece aynı ikon
        FOG(R.drawable.fog, 45, true),
        HEAVY_FOG(R.drawable.heavy_fog, 48, true),
        DRIZZLE_LIGHT(R.drawable.drizzle_light, 51, true),
        DRIZZLE_MEDIUM(R.drawable.drizzle_light, 53, true),
        DRIZZLE_HEAVY(R.drawable.drizzle_light, 55, true),
        RAIN_LIGHT(R.drawable.rain_light, 61, true),
        RAIN_MEDIUM(R.drawable.rain_light, 63, true),
        RAIN_HEAVY(R.drawable.rain_heavy, 65, true),
        SNOW_LIGHT(R.drawable.snow_light, 71, true),
        SNOW_MEDIUM(R.drawable.snow_light, 73, true),
        SNOW_HEAVY(R.drawable.snow_light, 75, true),
        THUNDERSTORM_LIGHT(R.drawable.thunderstorm_light, 95, true),
        THUNDERSTORM_HAIL(R.drawable.thunderstorm_hail, 96, true),
        THUNDERSTORM_HEAVY(R.drawable.thunderstorm_heavy, 99, true),
        UNKNOWN(R.drawable.unknown_weather, -1, true);

        private final int resourceId;
        private final int weatherCode;
        private final boolean isDay;

        WeatherIcon(int resourceId, int weatherCode, boolean isDay) {
            this.resourceId = resourceId;
            this.weatherCode = weatherCode;
            this.isDay = isDay;
        }

        public int getResourceId() {
            return resourceId;
        }

        public static int getWeatherIconResource(int weatherCode, boolean isDay) {
            for (WeatherIcon icon : WeatherIcon.values()) {
                if (icon.weatherCode == weatherCode && (icon.isDay == isDay || weatherCode >= 3)) {
                    return icon.getResourceId();
                }
            }
            return UNKNOWN.getResourceId();
        }
    }

    private void updateWeatherInfo(Context context, RemoteViews views, double latitude, double longitude, AppWidgetManager appWidgetManager, int appWidgetId) {
        WeatherService weatherService = new WeatherService();
        weatherService.getWeatherData(latitude, longitude, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherData weatherData) {
                if (weatherData.getCurrentWeather() != null) {
                    views.setTextViewText(R.id.weather_temp,
                        String.format(Locale.getDefault(), "%.1f°C", 
                        weatherData.getCurrentWeather().getTemperature()));
                    
                    views.setTextViewText(R.id.weather_desc, 
                        weatherData.getWeatherDescription());
                    
                    views.setImageViewResource(R.id.weather_icon, 
                        WeatherIcon.getWeatherIconResource(
                            weatherData.getCurrentWeather().getWeatherCode(),
                            weatherData.getCurrentWeather().isDay()
                        )
                    );
                    appWidgetManager.updateAppWidget(appWidgetId, views);
                    scheduleWeatherUpdate(context);
                    Log.d("PrayerTimesWidget", "Hava durumu güncellendi: " + weatherData.getWeatherDescription());
                }
            }

            @Override
            public void onError(String error) {
                Log.e("PrayerTimesWidget", "Hava durumu güncellenemedi: " + error);
                views.setImageViewResource(R.id.weather_icon, WeatherIcon.UNKNOWN.getResourceId());
            }
        });
    }
}


