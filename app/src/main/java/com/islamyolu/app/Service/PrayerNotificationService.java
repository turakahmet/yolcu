package com.islamyolu.app.Service;

import static com.islamyolu.app.VakitApi.NotificationConstants.FOREGROUND_NOTIFICATION_ID;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.islamyolu.app.R;
import com.islamyolu.app.main.MainActivity;
import com.islamyolu.app.prayertimes.CurrentPrayerTimes;
import com.islamyolu.app.prayertimes.PrayerDisplay;
import com.islamyolu.app.prayertimes.PrayerSlot;
import com.islamyolu.app.prayertimes.PrayerTimeline;
import com.islamyolu.app.prayertimes.db.PrayerDayEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * "Vakti her zaman göster" kalıcı bildirimi. Vakitler Room'dan okunur (§5), geri sayım saniyede bir
 * şehrin gmt farkıyla hesaplanır. Veri arka planda periyodik ve gün dönünce yeniden yüklenir.
 */
public class PrayerNotificationService extends Service {
    private static final String TAG = "PrayerNotificationService";
    private static final String CHANNEL_ID = "prayer_service_channel";
    /** Senkronla gelen yeni veriyi yakalamak için Room'dan yeniden okuma aralığı */
    private static final long RELOAD_INTERVAL_MS = 10 * 60 * 1000L;

    private Handler handler;
    private boolean isRunning = false;
    private NotificationManager notificationManager;
    private SharedPrefsHelperService sharedPrefsHelperSettingService;
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    @Nullable private CurrentPrayerTimes current;
    private long loadedAt;
    private boolean loading;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updatePrayerTimes();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        sharedPrefsHelperSettingService = new SharedPrefsHelperService(this, MainActivity.SETTINGS_PREFS);
        createNotificationChannel();
        Log.d(TAG, "Service created successfully");
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.namaz_vakit_notification_header),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setShowBadge(false);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean showAlways = sharedPrefsHelperSettingService.getSwitchState("switchNotificationVakitHerzamanGoster");
        if (!showAlways) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!isRunning) {
            reloadTimes();
            handler.removeCallbacks(updateRunnable);
            handler.post(updateRunnable);
            Log.d(TAG, "Service started");
        }
        return START_STICKY;
    }

    /** Room'dan arka planda okur; ana thread'de disk işi yapılmaz. */
    private void reloadTimes() {
        if (loading) return;
        loading = true;
        loader.execute(() -> {
            CurrentPrayerTimes loaded = CurrentPrayerTimes.load(getApplicationContext(), Instant.now());
            handler.post(() -> {
                current = loaded;
                loadedAt = System.currentTimeMillis();
                loading = false;
                if (!isRunning && (loaded.selection == null || loaded.timeline.today(Instant.now()) == null)) {
                    // Gösterilecek vakit yok (şehir seçilmemiş/veri bitmiş): bildirim açılamaz, servis kapanır
                    Log.w(TAG, "Vakit verisi yok; kalıcı bildirim başlatılmadı");
                    stopSelf();
                }
            });
        });
    }

    private void updatePrayerTimes() {
        Instant now = Instant.now();
        if (current == null) {
            return; // ilk yükleme sürüyor
        }
        PrayerTimeline.Moment next = current.timeline.next(now);
        PrayerDayEntity today = current.timeline.today(now);
        if (next == null || today == null || System.currentTimeMillis() - loadedAt > RELOAD_INTERVAL_MS) {
            reloadTimes(); // gün döndü, veri yenilendi veya bitti
        }
        if (current.selection == null || next == null || today == null) {
            return; // gösterilecek vakit yok; eski bildirim yerinde kalır
        }
        updatePrayerNotification(today, next, now);
    }

    private void updatePrayerNotification(PrayerDayEntity today, PrayerTimeline.Moment next, Instant now) {
        try {
            String cityName = current.selection.getCityName();
            String nextName = getString(PrayerDisplay.nameRes(next.slot));
            String remainingTime = PrayerDisplay.remaining(Duration.between(now, next.instant), true);

            RemoteViews collapsedView = new RemoteViews(getPackageName(), R.layout.notification_collapsed);
            RemoteViews expandedView = new RemoteViews(getPackageName(), R.layout.notification_expanded);

            // Ana aktiviteyi açmak için Intent oluştur
            Intent mainActivityIntent = new Intent(this, MainActivity.class);
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mainActivityIntent.setAction(Intent.ACTION_MAIN);
            mainActivityIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    mainActivityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            PendingIntent emptyIntent = PendingIntent.getActivity(this, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE);

            collapsedView.setOnClickPendingIntent(R.id.notification_collapsed_root, emptyIntent);
            expandedView.setOnClickPendingIntent(R.id.notification_expanded_root, pendingIntent);

            collapsedView.setTextViewText(R.id.remainingTime, remainingTime);
            collapsedView.setTextViewText(R.id.remainingTimeLabel, getString(R.string.prayer_notification_next, nextName));
            collapsedView.setTextViewText(R.id.nextPrayerInfoRemainCity,
                    cityName + " | " + getString(R.string.prayer_notification_time, nextName, next.time));
            collapsedView.setTextViewText(R.id.hicriAy,
                    getString(R.string.prayer_notification_hijri, PrayerDisplay.hijri(this, today.getHijri())));

            setupExpandedView(expandedView, cityName, remainingTime, today, next.slot);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.crescent)
                    .setCustomContentView(collapsedView)
                    .setCustomBigContentView(expandedView)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setShowWhen(false)
                    .setContentIntent(pendingIntent)
                    .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                    .setColor(Color.GREEN)
                    .build();

            if (!isRunning) {
                startForeground(FOREGROUND_NOTIFICATION_ID, notification);
                isRunning = true;
            } else {
                notificationManager.notify(FOREGROUND_NOTIFICATION_ID, notification);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Bildirim güncelleme hatası: " + e.getMessage(), e);
        }
    }

    private void setupExpandedView(RemoteViews expandedView, String cityName, String remainingTime,
                                   PrayerDayEntity today, PrayerSlot nextSlot) {
        expandedView.setTextViewText(R.id.cityName, cityName);
        expandedView.setTextViewText(R.id.remainingTime, remainingTime);

        for (PrayerSlot slot : PrayerSlot.values()) {
            int viewId = viewIdFor(slot);
            expandedView.setInt(viewId, "setBackgroundResource", slot == nextSlot
                    ? R.drawable.prayer_time_card_background_active : R.drawable.prayer_time_card_background);
            expandedView.setTextViewText(viewId, getString(R.string.prayer_notification_time,
                    getString(PrayerDisplay.nameRes(slot)), PrayerDisplay.time(slot.timeOf(today))));
        }
    }

    private static int viewIdFor(PrayerSlot slot) {
        switch (slot) {
            case IMSAK:
                return R.id.imsak;
            case GUNES:
                return R.id.gunes;
            case OGLE:
                return R.id.ogle;
            case IKINDI:
                return R.id.ikindi;
            case AKSAM:
                return R.id.aksam;
            default:
                return R.id.yatsi;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        boolean showAlways = sharedPrefsHelperSettingService.getSwitchState("switchNotificationVakitHerzamanGoster");
        if (showAlways) {
            // Servis yeniden başlatılmalı
            Intent broadcastIntent = new Intent("RestartPrayerService");
            sendBroadcast(broadcastIntent);
        }
        isRunning = false;
        if (handler != null) {
            handler.removeCallbacks(updateRunnable);
        }
        loader.shutdownNow();
        Log.d(TAG, "Service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        if (sharedPrefsHelperSettingService.getSwitchState("switchNotificationVakitHerzamanGoster")) {
            Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
            restartServiceIntent.setPackage(getPackageName());
            startService(restartServiceIntent);
            Log.d(TAG, "Service restarted after task removed");
        }
    }
}
