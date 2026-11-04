package com.islamyolu.app.VakitApi;

import static com.islamyolu.app.main.MainActivity.SETTINGS_PREFS;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.islamyolu.app.R;
import com.islamyolu.app.Service.SharedPrefsHelperService;
import com.islamyolu.app.receiver.PrayerSoundReceiver;
import com.islamyolu.app.prayertimes.CurrentPrayerTimes;
import com.islamyolu.app.prayertimes.PrayerClock;
import com.islamyolu.app.prayertimes.PrayerSlot;
import com.islamyolu.app.prayertimes.PrayerTimeline;
import com.islamyolu.app.prayertimes.db.PrayerDayEntity;
import com.islamyolu.app.widget.PrayerTimesWidget;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DailyPrayerWorker extends Worker {
    private SharedPrefsHelperService sharedPrefsHelperService;
    private static final String TAG = "DailyPrayerWorker";

    public DailyPrayerWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        sharedPrefsHelperService = new SharedPrefsHelperService(context, SETTINGS_PREFS);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            // Vakitler Room'dan (Diyanet); kesin an şehrin o günkü gmt farkıyla hesaplanır (§5)
            Instant now = Instant.now();
            CurrentPrayerTimes current = CurrentPrayerTimes.load(getApplicationContext(), now);
            if (current.selection == null) {
                // Şehir seçilmemiş: seçim yapılınca PrayerTimesUpdateNotifier bu worker'ı tekrar çalıştırır
                Log.d(TAG, "Namaz vakti şehri seçilmemiş. Alarmlar kurulamıyor.");
                scheduleNextDayWorker();
                return Result.success();
            }
            LocalDate today = current.timeline.cityToday(now);
            if (today == null) {
                // Bugünün verisi yok: senkron yenileyince alarmlar yeniden kurulur
                Log.d(TAG, "Bugünün vakitleri Room'da yok. Alarmlar kurulamıyor.");
                scheduleNextDayWorker();
                return Result.success();
            }

            // Alarmları kur (boş vakitler atlanır)
            setPrayerAlarms(getApplicationContext(), current.timeline, today);
            pruneSentNotifications(getApplicationContext());
            sharedPrefsHelperService.removeStaleSettingsCityName();

            // Bir sonraki gün için worker'ı şehrin gece yarısına (00:02) planla; cihazın saat dilimine değil
            PrayerDayEntity todayData = current.timeline.day(today);
            Instant nextRun = todayData != null
                    ? PrayerClock.nextDayAt(today, LocalTime.of(0, 2), todayData.getGmtOffsetMinutes())
                    : null;
            scheduleNextDayWorker(nextRun);

            if (isWidgetActive(getApplicationContext(), PrayerTimesWidget.class)) {
                Log.d("DailyPrayerWorker", "widget güncellemesi yapıldı");

                PrayerTimesWidget widgetProvider = new PrayerTimesWidget();
                widgetProvider.scheduleNextUpdate(getApplicationContext());
                widgetProvider.scheduleWeatherUpdate(getApplicationContext());
            }

            return Result.success();
        } catch (Exception e) {
            Log.e("DailyPrayerWorker", "Hata: " + e.getMessage());
            return Result.retry();
        }
    }


    public static boolean isWidgetActive(Context context, Class<?> widgetClass) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, widgetClass);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
        return appWidgetIds.length > 0;
    }

    private void setPrayerAlarms(Context context, PrayerTimeline timeline, LocalDate today) {
        Calendar now = Calendar.getInstance();
        SharedPrefsHelperService prefsHelper = new SharedPrefsHelperService(context, SETTINGS_PREFS);
        int notificationMinutes = prefsHelper.getRadioButtonState("radioGroupVakit");
        int notificationMinusSabahEzaniIcin = 0;
        Log.d("DailyPrayerWorker", "Bildirim süresi: " + notificationMinutes + " dakika önce");

        // Vakit, ayar anahtarı ve bildirim/ses içinde kullanılan ad (mevcut request code'lar korunur)
        Object[][] prayerSwitchPairs = {
                {PrayerSlot.IMSAK, "switchNotificationImsak", "İmsak"},
                {PrayerSlot.GUNES, "switchNotificationSabah", "Güneş"},
                {PrayerSlot.OGLE, "switchNotificationOgle", "Öğle"},
                {PrayerSlot.IKINDI, "switchNotificationIkindi", "İkindi"},
                {PrayerSlot.AKSAM, "switchNotificationAksam", "Akşam"},
                {PrayerSlot.YATSI, "switchNotificationYatsi", "Yatsı"}
        };

        for (Object[] prayerInfo : prayerSwitchPairs) {
            PrayerSlot slot = (PrayerSlot) prayerInfo[0];
            String switchKey = (String) prayerInfo[1];
            String prayerName = (String) prayerInfo[2];

            try {
                Instant prayerInstant = timeline.instantOf(today, slot);

                // Önce bugünün bu vakit için kurulu alarmlarını kaldır: şehir değiştiyse eski şehrin saati,
                // bildirim kapatıldıysa kapatılan vakit çalmasın (aynı request code ile)
                Date idDate = prayerInstant != null ? new Date(prayerInstant.toEpochMilli()) : new Date();
                cancelAlarmForPrayer(context, idDate, prayerName);
                cancelPrayerSound(context, prayerName);

                if (!sharedPrefsHelperService.getSwitchState("switchNotificationAll") ||
                        !sharedPrefsHelperService.getSwitchState(switchKey)) {
                    continue;
                }
                if (prayerInstant == null) {
                    continue; // vakit oluşmuyor (kutup bölgesi): bildirim/alarm kurulmaz (§5)
                }
                // Kesin an şehrin o günkü gmt farkıyla (PrayerClock); cihaz saat dilimi kullanılmaz
                Calendar prayerCal = Calendar.getInstance();
                prayerCal.setTimeInMillis(prayerInstant.toEpochMilli());

                if (prayerCal.after(now)) {
                    String message = getNotificationMessage(getApplicationContext(), prayerName);
                    scheduleAlarm(getApplicationContext(), message, prayerCal.getTime(), notificationMinutes, prayerName);
                    // Ezan sesi için alarm (tam vaktinde)
                    if (switchKey.equals("switchNotificationSabah"))
                        notificationMinusSabahEzaniIcin = 60;
                    else
                        notificationMinusSabahEzaniIcin = 0;

                    schedulePrayerSound(context, prayerCal.getTime(), prayerName, notificationMinusSabahEzaniIcin);

                    Log.v(TAG, String.format(
                            "%s vakti için alarmlar kuruldu:\n" +
                                    "Ezan zamanı: %s",
                            prayerName,
                            prayerCal.getTime()
                    ));
                }
            } catch (Exception e) {
                Log.e(TAG, prayerName + " vakti için alarm kurulurken hata: " + e.getMessage());
            }
        }
    }

    private void cancelAlarmForPrayer(Context context, Date prayerTime, String prayerName) {
        try {
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction("com.myapp.yolcu.ACTION_ALARM_NOTIFICATION");

            // Aynı request code'u kullanarak PendingIntent oluştur
            int requestCode = generateNotificationId(prayerTime, prayerName).hashCode();

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                Log.d("DailyPrayerWorker", prayerName + " için alarm iptal edildi");
            }
        } catch (Exception e) {
            Log.e("DailyPrayerWorker", "Alarm iptal edilirken hata: " + e.getMessage());
        }
    }

    /** schedulePrayerSound ile aynı Intent ve request code: kurulmuşsa ezan sesi alarmını kaldırır. */
    private void cancelPrayerSound(Context context, String prayerName) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        Intent intent = new Intent(context, PrayerSoundReceiver.class);
        intent.setAction("com.myapp.yolcu.ACTION_ALARM_NOTIFICATION");
        int requestCode = ("sound_" + prayerName + Calendar.getInstance().get(Calendar.DAY_OF_YEAR)).hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private String getNotificationMessage(Context context, String prayerName) {
        String messageTemplate;
        switch (prayerName) {
            case "İmsak":
                messageTemplate = context.getString(R.string.imsak_vakit_notification_body);
                break;
            case "Güneş":
                messageTemplate = context.getString(R.string.gunes_vakit_notification_body);
                break;
            case "Öğle":
                messageTemplate = context.getString(R.string.ogle_vakit_notification_body);
                break;
            case "İkindi":
                messageTemplate = context.getString(R.string.ikindi_vakit_notification_body);
                break;
            case "Akşam":
                messageTemplate = context.getString(R.string.aksam_vakit_notification_body);
                break;
            case "Yatsı":
                messageTemplate = context.getString(R.string.yatsi_vakit_notification_body);
                break;
            default:
                Log.e("DailyPrayerWorker", "Bilinmeyen namaz vakti: " + prayerName);
                return "";
        }

        int notificationMinutes = sharedPrefsHelperService.getRadioButtonState("radioGroupVakit");
        return messageTemplate.replace("##", String.valueOf(notificationMinutes));
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleAlarm(Context context, String messageTemplate, Date prayerTime,
                               int notificationMinutes, String prayerName) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e("DailyPrayerWorker", prayerName + " için AlarmManager null!");
                return;
            }

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction("com.myapp.yolcu.ACTION_ALARM_NOTIFICATION");

            String message = messageTemplate.replace("##", String.valueOf(notificationMinutes));
            intent.putExtra("message", message);
            intent.putExtra("title", context.getString(R.string.namaz_vakit_notification_header));

            String notificationId = generateNotificationId(prayerTime, prayerName);
            intent.putExtra("notificationId", notificationId);

            int requestCode = notificationId.hashCode();

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Calendar notificationTime = Calendar.getInstance();
            notificationTime.setTime(prayerTime);
            notificationTime.add(Calendar.MINUTE, -notificationMinutes);

            if (notificationTime.getTimeInMillis() > System.currentTimeMillis()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        notificationTime.getTimeInMillis(),
                        pendingIntent
                );

                Log.w("DailyPrayerWorker", String.format(
                        "Alarm kuruldu: %s vakti için\n" +
                                "Namaz Vakti: %s\n" +
                                "Bildirim Zamanı: %s\n" +
                                "Bildirim Mesajı: %s\n" +
                                "Notification ID: %s",
                        prayerName,
                        prayerTime,
                        notificationTime.getTime(),
                        message,
                        notificationId
                ));
            }
        } catch (Exception e) {
            Log.e("DailyPrayerWorker", prayerName + " vakti için hata: " + e.getMessage());
        }
    }

    // Benzersiz bildirim ID'si oluştur
    private String generateNotificationId(Date prayerTime, String prayerName) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(prayerTime);
        return String.format(Locale.getDefault(), "%s_%d_%02d_%02d",
                prayerName,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
        );
    }

    /**
     * AlarmReceiver'ın "gönderildi" kayıtlarından ("Öğle_2026_11_04") 2 günden eski olanları siler;
     * önceden hiç temizlenmediği için dosya her gün büyüyordu. Kayıt adı generateNotificationId ile aynı biçimdedir.
     */
    private void pruneSentNotifications(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("SentNotifications", Context.MODE_PRIVATE);
        Calendar limit = Calendar.getInstance();
        limit.add(Calendar.DAY_OF_YEAR, -2);
        String oldestKept = String.format(Locale.ROOT, "%d_%02d_%02d",
                limit.get(Calendar.YEAR), limit.get(Calendar.MONTH) + 1, limit.get(Calendar.DAY_OF_MONTH));
        SharedPreferences.Editor editor = prefs.edit();
        int removed = 0;
        for (String key : prefs.getAll().keySet()) {
            int split = key.indexOf('_');
            String datePart = split >= 0 ? key.substring(split + 1) : "";
            // "yyyy_MM_dd" metin olarak sıralanabilir; biçim dışı kayıtlar da temizlenir
            if (datePart.length() != oldestKept.length() || datePart.compareTo(oldestKept) < 0) {
                editor.remove(key);
                removed++;
            }
        }
        if (removed > 0) {
            editor.apply();
            Log.d(TAG, removed + " eski bildirim kaydı temizlendi");
        }
    }

    /** Şehrin vakit verisi yoksa (seçim/veri yok, worker durduruldu) cihazın gece yarısına planlanır. */
    private void scheduleNextDayWorker() {
        scheduleNextDayWorker(null);
    }

    /** @param cityNextRun şehrin ertesi günü 00:02 (gmt farkıyla); null ise cihazın gece yarısı */
    private void scheduleNextDayWorker(@Nullable Instant cityNextRun) {
        try {
            Calendar nextRun = Calendar.getInstance();
            if (cityNextRun != null) {
                nextRun.setTimeInMillis(cityNextRun.toEpochMilli());
            } else {
                // Bir sonraki gün için zamanı ayarla (00:02)
                nextRun.add(Calendar.DAY_OF_YEAR, 1);
                nextRun.set(Calendar.HOUR_OF_DAY, 0);
                nextRun.set(Calendar.MINUTE, 2);
                nextRun.set(Calendar.SECOND, 0);
                nextRun.set(Calendar.MILLISECOND, 0);
            }

            long delay = Math.max(0, nextRun.getTimeInMillis() - System.currentTimeMillis());

            Log.d("DailyPrayerWorker", String.format(
                    "Bir sonraki gün için worker planlanıyor:\n" +
                            "Şu anki zaman: %s\n" +
                            "Planlanan zaman: %s\n" +
                            "Kalan süre: %d dakika",
                    new Date(),
                    nextRun.getTime(),
                    TimeUnit.MILLISECONDS.toMinutes(delay)
            ));

            // Work request oluştur
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(DailyPrayerWorker.class)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .addTag("DailyPrayerWork")
                    .build();

            // Mevcut worker'ı iptal et ve yenisini planla
            WorkManager.getInstance(getApplicationContext())
                    .enqueueUniqueWork(
                            "DailyPrayerWork",
                            ExistingWorkPolicy.REPLACE,
                            workRequest
                    );

            Log.d("DailyPrayerWorker", String.format(
                    "Worker başarıyla planlandı:\n" +
                            "Work ID: %s\n" +
                            "İşlem sırası:\n" +
                            "1. Gece 00:02'de çalışacak\n" +
                            "2. Şehir bilgisini kontrol edecek\n" +
                            "3. Yeni günün namaz vakitlerini hesaplayacak\n" +
                            "4. Tüm gün için alarmları kuracak\n" +
                            "5. Bir sonraki gün için kendini tekrar planlayacak",
                    workRequest.getId()
            ));

            // Worker durumunu log'la
            Log.d("DailyPrayerWorker", String.format(
                    "Worker durumu:\n" +
                            "ID: %s\n" +
                            "Planlanan zaman: %s\n" +
                            "Tag: DailyPrayerWork",
                    workRequest.getId(),
                    nextRun.getTime()
            ));

        } catch (Exception e) {
            Log.e("DailyPrayerWorker", String.format(
                    "Worker planlanırken hata oluştu:\n" +
                            "Hata: %s\n" +
                            "Stack trace: %s",
                    e.getMessage(),
                    Log.getStackTraceString(e)
            ));
        }
    }


    @SuppressLint("ScheduleExactAlarm")
    private void schedulePrayerSound(Context context, Date prayerTime, String prayerName, int notificationMinusSabahEzaniIcin) {
        try {
            SharedPrefsHelperService prefsHelper = new SharedPrefsHelperService(context, SETTINGS_PREFS);
            int ezanType = prefsHelper.getSpinnerSelection("spinner_ezan_sound", 3);
            int duaType = prefsHelper.getSpinnerSelection("spinner_ezan_dua", 3);

            if (ezanType == 4 && duaType == 2) { // Her ikisi de kapalı
                return;
            }

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            Intent intent = new Intent(context, PrayerSoundReceiver.class);
            intent.setAction("com.myapp.yolcu.ACTION_ALARM_NOTIFICATION");
            intent.putExtra("ezanType", ezanType);
            intent.putExtra("duaType", duaType);

            Log.d(TAG, String.format(
                    "Ezan Sesi değerleri:\n" +
                            "ezanType: %d\n" +
                            "duaType: %d",
                    intent.getIntExtra("ezanType", -1),
                    intent.getIntExtra("duaType", -1)
            ));

            int requestCode = ("sound_" + prayerName + Calendar.getInstance().get(Calendar.DAY_OF_YEAR)).hashCode();

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Calendar notificationTime = Calendar.getInstance();
            notificationTime.setTime(prayerTime);
            notificationTime.add(Calendar.MINUTE, -notificationMinusSabahEzaniIcin);
            if (notificationTime.getTimeInMillis() > System.currentTimeMillis()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        notificationTime.getTimeInMillis(),
                        pendingIntent
                );

                Log.d(TAG, String.format(
                        "Ezan sesi alarmı kuruldu:\n" +
                                "Vakit: %s\n" +
                                "Zaman: %s\n" +
                                "Ezan Tipi: %d\n" +
                                "Dua Tipi: %d",
                        prayerName,
                        notificationTime.getTime(),
                        ezanType,
                        duaType
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Ezan sesi alarmı kurulurken hata: " + e.getMessage());
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        Log.d("DailyPrayerWorker", "Worker durduruldu. Bir sonraki çalışma için planlanacak.");
        // Worker durdurulduğunda bir sonraki günü planla
        scheduleNextDayWorker();
    }
}

