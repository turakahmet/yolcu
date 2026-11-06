package com.islamyolu.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.islamyolu.app.VakitApi.PrayerAlarmScheduler;
import com.islamyolu.app.VakitApi.PrayerSilencer;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.SettingsPreferences;
import com.islamyolu.app.sound.SoundManager;

/**
 * Vakit anı alarmı ({@link PrayerAlarmScheduler} kurar): ayara göre telefonu titreşime alır ve ezan/dua
 * sesini başlatır. İkisi tek alarmda: Doze'da aynı uygulamanın ardışık tam zamanlı alarmları kısıtlanır.
 * Ses çalma işi {@link SoundManager}'dadır (ön plan servisi; ses kısma tuşuyla susar).
 */
public class PrayerSoundReceiver extends BroadcastReceiver {
    private static final String TAG = "PrayerSoundReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!PrayerAlarmScheduler.ACTION_PRAYER_ALARM.equals(intent.getAction())) {
            Log.d(TAG, "Bilinmeyen action: " + intent.getAction());
            return;
        }
        // Önceki sürümün alarmlarında bu bayraklar yok: yalnızca ses çalardı
        boolean silence = intent.getBooleanExtra(PrayerAlarmScheduler.EXTRA_SILENCE, false);
        boolean playSound = intent.getBooleanExtra(PrayerAlarmScheduler.EXTRA_PLAY_SOUND, true);

        if (silence) {
            // Medya sesi zil modundan etkilenmez: titreşime alınsa da ezan medya ses seviyesinde çalar
            PrayerSilencer.silence(context);
        }
        if (playSound) {
            playPrayerSound(context);
        }
    }

    private void playPrayerSound(Context context) {
        SettingsPreferences settings = AppPreferences.from(context).settings();
        if (!settings.isAllNotificationsOn()) {
            Log.d(TAG, "Tüm bildirimler kapalı");
            return;
        }
        if (!settings.isNotificationSoundOn()) {
            Log.d(TAG, "Bildirim sesi kapalı");
            return;
        }
        // Seçim çalma anında güncel ayardan okunur (alarm kurulurken alarmın içine yazılmaz)
        int ezanType = settings.getEzanSoundType();
        int duaType = settings.getEzanDuaType();
        if (!SoundManager.from(context).startPrayerAudio(ezanType, duaType)) {
            // Yalnızca Android 12+'da tam zamanlı alarm izni kullanıcı tarafından kapatıldıysa olur
            // (Android 13+'da USE_EXACT_ALARM ile izin hep var); vakit bildirimi yine gösterilir
            Log.w(TAG, "Ezan sesi başlatılamadı (tam zamanlı alarm izni yok)");
        }
    }
}
