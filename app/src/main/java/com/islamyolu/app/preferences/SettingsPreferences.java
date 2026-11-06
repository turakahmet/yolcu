package com.islamyolu.app.preferences;

import android.annotation.SuppressLint;
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

    /** Altı vaktin bildirim anahtarları, gün içindeki sırayla (vakit başına ayarlar bunlara bağlanır). */
    public static final String[] PRAYER_KEYS = {
            KEY_NOTIFY_IMSAK, KEY_NOTIFY_SABAH, KEY_NOTIFY_OGLE, KEY_NOTIFY_IKINDI, KEY_NOTIFY_AKSAM, KEY_NOTIFY_YATSI
    };
    /** Vakit başına hatırlatma süresi/açıklığı: "<vakit anahtarı><sonek>"; yoksa genel ayar geçerli. */
    private static final String SUFFIX_REMINDER_MINUTES = "_reminderMinutes";
    private static final String SUFFIX_REMINDER_ON = "_reminderOn";

    private static final String KEY_REMINDER_MINUTES = "radioGroupVakit";
    private static final String KEY_EZAN_SOUND = "spinner_ezan_sound";
    private static final String KEY_EZAN_DUA = "spinner_ezan_dua";
    private static final String KEY_VAKIT_SOUND_MODE = "vakit_sound_mode_enabled";
    private static final String KEY_KERAHAT_NOTIFY = "kerahatNotify";
    private static final String KEY_SILENCE_MINUTES = "vakitSilenceMinutes";
    private static final String KEY_SAVED_RINGER_MODE = "vakitSavedRingerMode";
    private static final String KEY_RINGER_RESTORE_AT = "vakitRingerRestoreAt";
    public static final int DEFAULT_SILENCE_MINUTES = 20;
    private static final String KEY_FIRST_RUN_DONE = "firstRunDefault";
    /** Eski bir hatanın bu dosyaya yazdığı il adı kopyası; hiçbir yer okumaz. */
    private static final String KEY_STALE_CITY_NAME = "city_name";

    public static final int DEFAULT_REMINDER_MINUTES = 10;
    /** Vakit öncesi uyarı seçenekleri (dakika) */
    private static final int[] REMINDER_OPTIONS = {5, 10, 15, 20, 30, 45};
    /**
     * Sabah namazı güneş doğunca biter; bildirim güneş doğuşundan önce gönderilir. Ezan genelde
     * güneşten ~1 saat önce okunduğu için 30 veya 60 dk seçilebilir.
     */
    private static final int[] SABAH_REMINDER_OPTIONS = {30, 60};
    public static final int DEFAULT_SABAH_REMINDER_MINUTES = 30;
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

    /**
     * Vaktin hatırlatma süresi (dakika). Süre artık yalnızca vakit başına ayarlanır; vakte özel kayıt
     * yoksa eski sürümdeki genel seçim ("radioGroupVakit", yeni kurulumda 10) kullanılır. Böylece
     * güncelleyen kullanıcının önceki seçimi bozulmaz.
     */
    public int getReminderMinutes(@NonNull String prayerKey) {
        if (KEY_NOTIFY_SABAH.equals(prayerKey)) {
            // Sabah: güneş doğuşundan önce yalnızca 30/60 dk; eski kısa süreler (5-20 dk) namazı
            // yetiştirmeye yetmediğinden geçerli seçeneklerden biri değilse varsayılana döner
            int saved = prefs.getInt(prayerKey + SUFFIX_REMINDER_MINUTES, DEFAULT_SABAH_REMINDER_MINUTES);
            for (int option : SABAH_REMINDER_OPTIONS) {
                if (option == saved) return saved;
            }
            return DEFAULT_SABAH_REMINDER_MINUTES;
        }
        return prefs.getInt(prayerKey + SUFFIX_REMINDER_MINUTES,
                prefs.getInt(KEY_REMINDER_MINUTES, DEFAULT_REMINDER_MINUTES));
    }

    /** Vakit detayında seçilebilecek hatırlatma süreleri (dakika). */
    @NonNull
    public static int[] reminderOptions(@NonNull String prayerKey) {
        return KEY_NOTIFY_SABAH.equals(prayerKey) ? SABAH_REMINDER_OPTIONS.clone() : REMINDER_OPTIONS.clone();
    }

    public void setReminderMinutes(@NonNull String prayerKey, int minutes) {
        prefs.edit().putInt(prayerKey + SUFFIX_REMINDER_MINUTES, minutes).apply();
    }

    /** Vakit öncesi uyarı açık mı (varsayılan açık; kapalıysa ezan sesi yine çalar). */
    public boolean isReminderOn(@NonNull String prayerKey) {
        return prefs.getBoolean(prayerKey + SUFFIX_REMINDER_ON, true);
    }

    public void setReminderOn(@NonNull String prayerKey, boolean on) {
        prefs.edit().putBoolean(prayerKey + SUFFIX_REMINDER_ON, on).apply();
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

    // --- Kerahat bildirimi ---

    public boolean isKerahatNotifyOn() {
        return prefs.getBoolean(KEY_KERAHAT_NOTIFY, false);
    }

    public void setKerahatNotify(boolean on) {
        prefs.edit().putBoolean(KEY_KERAHAT_NOTIFY, on).apply();
    }

    // --- Namazda titreşim: süre ve geri yüklenecek ses profili ---

    /** Titreşimde kalma süresi (dakika); sonra eski profile dönülür. */
    public int getSilenceMinutes() {
        return prefs.getInt(KEY_SILENCE_MINUTES, DEFAULT_SILENCE_MINUTES);
    }

    public void setSilenceMinutes(int minutes) {
        prefs.edit().putInt(KEY_SILENCE_MINUTES, minutes).apply();
    }

    /** Titreşime alınmadan önceki zil modu ve geri yükleme anı; titreşime alınmadıysa mod -1. */
    public int getSavedRingerMode() {
        return prefs.getInt(KEY_SAVED_RINGER_MODE, -1);
    }

    public long getRingerRestoreAt() {
        return prefs.getLong(KEY_RINGER_RESTORE_AT, 0L);
    }

    @SuppressLint("ApplySharedPref")
    public void saveRingerMode(int mode, long restoreAtMillis) {
        // commit: alıcı hemen ardından sonlanabilir; değer diske yazılmadan kaybolmasın
        prefs.edit().putInt(KEY_SAVED_RINGER_MODE, mode).putLong(KEY_RINGER_RESTORE_AT, restoreAtMillis).commit();
    }

    @SuppressLint("ApplySharedPref")
    public void clearSavedRingerMode() {
        prefs.edit().remove(KEY_SAVED_RINGER_MODE).remove(KEY_RINGER_RESTORE_AT).commit();
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
