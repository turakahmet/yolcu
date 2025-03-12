package com.myapp.yolcu.ui.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;

import com.myapp.yolcu.R;
import com.myapp.yolcu.ui.Service.SharedPrefsHelperService;
import com.myapp.yolcu.ui.VakitApi.City;
import com.myapp.yolcu.ui.VakitApi.NamazVakiti;
import com.myapp.yolcu.ui.VakitApi.VakitNoApi;
import com.myapp.yolcu.ui.main.MainActivity;

import java.util.Calendar;
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
            }else if (ACTION_REFRESH_WEATHER.equals(action)) {
                Log.d(TAG, "Weather refresh button clicked");
                
                // Widget ID'sini al
                int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 
                    AppWidgetManager.INVALID_APPWIDGET_ID);
                
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    // Sadece tıklanan widget'ı güncelle
                    updateWidgetInBackground(context, widgetId);
                } else {
                    // Tüm widget'ları güncelle
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                    ComponentName thisWidget = new ComponentName(context, PrayerTimesWidget.class);
                    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
                    onUpdate(context, appWidgetManager, appWidgetIds);
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
                    Log.e(TAG, "Background update error: " + e.getMessage());
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Widget update error: " + e.getMessage());
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
            Log.e(TAG, "Widget view update error: " + e.getMessage());
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

        // WeatherPrefs'i sıfırla (son güncelleme zamanını 0 yap)
        SharedPreferences prefs = context.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("last_update", 0); // 0 yaparak hemen güncelleme yapmasını sağla
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
        CLEAR_DAY(R.drawable.day_clear, new int[]{0}, true),
        CLEAR_NIGHT(R.drawable.night_clear, new int[]{0}, false),

        FEW_CLOUDS_DAY(R.drawable.day_few_clouds, new int[]{1}, true),
        FEW_CLOUDS_NIGHT(R.drawable.night_few_clouds, new int[]{1}, false),

        PARTLY_CLOUDY_DAY(R.drawable.day_partly_cloudy, new int[]{2}, true),
        PARTLY_CLOUDY_NIGHT(R.drawable.night_partly_cloudy, new int[]{2}, false),

        CLOUDY(R.drawable.cloudy, new int[]{3,5,19}, true),
        CLOUDY_NIGHT(R.drawable.night_few_clouds, new int[]{3,5,19}, false),

        SANDSTORM(R.drawable.sandstorm, new int[]{8,9,30,31,32,33,34,35,36,37,38,39}, true),
        SANDSTORM_NIGHT(R.drawable.sandstorm, new int[]{8,9,30,31,32,33,34,35,36,37,38,39}, false),

        FOG(R.drawable.fog, new int[]{10,11,12,28,40,41,42,43,44,45}, true),
        FOG_NIGHT(R.drawable.fog, new int[]{10,11,12,28,40,41,42,43,44,45}, false),

        HEAVY_FOG(R.drawable.heavy_fog, new int[]{46,47,48,49}, true),
        HEAVY_FOG_NIGHT(R.drawable.heavy_fog, new int[]{46,47,48,49}, false),

        DRIZZLE(R.drawable.drizzle_light, new int[]{20,50,52,54,59,58,57,56,60,62,64}, true),
        DRIZZLE_NIGHT(R.drawable.drizzle_light, new int[]{20,50,52,54,59,58,57,56,60,62,64}, false),

        RAIN_LIGHT(R.drawable.rain_light, new int[]{21,51,53,55,61, 63,80,91,92}, true),
        RAIN_LIGHT_NIGHT(R.drawable.night_rain, new int[]{61, 63,92}, false),

        RAIN_HEAVY(R.drawable.rain_heavy, new int[]{25,65,81,82}, true),
        RAIN_HEAVY_NIGHT(R.drawable.rain_heavy, new int[]{25,65,81,82}, false),

        SNOW_LIGHT(R.drawable.snow_light, new int[]{22,70,71,72, 73, 74, 75, 77, 85,86}, true),
        SNOW_LIGHT_NIGHT(R.drawable.night_snow, new int[]{22,70,71,72, 73, 74, 75, 77,85,86}, false),
        SNOW_MIXED(R.drawable.snow_mixed, new int[]{23,26,27,66,67,68,69,83,84,87,88,89,90,93,94}, true),
        SNOW_MIXED_NIGHT(R.drawable.snow_mixed_night, new int[]{23,26,27,66,67,68,69,83,84,87,88,89,90,93,94}, false),
        SNOW_CRYSTAL(R.drawable.snow_crystals, new int[]{24,76,78,79}, false),

        THUNDERSTORM(R.drawable.thunderstorm_light, new int[]{17,18,13,29,98,99 }, true),
        THUNDERSTORM_HEAVY(R.drawable.thunderstorm_heavy, new int[]{95,96,97}, true),
        UNKNOWN(R.drawable.unknown_weather, new int[]{-1}, true);

        private final int resourceId;
        private final int[] weatherCodes;
        private final boolean isDay;

        WeatherIcon(int resourceId, int[] weatherCodes, boolean isDay) {
            this.resourceId = resourceId;
            this.weatherCodes = weatherCodes;
            this.isDay = isDay;
        }

        public int getResourceId() {
            return resourceId;
        }

        public static int getWeatherIconResource(int weatherCode, boolean isDay) {
            for (WeatherIcon icon : WeatherIcon.values()) {
                for (int code : icon.weatherCodes) {
                    if (code == weatherCode && (icon.isDay == isDay || weatherCode >= 3)) {
                        return icon.getResourceId();
                    }
                }
            }
            return UNKNOWN.getResourceId();
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
        weatherService.getWeatherData(latitude, longitude, new WeatherService.WeatherCallback() {
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
                        Log.e(TAG, "Hava durumu güncellenemedi weatherDesc Bilinmiyor" );
                        showRefreshButton(views, context, appWidgetId);
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
                    Log.e(TAG, "Hava durumu güncellenemedi: getCurrentWeather null" );
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


