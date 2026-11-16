package com.islamyolu.app.preferences;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Hava durumu önbelleği ("WeatherPrefs"). */
public final class WeatherPreferences {

    private static final String KEY_LAST_UPDATE = "last_update";
    private static final String KEY_DATA = "weather_data";
    private static final String KEY_LOCATION = "weather_location";

    private final SharedPreferences prefs;

    WeatherPreferences(@NonNull SharedPreferences prefs) {
        this.prefs = prefs;
    }

    /** Son başarılı güncelleme (epoch ms); hiç yoksa 0. */
    public long getLastUpdate() {
        return prefs.getLong(KEY_LAST_UPDATE, 0);
    }

    public void setLastUpdate(long epochMillis) {
        prefs.edit().putLong(KEY_LAST_UPDATE, epochMillis).apply();
    }

    /** Bir sonraki istekte önbellek yerine sunucudan yenilenmesini sağlar. */
    public void invalidate() {
        setLastUpdate(0);
    }

    /** Kayıtlı hava durumu JSON'u; yoksa null. */
    @Nullable
    public String getDataJson() {
        return prefs.getString(KEY_DATA, null);
    }

    public void setDataJson(@NonNull String json) {
        prefs.edit().putString(KEY_DATA, json).apply();
    }

    /** Kayıtlı verinin konumu ("enlem,boylam"); eski kayıtlarda yok (null). */
    @Nullable
    public String getLocationKey() {
        return prefs.getString(KEY_LOCATION, null);
    }

    /** Veri, zaman ve konum birlikte yazılır: yarım kalmış önbellek oluşmaz. */
    public void save(@NonNull String json, long epochMillis, @NonNull String locationKey) {
        prefs.edit()
                .putString(KEY_DATA, json)
                .putLong(KEY_LAST_UPDATE, epochMillis)
                .putString(KEY_LOCATION, locationKey)
                .apply();
    }
}
