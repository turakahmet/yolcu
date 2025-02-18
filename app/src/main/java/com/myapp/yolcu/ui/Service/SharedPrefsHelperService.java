package com.myapp.yolcu.ui.Service;

import static com.myapp.yolcu.ui.main.MainActivity.LOCATION_PREFS;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.annotation.SuppressLint;

import com.myapp.yolcu.ui.VakitApi.DailyPrayerWorkerReceiver;

import java.util.Calendar;

public class SharedPrefsHelperService {

    private SharedPreferences sharedPreferences;
    private static final String RUN_KEY = "firstRunDefault";
    private static final String ALARM_INITIALIZED_KEY = "alarmSystemInitialized";
    private final Context context;

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

    public void defaultSettingPreference() {
        if (!getSwitchState(RUN_KEY)) {
            Log.d("SharedPrefsHelper", "İlk çalıştırma - Varsayılan ayarlar yapılandırılıyor");
            
            // Varsayılan bildirim ayarlarını kur
            setSwitchState("switchNotificationAll", true);
            setSwitchState("switchNotificationImsak", true);
            setSwitchState("switchNotificationSabah", true);
            setSwitchState("switchNotificationOgle", true);
            setSwitchState("switchNotificationIkindi", true);
            setSwitchState("switchNotificationAksam", true);
            setSwitchState("switchNotificationYatsi", true);
            setRadioButtonState("radioGroupVakit", 10);
            setSwitchState("switchNotificationKerahat", true);
            setSwitchState("switchNotificationSound", true);
            setSwitchState("vakitTitresim", false);
            setSwitchState("eDonusumUserApiPathSegment",false);
            
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
    }

    public String getCityNameFromPreferences() {
        SharedPreferences prefs = context
                .getSharedPreferences(LOCATION_PREFS, Context.MODE_PRIVATE);
        return prefs.getString("city_name", null);
    }
}
