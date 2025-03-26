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
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.islamyolu.app.R;
import com.islamyolu.app.Service.SharedPrefsHelperService;
import com.islamyolu.app.receiver.PrayerSoundReceiver;
import com.islamyolu.app.widget.PrayerTimesWidget;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
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
            String cityName = sharedPrefsHelperService.getCityNameFromPreferences();
            if (cityName == null || cityName.isEmpty()) {
                Log.d("DailyPrayerWorker", "Şehir bilgisi bulunamadı. Alarmlar kurulamıyor.");
                return Result.retry(); // Daha sonra tekrar dene
            }

            Log.d("DailyPrayerWorker", "DailyPrayerWorker çalışıyor - Şehir: " + cityName);

            // Namaz vakitlerini al
            VakitNoApi vakitNoApi = new VakitNoApi();
            NamazVakiti namazVakiti = vakitNoApi.localPrayerTimes(Objects.requireNonNull(City.fromCityName(cityName)).getLatitude(), Objects.requireNonNull(City.fromCityName(cityName)).getLongitude());

            if (namazVakiti == null) {
                Log.d("DailyPrayerWorker", "Namaz vakitleri alınamadı. Alarmlar kurulamıyor.");
                return Result.retry();
            }

            // Namaz vakitlerinin geçerli olup olmadığını kontrol et
            if (!isValidPrayerTimes(namazVakiti)) {
                Log.d("DailyPrayerWorker", "Namaz vakitleri geçersiz. Alarmlar kurulamıyor.");
                return Result.retry();
            }

            Log.d("DailyPrayerWorker", "Namaz vakitleri alındı: " +
                    "İmsak: " + namazVakiti.getImsak() + ", " +
                    "Güneş: " + namazVakiti.getGunes() + ", " +
                    "Öğle: " + namazVakiti.getOgle() + ", " +
                    "İkindi: " + namazVakiti.getIkindi() + ", " +
                    "Akşam: " + namazVakiti.getAksam() + ", " +
                    "Yatsı: " + namazVakiti.getYatsi());

            // Alarmları kur
            setPrayerAlarms(getApplicationContext(), namazVakiti);

            // Bir sonraki gün için worker'ı planla
            scheduleNextDayWorker();

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

    private boolean isValidPrayerTimes(NamazVakiti namazVakiti) {
        // Tüm vakitlerin geçerli olup olmadığını kontrol et
        return namazVakiti.getImsak() != null && !namazVakiti.getImsak().isEmpty() &&
                namazVakiti.getGunes() != null && !namazVakiti.getGunes().isEmpty() &&
                namazVakiti.getOgle() != null && !namazVakiti.getOgle().isEmpty() &&
                namazVakiti.getIkindi() != null && !namazVakiti.getIkindi().isEmpty() &&
                namazVakiti.getAksam() != null && !namazVakiti.getAksam().isEmpty() &&
                namazVakiti.getYatsi() != null && !namazVakiti.getYatsi().isEmpty();
    }


    private void setPrayerAlarms(Context context, NamazVakiti namazVakiti) {
        Calendar now = Calendar.getInstance();
        SharedPrefsHelperService prefsHelper = new SharedPrefsHelperService(context, SETTINGS_PREFS);
        int notificationMinutes = prefsHelper.getRadioButtonState("radioGroupVakit");
        int notificationMinusSabahEzaniIcin = 0;
        Log.d("DailyPrayerWorker", "Bildirim süresi: " + notificationMinutes + " dakika önce");

        String[][] prayerSwitchPairs = {
                {namazVakiti.getImsak(), "switchNotificationImsak", "İmsak"},
                {namazVakiti.getGunes(), "switchNotificationSabah", "Güneş"},
                {namazVakiti.getOgle(), "switchNotificationOgle", "Öğle"},
                {namazVakiti.getIkindi(), "switchNotificationIkindi", "İkindi"},
                {namazVakiti.getAksam(), "switchNotificationAksam", "Akşam"},
                {namazVakiti.getYatsi(), "switchNotificationYatsi", "Yatsı"}
        };

        for (String[] prayerInfo : prayerSwitchPairs) {
            String prayerTime = prayerInfo[0];
            String switchKey = prayerInfo[1];
            String prayerName = prayerInfo[2];

            if (!sharedPrefsHelperService.getSwitchState("switchNotificationAll") ||
                    !sharedPrefsHelperService.getSwitchState(switchKey)) {
                continue;
            }

            try {
                String[] timeParts = prayerTime.split(":");
                Calendar prayerCal = Calendar.getInstance();
                prayerCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
                prayerCal.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
                prayerCal.set(Calendar.SECOND, 0);

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

    // Bildirimin gönderilip gönderilmediğini kontrol et
    private boolean isNotificationSent(Context context, String notificationId) {
        SharedPreferences prefs = context.getSharedPreferences(
                "SentNotifications",
                Context.MODE_PRIVATE
        );
        return prefs.getBoolean(notificationId, false);
    }

    // Bildirimi gönderildi olarak işaretle
    private void markNotificationAsSent(Context context, String notificationId) {
        SharedPreferences prefs = context.getSharedPreferences(
                "SentNotifications",
                Context.MODE_PRIVATE
        );
        prefs.edit().putBoolean(notificationId, true).apply();
    }

    // Eski bildirimleri temizle (her gün 00:01'de çağrılacak)
    private void clearOldNotifications(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                "SentNotifications",
                Context.MODE_PRIVATE
        );
        prefs.edit().clear().apply();
        Log.d("DailyPrayerWorker", "Eski bildirim kayıtları temizlendi");
    }

    private void scheduleNextDayWorker() {
        try {
            // Bir sonraki gün için zamanı ayarla (00:02)
            Calendar nextRun = Calendar.getInstance();
            nextRun.add(Calendar.DAY_OF_YEAR, 1);
            nextRun.set(Calendar.HOUR_OF_DAY, 0);
            nextRun.set(Calendar.MINUTE, 2);
            nextRun.set(Calendar.SECOND, 0);
            nextRun.set(Calendar.MILLISECOND, 0);

            long delay = nextRun.getTimeInMillis() - System.currentTimeMillis();

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

    // Switch durumlarını loglama metodu
    private void logAllSwitchStates() {
        Log.d("DailyPrayerWorker", "Switch Durumları:");
        Log.d("DailyPrayerWorker", "switchNotificationAll: " +
                sharedPrefsHelperService.getSwitchState("switchNotificationAll"));
        Log.d("DailyPrayerWorker", "switchNotificationImsak: " +
                sharedPrefsHelperService.getSwitchState("switchNotificationImsak"));
        Log.d("DailyPrayerWorker", "switchNotificationSabah: " +
                sharedPrefsHelperService.getSwitchState("switchNotificationSabah"));
        Log.d("DailyPrayerWorker", "switchNotificationOgle: " +
                sharedPrefsHelperService.getSwitchState("switchNotificationOgle"));
        Log.d("DailyPrayerWorker", "switchNotificationIkindi: " +
                sharedPrefsHelperService.getSwitchState("switchNotificationIkindi"));
        Log.d("DailyPrayerWorker", "switchNotificationAksam: " +
                sharedPrefsHelperService.getSwitchState("switchNotificationAksam"));
        Log.d("DailyPrayerWorker", "switchNotificationYatsi: " +
                sharedPrefsHelperService.getSwitchState("switchNotificationYatsi"));
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

