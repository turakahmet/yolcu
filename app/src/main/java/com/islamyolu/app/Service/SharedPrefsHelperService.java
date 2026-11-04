package com.islamyolu.app.Service;

import static com.islamyolu.app.main.MainActivity.LOCATION_PREFS;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.islamyolu.app.main.MainActivity;
import com.islamyolu.app.prayertimes.PrayerCitySelection;

public class SharedPrefsHelperService {

    private static final String TAG = "SharedPrefsHelper";
    private final SharedPreferences sharedPreferences;
    private static final String RUN_KEY = "firstRunDefault";
    private final Context context;

    public static final String KEY_VAKIT_SOUND_MODE = "vakit_sound_mode_enabled";

    public SharedPrefsHelperService(Context context, String prefsName) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
    }

    public void setSwitchState(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public void setRadioButtonState(String key, int value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public int getRadioButtonState(String key) {
        return sharedPreferences.getInt(key, 10);
    }

    public boolean getSwitchState(String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    public void setSpinnerSelection(String key, int position) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, position);
        editor.apply();
        Log.d("SharedPrefsHelper", key + " spinner position saved: " + position);
    }

    public int getSpinnerSelection(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    public void defaultSettingPreference() {
        if (!getSwitchState(RUN_KEY)) {
            Log.d("SharedPrefsHelper", "İlk çalıştırma - Varsayılan ayarlar yapılandırılıyor");

            // Varsayılan bildirim ayarlarını kur
            setSwitchState("switchNotificationAll", true);
            setSwitchState("switchNotificationImsak", false);
            setSwitchState("switchNotificationSabah", true);
            setSwitchState("switchNotificationOgle", true);
            setSwitchState("switchNotificationIkindi", true);
            setSwitchState("switchNotificationAksam", true);
            setSwitchState("switchNotificationYatsi", true);
            setRadioButtonState("radioGroupVakit", 10);
            setSwitchState("switchNotificationKerahat", true);
            setSwitchState("switchNotificationSound", true);
            setSwitchState("vakitTitresim", false);

            // Spinner varsayılan değerleri
            setSpinnerSelection("spinner_ezan_sound", 3); // "Bildirim" seçeneği
            setSpinnerSelection("spinner_ezan_dua", 2);  // "Kapalı" seçeneği

            setSwitchState(RUN_KEY, true);

            // Ayarların kaydedildiğini kontrol et ve logla
            logAllSettings();
        }
    }

    private void logAllSettings() {
        Log.d("SharedPrefsHelper", "Mevcut Ayarlar:");
        Log.d("SharedPrefsHelper", "switchNotificationAll: " + getSwitchState("switchNotificationAll"));
        Log.d("SharedPrefsHelper", "switchNotificationImsak: " + getSwitchState("switchNotificationImsak"));
        Log.d("SharedPrefsHelper", "switchNotificationSabah: " + getSwitchState("switchNotificationSabah"));
        Log.d("SharedPrefsHelper", "switchNotificationOgle: " + getSwitchState("switchNotificationOgle"));
        Log.d("SharedPrefsHelper", "switchNotificationIkindi: " + getSwitchState("switchNotificationIkindi"));
        Log.d("SharedPrefsHelper", "switchNotificationAksam: " + getSwitchState("switchNotificationAksam"));
        Log.d("SharedPrefsHelper", "switchNotificationYatsi: " + getSwitchState("switchNotificationYatsi"));
        Log.d("SharedPrefsHelper", "radioGroupVakit: " + getRadioButtonState("radioGroupVakit"));
        Log.d("SharedPrefsHelper", "spinner_ezan_sound: " + getSpinnerSelection("spinner_ezan_sound", 3));
        Log.d("SharedPrefsHelper", "spinner_ezan_dua: " + getSpinnerSelection("spinner_ezan_dua", 2));
    }

    public String getCityNameFromPreferences() {
        SharedPreferences prefs = context
                .getSharedPreferences(LOCATION_PREFS, Context.MODE_PRIVATE);
        return prefs.getString("city_name", null);
    }

    public void setVakitSoundModeEnabled(boolean enabled) {
        Log.d(TAG, "Vakit ses modu ayarı kaydediliyor: " + enabled);
        sharedPreferences.edit().putBoolean(KEY_VAKIT_SOUND_MODE, enabled).apply();
    }

    public boolean isVakitSoundModeEnabled() {
        boolean enabled = sharedPreferences.getBoolean(KEY_VAKIT_SOUND_MODE, false);
        Log.d(TAG, "Vakit ses modu ayarı okunuyor: " + enabled);
        return enabled;
    }

    /**
     * Eski hatanın ayar dosyasına (SettingPrefs) yazdığı "city_name" kopyasını siler; hiçbir yer okumuyor.
     * Doğru kayıt LOCATION_PREFS'tedir ve dokunulmaz. Kopya yoksa bir şey yapmaz.
     */
    public void removeStaleSettingsCityName() {
        SharedPreferences settings = context.getSharedPreferences(MainActivity.SETTINGS_PREFS, Context.MODE_PRIVATE);
        if (settings.contains("city_name")) {
            settings.edit().remove("city_name").apply();
            Log.d(TAG, "Ayar dosyasındaki eski city_name kopyası silindi");
        }
    }

    /** Okuma ile aynı dosyaya (LOCATION_PREFS) yazar; önceden yanlışlıkla ayar dosyasına yazıyordu. */
    public void setCityNameFromPreferences(String cityName) {
        locationPreferences().edit().putString("city_name", cityName).apply();
    }

    // --- Diyanet vakitleri için seçili şehir (LOCATION_PREFS; eski "city_name" akışından bağımsız) ---

    private static final String KEY_PRAYER_CITY_ID = "prayer_city_id";
    private static final String KEY_PRAYER_CITY_NAME = "prayer_city_name";
    private static final String KEY_PRAYER_STATE_ID = "prayer_state_id";
    private static final String KEY_PRAYER_STATE_NAME = "prayer_state_name";
    private static final String KEY_PRAYER_COUNTRY_ID = "prayer_country_id";
    private static final String KEY_PRAYER_COUNTRY_NAME = "prayer_country_name";
    private static final int NO_ID = -1;

    public void savePrayerCitySelection(PrayerCitySelection selection) {
        locationPreferences().edit()
                .putInt(KEY_PRAYER_CITY_ID, selection.getCityId())
                .putString(KEY_PRAYER_CITY_NAME, selection.getCityName())
                .putInt(KEY_PRAYER_STATE_ID, selection.getStateId())
                .putString(KEY_PRAYER_STATE_NAME, selection.getStateName())
                .putInt(KEY_PRAYER_COUNTRY_ID, selection.getCountryId())
                .putString(KEY_PRAYER_COUNTRY_NAME, selection.getCountryName())
                .apply();
    }

    /** Seçim yapılmamışsa null. */
    public PrayerCitySelection getPrayerCitySelection() {
        SharedPreferences prefs = locationPreferences();
        int cityId = prefs.getInt(KEY_PRAYER_CITY_ID, NO_ID);
        String cityName = prefs.getString(KEY_PRAYER_CITY_NAME, null);
        if (cityId == NO_ID || cityName == null) {
            return null;
        }
        return new PrayerCitySelection(cityId, cityName,
                prefs.getInt(KEY_PRAYER_STATE_ID, NO_ID), prefs.getString(KEY_PRAYER_STATE_NAME, null),
                prefs.getInt(KEY_PRAYER_COUNTRY_ID, NO_ID), prefs.getString(KEY_PRAYER_COUNTRY_NAME, null));
    }

    // --- Eski sürümden tek seferlik taşıma (city_name → prayer_city_*) ---

    private static final String KEY_LEGACY_MIGRATION = "prayer_legacy_migration";

    /** Taşıma sonucu ("migrated", "declined", "no_match", "no_legacy"); hiç denenmediyse null. */
    public String getLegacyMigrationState() {
        return locationPreferences().getString(KEY_LEGACY_MIGRATION, null);
    }

    public void setLegacyMigrationState(String state) {
        locationPreferences().edit().putString(KEY_LEGACY_MIGRATION, state).apply();
    }

    /**
     * Taşımayı geri alır: yalnızca yeni anahtarlar (prayer_city_* ve taşıma durumu) silinir; eski
     * "city_name" hiç değiştirilmediği için önceki durum aynen geri gelir.
     */
    public void revertLegacyMigration() {
        locationPreferences().edit()
                .remove(KEY_PRAYER_CITY_ID)
                .remove(KEY_PRAYER_CITY_NAME)
                .remove(KEY_PRAYER_STATE_ID)
                .remove(KEY_PRAYER_STATE_NAME)
                .remove(KEY_PRAYER_COUNTRY_ID)
                .remove(KEY_PRAYER_COUNTRY_NAME)
                .remove(KEY_LEGACY_MIGRATION)
                .apply();
    }

    private SharedPreferences locationPreferences() {
        return context.getSharedPreferences(LOCATION_PREFS, Context.MODE_PRIVATE);
    }
}
