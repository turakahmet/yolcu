package com.myapp.yolcu.ui.Service;

import static com.myapp.yolcu.ui.main.MainActivity.LOCATION_PREFS;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SharedPrefsHelperService {

    private static final String TAG = "SharedPrefsHelper";
    private final SharedPreferences sharedPreferences;
    private static final String RUN_KEY = "firstRunDefault";
    private static final String ALARM_INITIALIZED_KEY = "alarmSystemInitialized";
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
            setSpinnerSelection("spinner_ezan_sound", 2); // "Bildirim" seçeneği
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
        Log.d("SharedPrefsHelper", "spinner_ezan_sound: " + getSpinnerSelection("spinner_ezan_sound", 2));
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
}
