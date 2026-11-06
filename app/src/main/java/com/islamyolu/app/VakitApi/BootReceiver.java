package com.islamyolu.app.VakitApi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.islamyolu.app.Service.PrayerNotificationService;

/**
 * Alarmların yeniden kurulması gereken sistem olayları:
 * <ul>
 *   <li>Cihaz açılışı ve uygulama güncellemesi: kurulu alarmlar silinir; Room'daki vakitlerle yeniden
 *       kurulur, ayar açıksa kalıcı vakit bildirimi yeniden başlatılır.</li>
 *   <li>Saat veya saat dilimi değişimi: alarmlar kurulurken yalnızca o andan sonraki vakitler kurulur.
 *       Saat geri alınınca (ör. 10:00 → 05:18) geçmiş sayılıp atlanan vakitler yeniden geleceğe düşer;
 *       yeniden kurulmazsa uygulama açılana kadar bildirim gelmez.</li>
 * </ul>
 * Bu yayınların hepsi Android 8+'ın manifest alıcısı kısıtından muaftır. Android 12+ arka plandan ön plan
 * servisi başlatmayı kısıtlar; BOOT_COMPLETED ve MY_PACKAGE_REPLACED bu kısıttan muaftır (kalıcı bildirim
 * servisi saat değişimini kendisi dinler, burada başlatılmaz).
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_MY_PACKAGE_REPLACED:
                rescheduleAlarms(context, action);
                PrayerNotificationService.startIfEnabled(context);
                break;
            case Intent.ACTION_TIME_CHANGED:
            case Intent.ACTION_TIMEZONE_CHANGED:
                rescheduleAlarms(context, action);
                break;
            default:
                break;
        }
    }

    private void rescheduleAlarms(Context context, String reason) {
        Log.d(TAG, reason + ": alarmlar yeniden kuruluyor");
        PendingResult result = goAsync();
        PrayerAlarmScheduler.rescheduleAsync(context, result::finish);
    }
}
