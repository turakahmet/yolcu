package com.islamyolu.app.Service;

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

import com.islamyolu.app.Entity.PrayerNames;
import com.islamyolu.app.R;
import com.islamyolu.app.VakitApi.NamazVakiti;
import com.islamyolu.app.VakitApi.VakitNoApi;
import com.islamyolu.app.main.MainActivity;

public class PrayerNotificationService extends Service {
    private static final String TAG = "PrayerNotificationService";
    private static final String CHANNEL_ID = "prayer_service_channel";
    private static final int NOTIFICATION_ID = 64254;
    private Handler handler;
    private boolean isRunning = false;
    private NamazVakiti currentNamazVakiti = new NamazVakiti();
    private NotificationManager notificationManager;
    private SharedPrefsHelperService sharedPrefsHelperSettingService;
    private final VakitNoApi vakitNoApi = new VakitNoApi();

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
                "Namaz Vakitleri",
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
        try {
            boolean showAlways = sharedPrefsHelperSettingService.getSwitchState("switchNotificationVakitHerzamanGoster");

            if (showAlways) {
                if (!isRunning || currentNamazVakiti == null) {
                    String cityName = sharedPrefsHelperSettingService.getCityNameFromPreferences();
                    currentNamazVakiti = vakitNoApi.getLocalPrayerTimesByCityName(cityName);

                    if (currentNamazVakiti != null) {
                        NamazVakiti.updateInstance(currentNamazVakiti);
                        updatePrayerTimes();
                        startPrayerTimeUpdates();
                        isRunning = true;
                        Log.d(TAG, "Service started and times updated");
                    }
                }
                return START_STICKY;
            } else {
                stopForeground(Service.STOP_FOREGROUND_REMOVE);
                stopSelf();
                return START_NOT_STICKY;
            }
        } catch (Exception e) {
            Log.e(TAG, "Service start error: " + e.getMessage(), e);
            return START_NOT_STICKY;
        }
    }

    private void startPrayerTimeUpdates() {
        handler.removeCallbacks(updateRunnable);
        handler.post(updateRunnable);
    }

    private void updatePrayerTimes() {
        try {
            if (currentNamazVakiti == null) {
                String cityName = sharedPrefsHelperSettingService.getCityNameFromPreferences();
                currentNamazVakiti = vakitNoApi.getLocalPrayerTimesByCityName(cityName);
                if (currentNamazVakiti != null) {
                    NamazVakiti.updateInstance(currentNamazVakiti);
                }
            }
            if (currentNamazVakiti != null) {
                updatePrayerNotification();
            } else {
                Log.e(TAG, "Namaz vakitleri null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Vakit güncelleme hatası: " + e.getMessage(), e);
        }
    }


    private void updatePrayerNotification() {
        try {
            String cityName = sharedPrefsHelperSettingService.getCityNameFromPreferences();
//            String remainingTime = getRemainingTimeFormatted();

            RemoteViews collapsedView = new RemoteViews(getPackageName(), R.layout.notification_collapsed);
            RemoteViews expandedView = new RemoteViews(getPackageName(), R.layout.notification_expanded);


            // Ana aktiviteyi açmak için Intent oluştur
            Intent mainActivityIntent = new Intent(this, MainActivity.class);
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mainActivityIntent.setAction(Intent.ACTION_MAIN);
            mainActivityIntent.addCategory(Intent.CATEGORY_LAUNCHER);


            // PendingIntent oluştur
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    mainActivityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            PendingIntent emptyIntent = PendingIntent.getActivity(this, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE);

            collapsedView.setOnClickPendingIntent(R.id.notification_collapsed_root, emptyIntent);
            expandedView.setOnClickPendingIntent(R.id.notification_expanded_root, pendingIntent);

            String remainingTime = currentNamazVakiti.getNextPrayerTimeRemainingTimeFormatted(true);

            collapsedView.setTextViewText(R.id.remainingTime, remainingTime);
            collapsedView.setTextViewText(R.id.remainingTimeLabel, String.format("sonra %s vakti", currentNamazVakiti.getNextPrayer().getName()));
            collapsedView.setTextViewText(R.id.nextPrayerInfoRemainCity, String.format("%s | %s: %s", cityName, currentNamazVakiti.getNextPrayer().getName(), currentNamazVakiti.getNextPrayer().getTime()));
            collapsedView.setTextViewText(R.id.hicriAy, "Hicri: " + currentNamazVakiti.getHicriDate());

            setupExpandedView(expandedView, cityName, remainingTime);

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
                startForeground(NOTIFICATION_ID, notification);
            } else {
                notificationManager.notify(NOTIFICATION_ID, notification);
            }

        } catch (Exception e) {
            Log.e(TAG, "Bildirim güncelleme hatası: " + e.getMessage(), e);
        }
    }

    private void setupExpandedView(RemoteViews expandedView, String cityName, String remainingTime) {
        expandedView.setTextViewText(R.id.cityName, cityName);
        expandedView.setTextViewText(R.id.remainingTime, remainingTime);

        int[] prayerTimeIds = {R.id.imsak, R.id.gunes, R.id.ogle, R.id.ikindi, R.id.aksam, R.id.yatsi};
        for (int id : prayerTimeIds) {
            expandedView.setInt(id, "setBackgroundResource", R.drawable.prayer_time_card_background);
        }

        int nextPrayerId = getViewIdForPrayer(currentNamazVakiti.getNextPrayer().getName());
        if (nextPrayerId != 0) {
            expandedView.setInt(nextPrayerId, "setBackgroundResource", R.drawable.prayer_time_card_background_active);
        }

        expandedView.setTextViewText(R.id.imsak, "İmsak: " + currentNamazVakiti.getImsak());
        expandedView.setTextViewText(R.id.gunes, "Güneş: " + currentNamazVakiti.getGunes());
        expandedView.setTextViewText(R.id.ogle, "Öğle: " + currentNamazVakiti.getOgle());
        expandedView.setTextViewText(R.id.ikindi, "İkindi: " + currentNamazVakiti.getIkindi());
        expandedView.setTextViewText(R.id.aksam, "Akşam: " + currentNamazVakiti.getAksam());
        expandedView.setTextViewText(R.id.yatsi, "Yatsı: " + currentNamazVakiti.getYatsi());
    }

    private int getViewIdForPrayer(String prayerName) {
        if (prayerName.equals(PrayerNames.IMSAK.getValue())) return R.id.imsak;
        if (prayerName.equals(PrayerNames.GUNES.getValue())) return R.id.gunes;
        if (prayerName.equals(PrayerNames.OGLE.getValue())) return R.id.ogle;
        if (prayerName.equals(PrayerNames.IKINDI.getValue())) return R.id.ikindi;
        if (prayerName.equals(PrayerNames.AKSAM.getValue())) return R.id.aksam;
        if (prayerName.equals(PrayerNames.YATSI.getValue())) return R.id.yatsi;
        return 0;
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