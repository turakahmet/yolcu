package com.islamyolu.app.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.islamyolu.app.R;
import com.islamyolu.app.Service.NetworkServiceUtil;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.VakitApi.City;
import com.islamyolu.app.prayertimes.CurrentPrayerTimes;
import com.islamyolu.app.prayertimes.PrayerClock;
import com.islamyolu.app.prayertimes.PrayerCountdown;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Namaz vakti widget'ı. Vakitler Repository üzerinden, arka plan thread'inde Room'dan okunur (§5).
 * <p>
 * Marka bağımsız çalışma için:
 * <ul>
 *   <li>Kalan süre saniyesiz "SS:DD" düz metindir; gösterilen dakika değiştiğinde yeniden çizilir.
 *       Uyandırmayan (RTC) alarm kullanılır: ekran kapalıyken çalışmaz, pil harcamaz; ekran açılınca
 *       hemen güncellenir. Düz metin gecikmede eksiye düşmez.</li>
 *   <li>Şehrin gece yarısında da yeniden çizilir; senkron/şehir değişiminde
 *       PrayerTimesUpdateNotifier tetikler.</li>
 *   <li>Arka plan işi goAsync ile yapılır; süreç iş bitmeden kapatılmaz.</li>
 *   <li>Boyuta göre düzen: küçükken yalnızca kalan süre, büyükken tüm bilgiler.</li>
 * </ul>
 */
public class PrayerTimesWidget extends AppWidgetProvider {
    private static final String ACTION_UPDATE = "com.myapp.yolcu.UPDATE_WIDGET";
    private static final String WEATHER_UPDATE_ACTION = "com.myapp.yolcu.UPDATE_WEATHER";
    private static final long WEATHER_UPDATE_INTERVAL = 3600000; // 1 saat
    public static final String PACKAGE_NAME = "com.islamyolu.app";
    private static final String ACTION_REFRESH_WEATHER = "com.myapp.yolcu.REFRESH_WEATHER";
    private static final String TAG = "PrayerTimesWidget";

    /** Bundan küçük widget'ta yalnızca kalan süre gösterilir (≈ ekranın 1/3'ü) */
    private static final int FULL_LAYOUT_MIN_WIDTH_DP = 220;
    private static final int FULL_LAYOUT_MIN_HEIGHT_DP = 100;
    /** Veri yokken (şehir seçilmemiş/veri bitmiş) yeniden deneme aralığı */
    private static final Duration NO_DATA_RETRY = Duration.ofMinutes(30);
    /** Dakika/vakit sınırından sonra yeniden çizim payı */
    private static final long REDRAW_AFTER_MS = 300;
    /** Exact alarm izni yoksa kabul edilen gecikme penceresi (sistem en az 10 dk'ya uzatabilir) */
    private static final long INEXACT_WINDOW_MS = 60_000;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action = intent.getAction();
        if (action != null) {
            Log.d(TAG, "Received action: " + action);
            if (ACTION_UPDATE.equals(action) || WEATHER_UPDATE_ACTION.equals(action)
                    || Intent.ACTION_TIME_CHANGED.equals(action) || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
                // Saat/saat dilimi değişince kronometre tabanı yeniden hesaplanmalı (kronometre
                // cihazın açık kalma süresine göre sayar, duvar saatine göre değil)
                updateWidgets(context, allWidgetIds(context));
            } else if (ACTION_REFRESH_WEATHER.equals(action)) {
                Log.d(TAG, "Weather refresh button clicked");
                int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
                if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    updateWidgets(context, new int[]{widgetId});
                }
            }
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateWidgets(context, appWidgetIds);
    }

    /** Kullanıcı widget'ı büyütüp küçültünce uygun düzen seçilir. */
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        updateWidgets(context, new int[]{appWidgetId});
    }

    private static int[] allWidgetIds(Context context) {
        return AppWidgetManager.getInstance(context)
                .getAppWidgetIds(new ComponentName(context, PrayerTimesWidget.class));
    }

    /** Vakitleri bir kez Room'dan okur, tüm widget'ları çizer ve bir sonraki güncellemeyi planlar. */
    private void updateWidgets(Context context, int[] appWidgetIds) {
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            return;
        }
        PendingResult pendingResult = goAsync();
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                Instant now = Instant.now();
                CurrentPrayerTimes current = CurrentPrayerTimes.load(app, now);
                City weatherLocation = weatherLocation(app);
                AppWidgetManager manager = AppWidgetManager.getInstance(app);
                for (int appWidgetId : appWidgetIds) {
                    boolean full = isFullSize(manager.getAppWidgetOptions(appWidgetId));
                    RemoteViews views = buildViews(app, appWidgetId, current, now, full);
                    manager.updateAppWidget(appWidgetId, views);
                    if (full && weatherLocation != null) {
                        updateWeatherInfo(app, weatherLocation.getLatitude(), weatherLocation.getLongitude(), manager, appWidgetId);
                    }
                }
                scheduleUpdateAt(app, nextRedrawAt(current, now));
            } catch (RuntimeException e) {
                Log.e(TAG, "Widget update error: " + e.getMessage(), e);
            } finally {
                pendingResult.finish();
            }
        });
    }

    /** Hava durumu eski il bilgisinden (city_name; Ayarlar > Güncelle) */
    @Nullable
    private static City weatherLocation(Context context) {
        String weatherCity = AppPreferences.from(context).location().getLegacyCityName();
        return weatherCity != null ? City.fromCityName(weatherCity) : null;
    }

    /** Başlatıcının bildirdiği boyuttan (dikey konum: en az genişlik, en çok yükseklik) düzen seçimi */
    private static boolean isFullSize(@Nullable Bundle options) {
        if (options == null) {
            return true;
        }
        int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);
        if (width == 0 || height == 0) {
            return true; // bazı başlatıcılar boyut bildirmez: tam düzen
        }
        return width >= FULL_LAYOUT_MIN_WIDTH_DP && height >= FULL_LAYOUT_MIN_HEIGHT_DP;
    }

    private RemoteViews buildViews(Context context, int appWidgetId, CurrentPrayerTimes current,
                                   Instant now, boolean full) {
        RemoteViews views = new RemoteViews(context.getPackageName(),
                full ? R.layout.prayer_times_widget : R.layout.prayer_times_widget_small);

        int transparency = WidgetSettingsActivity.loadTransparencyPref(context, appWidgetId);
        // Şeffaflık alpha hesapla
        int alpha = 255 - (transparency * 255 / 100);
        int baseColor = ContextCompat.getColor(context, R.color.widget_background);
        int backgroundColor = Color.argb(alpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
        views.setInt(R.id.widget_layout, "setBackgroundColor", backgroundColor);

        // Widget'a dokununca uygulama açılır (küçük düzende tamamına, büyükte ok simgesine)
        PendingIntent openApp = openAppIntent(context);
        if (openApp != null) {
            views.setOnClickPendingIntent(full ? R.id.go_app : R.id.widget_layout, openApp);
        }

        PrayerDayEntity today = current.timeline.today(now);
        PrayerCountdown countdown = PrayerCountdown.of(current.timeline, now);
        PrayerTimeline.Moment next = countdown != null ? countdown.next : null;
        views.setTextViewText(R.id.widget_city_name, current.selection != null
                ? current.selection.getCityName() : context.getString(R.string.prayer_choose_city));

        if (today != null && next != null) {
            // Sıradaki vakte kalan süre, saniyesiz "SS:DD" (uygulama ve bildirimle aynı hesap).
            // Düz metin olduğu için gecikmede bile eksiye düşmez; her dakika başında yeniden çizilir.
            views.setTextViewText(R.id.widget_next_prayer_label, context.getString(
                    R.string.widget_next_prayer_label, context.getString(PrayerDisplay.nameRes(next.slot))));
            views.setTextViewText(R.id.widget_next_prayer_time, countdown.textWithoutSeconds());
        } else {
            // Şehir seçilmemiş veya veri bitmiş (§6)
            views.setTextViewText(R.id.widget_next_prayer_label, context.getString(current.selection == null
                    ? R.string.prayer_status_no_city : R.string.prayer_status_outdated_offline));
            views.setTextViewText(R.id.widget_next_prayer_time, "");
        }

        if (full) {
            views.setTextViewText(R.id.widget_hijri_date, PrayerDisplay.hijri(context, today != null ? today.getHijri() : null));
            resetAllPanelsAndTexts(views, context);
            if (today != null && next != null) {
                updateActivePanelAndTexts(views, next.slot, context);
            }
            views.setTextViewText(R.id.widget_imsak_time, timeOf(today, PrayerSlot.IMSAK));
            views.setTextViewText(R.id.widget_gunes_time, timeOf(today, PrayerSlot.GUNES));
            views.setTextViewText(R.id.widget_ogle_time, timeOf(today, PrayerSlot.OGLE));
            views.setTextViewText(R.id.widget_ikindi_time, timeOf(today, PrayerSlot.IKINDI));
            views.setTextViewText(R.id.widget_aksam_time, timeOf(today, PrayerSlot.AKSAM));
            views.setTextViewText(R.id.widget_yatsi_time, timeOf(today, PrayerSlot.YATSI));
        }
        return views;
    }

    @Nullable
    private static PendingIntent openAppIntent(Context context) {
        Intent goAppIntent = context.getPackageManager().getLaunchIntentForPackage(PACKAGE_NAME);
        if (goAppIntent == null) {
            Log.e(TAG, "Uygulama bulunamadı");
            return null;
        }
        goAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, 0, goAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Bir sonraki yeniden çizim: gösterilen dakikanın değiştiği an (sıradaki vakit anı da bir dakika
     * sınırıdır) ya da şehrin gece yarısı (yeni günün vakitleri), hangisi önceyse.
     * Veri yoksa 30 dakika sonra yeniden denenir.
     */
    private static Instant nextRedrawAt(CurrentPrayerTimes current, Instant now) {
        PrayerCountdown countdown = PrayerCountdown.of(current.timeline, now);
        LocalDate today = current.timeline.cityToday(now);
        PrayerDayEntity todayData = today != null ? current.timeline.day(today) : null;
        if (countdown == null || todayData == null) {
            return now.plus(NO_DATA_RETRY);
        }
        Instant minuteTick = now.plusMillis(countdown.millisUntilMinuteTick());
        Instant midnight = PrayerClock.nextDayAt(today, LocalTime.MIDNIGHT, todayData.getGmtOffsetMinutes());
        Instant target = minuteTick.isBefore(midnight) ? minuteTick : midnight;
        // Sınırın hemen sonrasına düşsün (aynı dakikayı tekrar çizmesin)
        return target.plusMillis(REDRAW_AFTER_MS);
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

    /**
     * Dışarıdan (alarm worker) tetikleme: bir dakika içinde yeniden çizilir; çizim kendi bir sonraki
     * güncellemesini (sıradaki vakit / gece yarısı) planlar.
     */
    public void scheduleNextUpdate(Context context) {
        scheduleUpdateAt(context.getApplicationContext(), Instant.now().plusSeconds(60));
    }

    /**
     * Tek bir bekleyen güncelleme tutulur (aynı PendingIntent). Exact alarm izni varsa tam zamanında,
     * yoksa dar pencereli alarm kullanılır (setAndAllowWhileIdle saatlerce gecikebiliyordu ve bu sürede
     * kronometre eksiye düşüyordu). RTC (uyandırmaz): ekran kapalıyken ekran açılınca teslim edilir.
     */
    private static void scheduleUpdateAt(Context context, Instant at) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        Intent intent = new Intent(context, PrayerTimesWidget.class).setAction(ACTION_UPDATE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long triggerAt = at.toEpochMilli();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setWindow(AlarmManager.RTC, triggerAt, INEXACT_WINDOW_MS, pendingIntent);
        } else {
            // Dakikalık çizim yalnızca ekran açıkken anlamlı: uyku/Doze sırasında beklenir, kota harcanmaz
            alarmManager.setExact(AlarmManager.RTC, triggerAt, pendingIntent);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "Widget enabled");

        AppPreferences.from(context).weather().invalidate();
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


    /**
     * Hava durumu yalnızca kendi alanlarını kısmi güncellemeyle yazar. Önceden tüm görünüm (eski
     * geri sayım dahil) cevap gelince yeniden uygulanıyordu; cevap vakit geçtikten sonra gelirse
     * widget geçmiş vakte sayıp eksiye düşüyor ve bir sonraki çizime kadar öyle kalıyordu.
     */
    private void updateWeatherInfo(Context context, double latitude, double longitude,
                                   AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.prayer_times_widget);
        if (!isNetworkAvailable(context)) {
            Log.w(TAG, "İnternet bağlantısı yok");
            showRefreshButton(views, context, appWidgetId);
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
            return;
        }

        WeatherService weatherService = new WeatherService(context);
        weatherService.getDailyWeatherAndForecastByDate(latitude, longitude, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherData weatherData) {
                if (weatherData != null && weatherData.getCurrentWeather() != null) {
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
                        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
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
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Hava durumu güncellenemedi: " + error);
                showRefreshButton(views, context, appWidgetId);
                appWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
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
        return NetworkServiceUtil.isConnected(context);
    }
}


