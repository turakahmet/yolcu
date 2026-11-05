package com.islamyolu.app.Service;

import static com.islamyolu.app.VakitApi.NotificationConstants.FOREGROUND_NOTIFICATION_ID;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.islamyolu.app.R;
import com.islamyolu.app.main.MainActivity;
import com.islamyolu.app.prayertimes.CurrentPrayerTimes;
import com.islamyolu.app.prayertimes.PrayerClock;
import com.islamyolu.app.prayertimes.PrayerCountdown;
import com.islamyolu.app.prayertimes.PrayerDisplay;
import com.islamyolu.app.prayertimes.PrayerSlot;
import com.islamyolu.app.prayertimes.PrayerTimeline;
import com.islamyolu.app.prayertimes.db.PrayerDayEntity;
import com.islamyolu.app.preferences.AppPreferences;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * "Vakti her zaman göster" kalıcı bildirimi (ön plan servisi, tür: specialUse).
 * <p>
 * Pil dostu ve donmayan yapı:
 * <ul>
 *   <li>Saniyeyi sistem kronometresi akıtır: kalan süre "03:" (metin) + "20:11" (kronometre).
 *       Uygulama saniyede bir bildirim göndermez; bu yüzden bildirim kısıtlamasına takılıp donmaz ve
 *       her güncellemede listede öne zıplayıp (ör. Haritalar ile) yer kapmaya çalışmaz.</li>
 *   <li>Bildirim yalnızca bir şey değişince yenilenir: saat hanesi, sıradaki vakit, şehrin gece
 *       yarısı (vakit listesi), vakit verisi (senkron/şehir değişimi), cihaz saati.</li>
 *   <li>Yenileme anı hem Handler (ekran açıkken kesin) hem tam zamanlı alarm (uykudayken; Doze
 *       kotasını harcamaz, ezan alarmlarını geciktirmez) ile planlanır; ekran açılınca da bakılır.</li>
 *   <li>Kanal "düşük" önemdedir: sessiz, öne çıkmaz ve bildirim panelinde kapalı (tek satır) gelir;
 *       kullanıcı diğer vakitleri görmek isterse kendisi açar.</li>
 * </ul>
 * Vakitler yalnızca Room'dan okunur (§5); hesap ana ekran ve widget ile aynıdır (PrayerCountdown).
 */
public class PrayerNotificationService extends Service {
    private static final String TAG = "PrayerNotificationService";

    /** Düşük önemli yeni kanal; eski kanalın önemi değiştirilemediği için yeni kimlik kullanılır. */
    private static final String CHANNEL_ID = "prayer_countdown_channel";
    private static final String LEGACY_CHANNEL_ID = "prayer_service_channel";

    /** Vakit verisi değişince (senkron, şehir seçimi) servisin kendini yenilemesi için. */
    private static final String ACTION_REFRESH = "com.islamyolu.app.action.PRAYER_NOTIFICATION_REFRESH";
    /** Saat/vakit sınırından sonra yenileme payı (kronometre 1 sn dolmadan eksiye geçmez) */
    private static final long BOUNDARY_SLACK_MS = 50;
    /** Veri yokken yeniden deneme aralığı */
    private static final long NO_DATA_RETRY_MS = 30 * 60 * 1000L;
    /** Duvar saati ile kronometre saati arasında bu kadar kayma olursa (saat ayarı) yeniden çizilir */
    private static final long CLOCK_DRIFT_TOLERANCE_MS = 1000;

    private Handler handler;
    private NotificationManager notificationManager;
    private AlarmManager alarmManager;
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private boolean foreground;
    private boolean receiverRegistered;

    // Son çizilen bildirimin anahtarı: aynıysa yeniden gönderilmez
    private int shownCityId = -1;
    @Nullable private String shownDate;
    @Nullable private Instant shownTarget;
    private long shownHours = -1;
    private long shownClockOffset;

    private final Runnable refreshRunnable = this::refresh;

    private final BroadcastReceiver events = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Yenileme alarmı, veri değişimi, saat ayarı veya ekran açılması
            refresh();
        }
    };

    /** Vakit verisi değişince çağrılır; servis çalışmıyorsa bir şey olmaz. */
    public static void requestRefresh(@NonNull Context context) {
        context.sendBroadcast(new Intent(ACTION_REFRESH).setPackage(context.getPackageName()));
    }

    /** Ayar açıksa servisi başlatır (uygulama, açılış ve güncelleme sonrası). */
    public static void startIfEnabled(@NonNull Context context) {
        if (!AppPreferences.from(context).settings().isAlwaysShowOn()) {
            return;
        }
        try {
            ContextCompat.startForegroundService(context, new Intent(context, PrayerNotificationService.class));
        } catch (RuntimeException e) {
            // Android 12+: arka plandan ön plan servisi başlatma izni yoksa (muafiyet dışı durum)
            Log.w(TAG, "Kalıcı bildirim servisi başlatılamadı: " + e.getMessage());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        createNotificationChannel();
        registerEvents();
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                getString(R.string.prayer_countdown_channel_name), NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.prayer_countdown_channel_description));
        channel.setShowBadge(false);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setSound(null, null);
        notificationManager.createNotificationChannel(channel);
        // Eski "varsayılan" önemli kanal: sesli/öne çıkan ve bazı arayüzlerde açık gelen bildirimin kaynağı
        notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID);
    }

    private void registerEvents() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_REFRESH);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        ContextCompat.registerReceiver(this, events, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        receiverRegistered = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!AppPreferences.from(this).settings().isAlwaysShowOn()) {
            stopSelfCompletely();
            return START_NOT_STICKY;
        }
        // startForegroundService'ten sonra birkaç saniye içinde ön plana geçilmeli: veri okunmadan
        // önce geçici bildirimle hemen geçilir, vakitler gelince aynı bildirim güncellenir
        if (!enterForeground(buildLoadingNotification())) {
            return START_NOT_STICKY;
        }
        refresh();
        return START_STICKY;
    }

    private boolean enterForeground(Notification notification) {
        if (foreground) {
            return true;
        }
        try {
            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    ? ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE : 0;
            ServiceCompat.startForeground(this, FOREGROUND_NOTIFICATION_ID, notification, type);
            foreground = true;
            return true;
        } catch (RuntimeException e) {
            Log.e(TAG, "Ön plana geçilemedi: " + e.getMessage(), e);
            stopSelf();
            return false;
        }
    }

    /** Vakitleri arka planda Room'dan okur, değişiklik varsa bildirimi yeniler ve sonraki anı planlar. */
    private void refresh() {
        handler.removeCallbacks(refreshRunnable);
        if (loader.isShutdown()) {
            return; // servis kapandı
        }
        loader.execute(() -> {
            CurrentPrayerTimes loaded = CurrentPrayerTimes.load(getApplicationContext(), Instant.now());
            handler.post(() -> render(loaded));
        });
    }

    private void render(CurrentPrayerTimes data) {
        if (!foreground) {
            return; // servis kapanıyor
        }
        if (data.selection == null) {
            // Şehir seçimi kaldırıldı: gösterilecek vakit yok
            stopSelfCompletely();
            return;
        }
        Instant now = Instant.now();
        PrayerCountdown countdown = PrayerCountdown.of(data.timeline, now);
        LocalDate todayDate = data.timeline.cityToday(now);
        PrayerDayEntity today = todayDate != null ? data.timeline.day(todayDate) : null;
        if (countdown == null || today == null) {
            // Veri bitti (§6): uyarı gösterilir, senkron gelince requestRefresh ile yenilenir
            notifyIfChanged(null, null, null, -1, () -> buildStatusNotification(data));
            scheduleRefresh(now.plusMillis(NO_DATA_RETRY_MS));
            return;
        }
        notifyIfChanged(data.selection.getCityId(), today.getDate(), countdown.next.instant, countdown.hours(),
                () -> buildCountdownNotification(data, today, countdown));

        // Bir sonraki değişim: saat hanesi düşer (vakit anı da bir saat sınırıdır) ya da şehrin gece yarısı
        Instant hourBoundary = now.plusMillis(countdown.millisWithinHour());
        Instant midnight = PrayerClock.nextDayAt(todayDate, LocalTime.MIDNIGHT, today.getGmtOffsetMinutes());
        Instant next = hourBoundary.isBefore(midnight) ? hourBoundary : midnight;
        scheduleRefresh(next.plusMillis(BOUNDARY_SLACK_MS));
    }

    private interface NotificationFactory {
        Notification build();
    }

    /** Görünen değer değişmediyse bildirim yeniden gönderilmez (sıralamada öne zıplama ve pil). */
    private void notifyIfChanged(@Nullable Integer cityId, @Nullable String date, @Nullable Instant target,
                                 long hours, NotificationFactory factory) {
        long clockOffset = System.currentTimeMillis() - SystemClock.elapsedRealtime();
        boolean unchanged = shownCityId == (cityId != null ? cityId : -2)
                && equalsNullable(shownDate, date)
                && equalsNullable(shownTarget, target)
                && shownHours == hours
                && Math.abs(clockOffset - shownClockOffset) < CLOCK_DRIFT_TOLERANCE_MS;
        if (unchanged) {
            return;
        }
        try {
            notificationManager.notify(FOREGROUND_NOTIFICATION_ID, factory.build());
            shownCityId = cityId != null ? cityId : -2;
            shownDate = date;
            shownTarget = target;
            shownHours = hours;
            shownClockOffset = clockOffset;
        } catch (RuntimeException e) {
            Log.e(TAG, "Bildirim güncelleme hatası: " + e.getMessage(), e);
        }
    }

    private static boolean equalsNullable(@Nullable Object a, @Nullable Object b) {
        return a == null ? b == null : a.equals(b);
    }

    /**
     * Ekran açıkken Handler kesin anda çalışır (üretici alarm hizalamasından etkilenmez); cihaz
     * uykudayken tam zamanlı alarm uyandırır. setExact (AllowWhileIdle değil): Doze'da bekler,
     * kota harcamaz ve ezan/bildirim alarmlarını geciktirmez; Doze'dan çıkınca hemen çalışır.
     */
    private void scheduleRefresh(Instant at) {
        long delay = Math.max(0, at.toEpochMilli() - System.currentTimeMillis());
        handler.removeCallbacks(refreshRunnable);
        handler.postDelayed(refreshRunnable, delay);

        if (alarmManager == null) {
            return;
        }
        PendingIntent pi = refreshAlarmIntent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setWindow(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), 60_000, pi);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pi);
        }
    }

    private PendingIntent refreshAlarmIntent() {
        Intent intent = new Intent(ACTION_REFRESH).setPackage(getPackageName());
        return PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // --- Bildirimler ---

    private NotificationCompat.Builder baseBuilder() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.crescent)
                .setPriority(NotificationCompat.PRIORITY_LOW)        // Android 7 ve altı için
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setOngoing(true)
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setShowWhen(false)
                .setLocalOnly(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentIntent(openAppIntent())
                .setColor(Color.GREEN);
    }

    private Notification buildLoadingNotification() {
        return baseBuilder()
                .setContentTitle(getString(R.string.namaz_vakit_notification_header))
                .setContentText(getString(R.string.prayer_notification_loading))
                .build();
    }

    private Notification buildStatusNotification(CurrentPrayerTimes data) {
        return baseBuilder()
                .setContentTitle(data.selection != null ? data.selection.getCityName()
                        : getString(R.string.namaz_vakit_notification_header))
                .setContentText(getString(R.string.prayer_status_outdated_offline))
                .build();
    }

    private Notification buildCountdownNotification(CurrentPrayerTimes data, PrayerDayEntity today,
                                                    PrayerCountdown countdown) {
        PrayerTimeline.Moment next = countdown.next;
        String cityName = data.selection.getCityName();
        String nextName = getString(PrayerDisplay.nameRes(next.slot));
        // Kronometre saatin içindeki kalan süreyi sayar; tabanı cihazın açık kalma saatine göredir
        long chronometerBase = SystemClock.elapsedRealtime() + countdown.millisWithinHour();
        String hours = countdown.hoursPrefix();

        RemoteViews collapsedView = new RemoteViews(getPackageName(), R.layout.notification_collapsed);
        RemoteViews expandedView = new RemoteViews(getPackageName(), R.layout.notification_expanded);

        // Kapalı görünüme dokunmak uygulamayı açmaz (detay için kullanıcı bildirimi açar); açık görünüm açar
        PendingIntent emptyIntent = PendingIntent.getActivity(this, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE);
        collapsedView.setOnClickPendingIntent(R.id.notification_collapsed_root, emptyIntent);
        expandedView.setOnClickPendingIntent(R.id.notification_expanded_root, openAppIntent());

        setCountdown(collapsedView, hours, chronometerBase);
        collapsedView.setTextViewText(R.id.remainingTimeLabel, getString(R.string.prayer_notification_next, nextName));
        collapsedView.setTextViewText(R.id.nextPrayerInfoRemainCity,
                cityName + " | " + getString(R.string.prayer_notification_time, nextName, next.time));
        collapsedView.setTextViewText(R.id.hicriAy,
                getString(R.string.prayer_notification_hijri, PrayerDisplay.hijri(this, today.getHijri())));

        expandedView.setTextViewText(R.id.cityName, cityName);
        setCountdown(expandedView, hours, chronometerBase);
        for (PrayerSlot slot : PrayerSlot.values()) {
            int viewId = viewIdFor(slot);
            expandedView.setInt(viewId, "setBackgroundResource", slot == next.slot
                    ? R.drawable.prayer_time_card_background_active : R.drawable.prayer_time_card_background);
            expandedView.setTextViewText(viewId, getString(R.string.prayer_notification_time,
                    getString(PrayerDisplay.nameRes(slot)), PrayerDisplay.time(slot.timeOf(today))));
        }

        return baseBuilder()
                .setCustomContentView(collapsedView)
                .setCustomBigContentView(expandedView)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .build();
    }

    private static void setCountdown(RemoteViews views, String hours, long chronometerBase) {
        views.setTextViewText(R.id.remainingHours, hours);
        views.setChronometer(R.id.remainingTime, chronometerBase, null, true);
        views.setChronometerCountDown(R.id.remainingTime, true);
    }

    private PendingIntent openAppIntent() {
        Intent intent = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER);
        return PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
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

    // --- Yaşam döngüsü ---

    private void stopSelfCompletely() {
        foreground = false;
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        foreground = false;
        handler.removeCallbacks(refreshRunnable);
        if (alarmManager != null) {
            alarmManager.cancel(refreshAlarmIntent());
        }
        if (receiverRegistered) {
            unregisterReceiver(events);
            receiverRegistered = false;
        }
        loader.shutdownNow();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
