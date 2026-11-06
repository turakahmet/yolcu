package com.islamyolu.app.VakitApi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.islamyolu.app.R;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.SettingsPreferences;
import com.islamyolu.app.prayertimes.CurrentPrayerTimes;
import com.islamyolu.app.prayertimes.Kerahat;
import com.islamyolu.app.prayertimes.PrayerClock;
import com.islamyolu.app.prayertimes.PrayerSlot;
import com.islamyolu.app.prayertimes.PrayerTimeline;
import com.islamyolu.app.prayertimes.db.PrayerDayEntity;
import com.islamyolu.app.receiver.PrayerSoundReceiver;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Vakit bildirimi ve ezan sesi alarmlarını kuran TEK yer. Vakitler yalnızca Room'dan okunur.
 * <p>
 * Bildirimin kaçmaması için üç katman:
 * <ol>
 *   <li>Alarmlar bugün VE yarın için kurulur: gece yeniden kurulum gecikse de ertesi günün alarmları hazırdır.</li>
 *   <li>Yeniden kurulum şehrin gece yarısına (00:02) AlarmManager ile planlanır. Eskiden bu iş gecikmeli
 *       WorkManager ile yapılıyordu; pil yönetimi sıkı cihazlarda (Honor vb.) iş, uygulama açılana kadar
 *       ertelendiği için o günün hiç alarmı kurulmuyordu.</li>
 *   <li>{@link DailyPrayerWorker} birkaç saatte bir yedek olarak yeniden kurar.</li>
 * </ol>
 * Kurulum idempotenttir: aynı vakit için aynı PendingIntent güncellenir, tekrar alarm oluşmaz.
 */
public final class PrayerAlarmScheduler {

    private static final String TAG = "PrayerAlarmScheduler";

    /** Bildirim ve ezan alarmlarının ortak action'ı (eski kurulu alarmlarla uyum için değiştirilmez). */
    public static final String ACTION_PRAYER_ALARM = "com.myapp.yolcu.ACTION_ALARM_NOTIFICATION";
    static final String ACTION_RESCHEDULE = "com.islamyolu.app.action.RESCHEDULE_PRAYER_ALARMS";

    static final String EXTRA_TITLE = "title";
    static final String EXTRA_MESSAGE = "message";
    static final String EXTRA_NOTIFICATION_ID = "notificationId";
    /** Vaktin kesin anı (epoch ms): geç teslim edilen bildirimi ayıklamak için */
    static final String EXTRA_PRAYER_AT = "prayerAt";
    /** true: kerahat başlangıç bildirimi (ayrı kanal) */
    static final String EXTRA_KERAHAT = "kerahat";
    /** Vakit anı alarmı (PrayerSoundReceiver): ezan sesi çalınsın mı, telefon titreşime alınsın mı */
    public static final String EXTRA_PLAY_SOUND = "playSound";
    public static final String EXTRA_SILENCE = "silence";

    private static final String BACKUP_WORK = "PrayerAlarmsBackup";
    private static final int RESCHEDULE_REQUEST_CODE = 7301;
    /** Sabah ezanı (Güneş anahtarı) güneşten bu kadar dakika önce çalar (mevcut davranış). */
    private static final int SABAH_EZANI_MINUTES_BEFORE_SUNRISE = 60;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    /** Vakit, ayar anahtarı ve bildirim/ses içinde kullanılan ad (request code'larda kullanılır, değiştirme). */
    private static final Object[][] PRAYERS = {
            {PrayerSlot.IMSAK, SettingsPreferences.KEY_NOTIFY_IMSAK, "İmsak", R.string.imsak_vakit_notification_body},
            {PrayerSlot.GUNES, SettingsPreferences.KEY_NOTIFY_SABAH, "Güneş", R.string.gunes_vakit_notification_body},
            {PrayerSlot.OGLE, SettingsPreferences.KEY_NOTIFY_OGLE, "Öğle", R.string.ogle_vakit_notification_body},
            {PrayerSlot.IKINDI, SettingsPreferences.KEY_NOTIFY_IKINDI, "İkindi", R.string.ikindi_vakit_notification_body},
            {PrayerSlot.AKSAM, SettingsPreferences.KEY_NOTIFY_AKSAM, "Akşam", R.string.aksam_vakit_notification_body},
            {PrayerSlot.YATSI, SettingsPreferences.KEY_NOTIFY_YATSI, "Yatsı", R.string.yatsi_vakit_notification_body}
    };

    private PrayerAlarmScheduler() {
    }

    /** Alıcılardan çağrılır: kurulumu arka planda yapar, bitince {@code onDone} çalışır (goAsync().finish). */
    public static void rescheduleAsync(@NonNull Context context, @Nullable Runnable onDone) {
        Context app = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                reschedule(app);
            } catch (RuntimeException e) {
                Log.e(TAG, "Alarmlar kurulamadı", e);
            } finally {
                if (onDone != null) onDone.run();
            }
        });
    }

    /** Room okur ve alarmları kurar; ana thread'de çağrılmaz. */
    @WorkerThread
    public static void reschedule(@NonNull Context context) {
        Context app = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager yok; alarmlar kurulamadı");
            return;
        }
        ensureBackupWork(app);
        // Cihaz yeniden başladıysa bekleyen "eski zil moduna dön" alarmı silinmiştir
        PrayerSilencer.ensureRestorePending(app);

        Instant now = Instant.now();
        CurrentPrayerTimes current = CurrentPrayerTimes.load(app, now);
        LocalDate today = current.selection != null ? current.timeline.cityToday(now) : null;
        if (today == null) {
            // Şehir seçilmemiş veya veri yok: seçim/senkron bitince PrayerTimesUpdateNotifier yeniden kurar
            Log.d(TAG, "Şehir seçimi veya bugünün vakitleri yok; alarmlar kurulmadı");
            scheduleNextReschedule(app, alarmManager, null);
            return;
        }

        AppPreferences preferences = AppPreferences.from(app);
        SettingsPreferences settings = preferences.settings();
        for (LocalDate date : new LocalDate[]{today, today.plusDays(1)}) {
            for (Object[] prayer : PRAYERS) {
                schedulePrayer(app, alarmManager, settings, current.timeline, date, now, prayer);
                scheduleKerahat(app, alarmManager, settings, current.timeline, date, now, prayer);
            }
        }
        preferences.sentNotifications().pruneOlderThan(2);
        preferences.settings().removeStaleCityName();

        PrayerDayEntity todayData = current.timeline.day(today);
        Instant next = todayData != null
                ? PrayerClock.nextDayAt(today, LocalTime.of(0, 2), todayData.getGmtOffsetMinutes())
                : null;
        scheduleNextReschedule(app, alarmManager, next);
    }

    private static void schedulePrayer(Context context, AlarmManager alarmManager, SettingsPreferences settings,
                                       PrayerTimeline timeline, LocalDate date, Instant now, Object[] prayer) {
        PrayerSlot slot = (PrayerSlot) prayer[0];
        String switchKey = (String) prayer[1];
        String name = (String) prayer[2];
        int bodyRes = (Integer) prayer[3];

        String notificationId = notificationId(name, date);
        // Önce kaldır: bildirim kapatıldıysa veya şehir değiştiyse eski saat çalmasın
        alarmManager.cancel(notificationIntent(context, notificationId));
        alarmManager.cancel(prayerMomentIntent(context, name, date, false, false));
        cancelLegacySound(context, alarmManager, name, date);

        Instant prayerAt = timeline.instantOf(date, slot);
        if (prayerAt == null || !settings.isAllNotificationsOn() || !settings.isSwitchOn(switchKey)) {
            return; // vakit oluşmuyor (kutup bölgesi) veya bu vakit kapalı
        }

        // Vakte özel süre ve "vakit öncesi uyarı" (kapalıysa yalnızca ezan sesi çalar)
        int minutes = settings.getReminderMinutes(switchKey);
        Instant notifyAt = prayerAt.minusSeconds(minutes * 60L);
        if (settings.isReminderOn(switchKey) && notifyAt.isAfter(now)) {
            String message = context.getString(bodyRes).replace("##", String.valueOf(minutes));
            String title = context.getString(R.string.namaz_vakit_notification_header);
            setAlarm(alarmManager, notifyAt, notificationIntent(context, notificationId, title, message, prayerAt, false));
        }

        // Vakit anı: ezan sesi ve/veya titreşime alma tek alarmla (Doze'da ardışık alarmlar kısıtlanır).
        // Sabah ezanı (Güneş anahtarı) güneşten 60 dk önce; imsak namaz vakti olmadığı için titreşime alınmaz.
        boolean playSound = !(settings.getEzanSoundType() == SettingsPreferences.EZAN_SOUND_OFF
                && settings.getEzanDuaType() == SettingsPreferences.EZAN_DUA_OFF);
        boolean silence = settings.isVakitSoundModeOn() && slot != PrayerSlot.IMSAK;
        int minutesBefore = slot == PrayerSlot.GUNES ? SABAH_EZANI_MINUTES_BEFORE_SUNRISE : 0;
        Instant momentAt = prayerAt.minusSeconds(minutesBefore * 60L);
        if ((playSound || silence) && momentAt.isAfter(now)) {
            setAlarm(alarmManager, momentAt, prayerMomentIntent(context, name, date, playSound, silence));
        }
    }

    /** Öğle ve akşamdan {@link Kerahat#MINUTES} dk önce tek bildirim (güneş doğuşu kerahati hariç). */
    private static void scheduleKerahat(Context context, AlarmManager alarmManager, SettingsPreferences settings,
                                        PrayerTimeline timeline, LocalDate date, Instant now, Object[] prayer) {
        PrayerSlot slot = (PrayerSlot) prayer[0];
        if (!Kerahat.precedes(slot)) return;
        String name = (String) prayer[2];
        String notificationId = "Kerahat_" + notificationId(name, date);
        alarmManager.cancel(notificationIntent(context, notificationId));

        Instant prayerAt = timeline.instantOf(date, slot);
        if (prayerAt == null || !settings.isKerahatNotifyOn()) return;
        Instant startAt = prayerAt.minusSeconds(Kerahat.MINUTES * 60L);
        if (!startAt.isAfter(now)) return;

        String prayerName = context.getString(slot == PrayerSlot.OGLE ? R.string.ogle_namazi : R.string.aksam_namazi);
        setAlarm(alarmManager, startAt, notificationIntent(context, notificationId,
                context.getString(R.string.kerahat_notification_title),
                context.getString(R.string.kerahat_notification_message, prayerName, Kerahat.MINUTES),
                prayerAt, true));
    }

    /**
     * Doze'da da çalışan alarm. Android 12+ ve tam zamanlı alarm izni yoksa (kullanıcı kapatmış)
     * eskiden SecurityException ile hiç kurulmuyordu; artık birkaç dakika sapmalı alarmla kurulur.
     */
    static void setAlarm(AlarmManager alarmManager, Instant at, PendingIntent pendingIntent) {
        long triggerAt = at.toEpochMilli();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Tam zamanlı alarm izni yok; yaklaşık alarm kuruluyor", e);
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    /** @param at şehrin ertesi günü 00:02; null ise cihazın ertesi günü 00:02 */
    private static void scheduleNextReschedule(Context context, AlarmManager alarmManager, @Nullable Instant at) {
        ZonedDateTime deviceNow = ZonedDateTime.now();
        Instant trigger = at != null ? at
                : deviceNow.toLocalDate().plusDays(1).atTime(0, 2).atZone(deviceNow.getZone()).toInstant();
        Intent intent = new Intent(context, DailyPrayerWorkerReceiver.class).setAction(ACTION_RESCHEDULE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, RESCHEDULE_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        setAlarm(alarmManager, trigger, pendingIntent);
        Log.d(TAG, "Alarmlar kuruldu; yeniden kurulum: " + trigger);
    }

    /** WorkManager yedeği: AlarmManager zinciri bir şekilde koparsa (ör. uygulama zorla durdurulup açıldı) toparlar. */
    private static void ensureBackupWork(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(DailyPrayerWorker.class, 6, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(BACKUP_WORK, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    /** Mevcut biçim korunur ("İmsak_2026_11_05"): gönderildi kaydı ve request code bununla tutulur. */
    static String notificationId(String name, LocalDate date) {
        return String.format(Locale.ROOT, "%s_%d_%02d_%02d",
                name, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    /** İptal için: Intent eşleşmesi extra'lara bakmaz, aynı action + request code yeterli. */
    private static PendingIntent notificationIntent(Context context, String notificationId) {
        Intent intent = new Intent(context, AlarmReceiver.class).setAction(ACTION_PRAYER_ALARM);
        return PendingIntent.getBroadcast(context, notificationId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** @param kerahat true ise kerahat kanalında gösterilir */
    private static PendingIntent notificationIntent(Context context, String notificationId,
                                                    String title, String message, Instant prayerAt, boolean kerahat) {
        Intent intent = new Intent(context, AlarmReceiver.class).setAction(ACTION_PRAYER_ALARM)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_MESSAGE, message)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId)
                .putExtra(EXTRA_PRAYER_AT, prayerAt.toEpochMilli())
                .putExtra(EXTRA_KERAHAT, kerahat);
        return PendingIntent.getBroadcast(context, notificationId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Vakit anı alarmı (ezan sesi + titreşime alma); request code vaktin kendi tarihine göre.
     * Hangi ezan/dua sesinin çalacağı burada YAZILMAZ, alıcı çalma anında güncel ayardan okur.
     * İptal için bayraklar önemsizdir (Intent eşleşmesi extra'lara bakmaz).
     */
    private static PendingIntent prayerMomentIntent(Context context, String name, LocalDate date,
                                                    boolean playSound, boolean silence) {
        Intent intent = new Intent(context, PrayerSoundReceiver.class).setAction(ACTION_PRAYER_ALARM)
                .putExtra(EXTRA_PLAY_SOUND, playSound)
                .putExtra(EXTRA_SILENCE, silence);
        int requestCode = ("sound_" + name + "_" + date).hashCode();
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Önceki sürümün ezan alarmı ("sound_" + ad + yılın günü): güncellemeden sonra iki kez çalmasın. */
    private static void cancelLegacySound(Context context, AlarmManager alarmManager, String name, LocalDate date) {
        Intent intent = new Intent(context, PrayerSoundReceiver.class).setAction(ACTION_PRAYER_ALARM);
        int requestCode = ("sound_" + name + date.getDayOfYear()).hashCode();
        PendingIntent legacy = PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (legacy != null) {
            alarmManager.cancel(legacy);
            legacy.cancel();
        }
    }
}
