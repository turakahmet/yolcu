package com.islamyolu.app.preferences;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Bildirim, ezan sesi ve hatırlatma ayarları ("SettingPrefs").
 * Anahtar adları önceki sürümlerle aynıdır.
 */
public final class SettingsPreferences {

    private static final String TAG = "SettingsPreferences";

    // Bildirim anahtarları (Ayarlar ekranındaki anahtarlarla aynı adlar)
    public static final String KEY_NOTIFY_ALL = "switchNotificationAll";
    public static final String KEY_NOTIFY_IMSAK = "switchNotificationImsak";
    public static final String KEY_NOTIFY_SABAH = "switchNotificationSabah";
    public static final String KEY_NOTIFY_OGLE = "switchNotificationOgle";
    public static final String KEY_NOTIFY_IKINDI = "switchNotificationIkindi";
    public static final String KEY_NOTIFY_AKSAM = "switchNotificationAksam";
    public static final String KEY_NOTIFY_YATSI = "switchNotificationYatsi";
    public static final String KEY_KERAHAT = "switchNotificationKerahat";
    public static final String KEY_SOUND = "switchNotificationSound";
    public static final String KEY_ALWAYS_SHOW = "switchNotificationVakitHerzamanGoster";
    public static final String KEY_VIBRATE = "vakitTitresim";

    private static final String KEY_REMINDER_MINUTES = "radioGroupVakit";
    private static final String KEY_EZAN_SOUND = "spinner_ezan_sound";
    private static final String KEY_EZAN_DUA = "spinner_ezan_dua";
    private static final String KEY_VAKIT_SOUND_MODE = "vakit_sound_mode_enabled";
    private static final String KEY_FIRST_RUN_DONE = "firstRunDefault";
    /** Eski bir hatanın bu dosyaya yazdığı il adı kopyası; hiçbir yer okumaz. */
    private static final String KEY_STALE_CITY_NAME = "city_name";

    public static final int DEFAULT_REMINDER_MINUTES = 10;
    /** Ezan sesi: 0 Ezan, 1 Kısa ezan, 2 Muhabbet, 3 Kısa bildirim, 4 Kapalı. */
    public static final int EZAN_SOUND_SHORT_NOTIFICATION = 3;
    public static final int EZAN_SOUND_OFF = 4;
    /** Ezan duası: 0 Dua, 1 Dua ve meal, 2 Kapalı. */
    public static final int EZAN_DUA_OFF = 2;

    private final SharedPreferences prefs;

    SettingsPreferences(@NonNull SharedPreferences prefs) {
        this.prefs = prefs;
    }

    // --- Genel anahtar erişimi (Ayarlar ekranı anahtarları listeyle yönetir) ---

    public boolean isSwitchOn(@NonNull String key) {
        return prefs.getBoolean(key, false);
    }

    public void setSwitch(@NonNull String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    // --- Sık kullanılan ayarlar ---

    public boolean isAllNotificationsOn() {
        return isSwitchOn(KEY_NOTIFY_ALL);
    }

    public boolean isNotificationSoundOn() {
        return isSwitchOn(KEY_SOUND);
    }

    public boolean isKerahatOn() {
        return isSwitchOn(KEY_KERAHAT);
    }

    public boolean isAlwaysShowOn() {
        return isSwitchOn(KEY_ALWAYS_SHOW);
    }

    public void setAlwaysShow(boolean value) {
        setSwitch(KEY_ALWAYS_SHOW, value);
    }

    /** Vakitten kaç dakika önce hatırlatılacağı (5, 10 veya 20). */
    public int getReminderMinutes() {
        return prefs.getInt(KEY_REMINDER_MINUTES, DEFAULT_REMINDER_MINUTES);
    }

    public void setReminderMinutes(int minutes) {
        prefs.edit().putInt(KEY_REMINDER_MINUTES, minutes).apply();
    }

    public int getEzanSoundType() {
        return prefs.getInt(KEY_EZAN_SOUND, EZAN_SOUND_SHORT_NOTIFICATION);
    }

    public void setEzanSoundType(int type) {
        prefs.edit().putInt(KEY_EZAN_SOUND, type).apply();
    }

    public int getEzanDuaType() {
        return prefs.getInt(KEY_EZAN_DUA, EZAN_DUA_OFF);
    }

    public void setEzanDuaType(int type) {
        prefs.edit().putInt(KEY_EZAN_DUA, type).apply();
    }

    public boolean isVakitSoundModeOn() {
        return prefs.getBoolean(KEY_VAKIT_SOUND_MODE, false);
    }

    public void setVakitSoundMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_VAKIT_SOUND_MODE, enabled).apply();
    }

    /** İlk çalıştırmada varsayılan bildirim ayarlarını yazar; sonraki çalıştırmalarda bir şey yapmaz. */
    public void applyDefaultsOnFirstRun() {
        if (prefs.getBoolean(KEY_FIRST_RUN_DONE, false)) {
            return;
        }
        prefs.edit()
                .putBoolean(KEY_NOTIFY_ALL, true)
                .putBoolean(KEY_NOTIFY_IMSAK, false)
                .putBoolean(KEY_NOTIFY_SABAH, true)
                .putBoolean(KEY_NOTIFY_OGLE, true)
                .putBoolean(KEY_NOTIFY_IKINDI, true)
                .putBoolean(KEY_NOTIFY_AKSAM, true)
                .putBoolean(KEY_NOTIFY_YATSI, true)
                .putInt(KEY_REMINDER_MINUTES, DEFAULT_REMINDER_MINUTES)
                .putBoolean(KEY_KERAHAT, true)
                .putBoolean(KEY_SOUND, true)
                .putBoolean(KEY_VIBRATE, false)
                .putInt(KEY_EZAN_SOUND, EZAN_SOUND_SHORT_NOTIFICATION)
                .putInt(KEY_EZAN_DUA, EZAN_DUA_OFF)
                .putBoolean(KEY_FIRST_RUN_DONE, true)
                .apply();
        Log.d(TAG, "İlk çalıştırma: varsayılan bildirim ayarları yazıldı");
    }

    /** Eski hatanın bu dosyaya yazdığı il adı kopyasını siler; doğru kayıt konum tercihlerindedir. */
    public void removeStaleCityName() {
        if (prefs.contains(KEY_STALE_CITY_NAME)) {
            prefs.edit().remove(KEY_STALE_CITY_NAME).apply();
            Log.d(TAG, "Ayar dosyasındaki eski city_name kopyası silindi");
        }
    }
}
