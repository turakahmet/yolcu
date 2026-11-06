package com.islamyolu.app.VakitApi;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

import androidx.annotation.NonNull;

import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.SettingsPreferences;

import java.time.Instant;

/**
 * "Namazda titreşime al": vakit girince telefonu titreşime alır, seçilen süre sonra eski profile döndürür.
 * <p>
 * Kurallar (kullanıcının kendi seçimini ezmemek için):
 * <ul>
 *   <li>Yalnızca zil modu "normal" ise titreşime alınır; zaten sessiz/titreşimdeyse veya Rahatsız
 *       Etmeyin açıksa dokunulmaz (sessiz moda geçmek ayrıca özel izin ister, bu yüzden titreşim kullanılır).</li>
 *   <li>Geri yüklemede telefon hâlâ titreşimdeyse eski moda döner; kullanıcı arada değiştirdiyse bırakılır.</li>
 *   <li>Eski mod ve geri yükleme anı tercihte saklanır: cihaz arada kapanıp açılsa da geri yükleme kaybolmaz.</li>
 * </ul>
 * Titreşime alma ezan sesiyle aynı alarmla tetiklenir (bkz. PrayerSoundReceiver): Doze'da aynı uygulamanın
 * ardışık tam zamanlı alarmları kısıtlandığı için ayrı alarm kurulmaz.
 */
public final class PrayerSilencer {

    private static final String TAG = "PrayerSilencer";
    private static final int RESTORE_REQUEST_CODE = 7302;

    private PrayerSilencer() {
    }

    /** Vakit girdi: telefon normal moddaysa titreşime alır ve geri yüklemeyi planlar. */
    public static void silence(@NonNull Context context) {
        AudioManager audio = context.getSystemService(AudioManager.class);
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (audio == null) return;
        if (audio.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            Log.d(TAG, "Zil zaten normal değil; dokunulmadı");
            return;
        }
        if (nm != null && nm.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_ALL) {
            Log.d(TAG, "Rahatsız Etmeyin açık; dokunulmadı");
            return;
        }
        SettingsPreferences settings = AppPreferences.from(context).settings();
        long restoreAt = System.currentTimeMillis() + settings.getSilenceMinutes() * 60_000L;
        try {
            audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        } catch (SecurityException e) {
            Log.w(TAG, "Zil modu değiştirilemedi", e);
            return;
        }
        settings.saveRingerMode(AudioManager.RINGER_MODE_NORMAL, restoreAt);
        scheduleRestore(context, restoreAt);
        Log.d(TAG, "Titreşime alındı; geri yükleme: " + Instant.ofEpochMilli(restoreAt));
    }

    /** Süre doldu: hâlâ bizim aldığımız titreşimdeyse eski moda döner. */
    static void restore(@NonNull Context context) {
        SettingsPreferences settings = AppPreferences.from(context).settings();
        int saved = settings.getSavedRingerMode();
        if (saved < 0) return;
        AudioManager audio = context.getSystemService(AudioManager.class);
        try {
            if (audio != null && audio.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                audio.setRingerMode(saved);
                Log.d(TAG, "Zil modu geri yüklendi");
            } else {
                Log.d(TAG, "Kullanıcı zil modunu değiştirmiş; geri yükleme atlandı");
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Zil modu geri yüklenemedi", e);
        } finally {
            settings.clearSavedRingerMode();
        }
    }

    /**
     * Alarmlar yeniden kurulurken çağrılır: cihaz yeniden başladıysa (alarmlar silinir) bekleyen geri
     * yüklemeyi yeniden planlar, süresi geçtiyse hemen geri yükler.
     */
    static void ensureRestorePending(@NonNull Context context) {
        SettingsPreferences settings = AppPreferences.from(context).settings();
        if (settings.getSavedRingerMode() < 0) return;
        long restoreAt = settings.getRingerRestoreAt();
        if (restoreAt <= System.currentTimeMillis()) {
            restore(context);
        } else {
            scheduleRestore(context, restoreAt);
        }
    }

    private static void scheduleRestore(Context context, long restoreAt) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        if (alarmManager == null) return;
        Intent intent = new Intent(context, RestoreReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, RESTORE_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PrayerAlarmScheduler.setAlarm(alarmManager, Instant.ofEpochMilli(restoreAt), pendingIntent);
    }

    /** Geri yükleme alarmının alıcısı (manifestte, dışa kapalı). */
    public static class RestoreReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            restore(context);
        }
    }
}
