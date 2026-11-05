package com.islamyolu.app.VakitApi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.islamyolu.app.Service.PrayerNotificationService;

import java.util.Objects;

/**
 * Cihaz açılınca ve uygulama güncellenince: ezan/bildirim alarmları Room'daki vakitlerle yeniden
 * kurulur, ayar açıksa kalıcı vakit bildirimi yeniden başlatılır.
 * <p>
 * Android 12+ arka plandan ön plan servisi başlatmayı kısıtlar; BOOT_COMPLETED ve
 * MY_PACKAGE_REPLACED bu kısıttan muaftır. Servis türü specialUse olduğu için Android 15'in açılışta
 * yasakladığı türlere (dataSync vb.) girmez; önceden kullanıcıdan elle açması isteniyordu.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        boolean boot = Objects.equals(action, Intent.ACTION_BOOT_COMPLETED);
        boolean updated = Objects.equals(action, Intent.ACTION_MY_PACKAGE_REPLACED);
        if (!boot && !updated) {
            return;
        }
        // Kurulu alarmlar yeniden başlatma/güncellemeyle silinir; worker Room'daki vakitlerle yeniden kurar
        context.sendBroadcast(new Intent(context, DailyPrayerWorkerReceiver.class));
        PrayerNotificationService.startIfEnabled(context);
        Log.d(TAG, (boot ? "Cihaz açıldı" : "Uygulama güncellendi") + "; alarmlar ve kalıcı bildirim yeniden kuruluyor");
    }
}
