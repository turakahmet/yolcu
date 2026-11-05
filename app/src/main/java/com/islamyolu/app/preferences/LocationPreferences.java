package com.islamyolu.app.preferences;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.islamyolu.app.prayertimes.PrayerCitySelection;

/**
 * Konuma bağlı tercihler ("location_prefs"): namaz vakti şehri, eski sürümün il adı ve son bilinen konum.
 */
public final class LocationPreferences {

    // Eski sürümün il adı (ör. "Isparta"); hava durumu ve geçiş için okunur
    private static final String KEY_LEGACY_CITY_NAME = "city_name";
    private static final String KEY_LAST_LAT = "last_known_latitude";
    private static final String KEY_LAST_LON = "last_known_longitude";

    // Diyanet vakitleri için seçili şehir
    private static final String KEY_PRAYER_CITY_ID = "prayer_city_id";
    private static final String KEY_PRAYER_CITY_NAME = "prayer_city_name";
    private static final String KEY_PRAYER_STATE_ID = "prayer_state_id";
    private static final String KEY_PRAYER_STATE_NAME = "prayer_state_name";
    private static final String KEY_PRAYER_COUNTRY_ID = "prayer_country_id";
    private static final String KEY_PRAYER_COUNTRY_NAME = "prayer_country_name";
    private static final String KEY_LEGACY_MIGRATION = "prayer_legacy_migration";
    private static final int NO_ID = -1;

    private final SharedPreferences prefs;

    LocationPreferences(@NonNull SharedPreferences prefs) {
        this.prefs = prefs;
    }

    // --- Eski il adı ---

    @Nullable
    public String getLegacyCityName() {
        return prefs.getString(KEY_LEGACY_CITY_NAME, null);
    }

    public void setLegacyCityName(@Nullable String cityName) {
        prefs.edit().putString(KEY_LEGACY_CITY_NAME, cityName).apply();
    }

    // --- Son bilinen konum ---

    public void saveLastKnownLocation(double latitude, double longitude) {
        prefs.edit()
                .putFloat(KEY_LAST_LAT, (float) latitude)
                .putFloat(KEY_LAST_LON, (float) longitude)
                .apply();
    }

    /** Kayıt yoksa {@link Float#NaN}. */
    public float getLastKnownLatitude() {
        return prefs.getFloat(KEY_LAST_LAT, Float.NaN);
    }

    /** Kayıt yoksa {@link Float#NaN}. */
    public float getLastKnownLongitude() {
        return prefs.getFloat(KEY_LAST_LON, Float.NaN);
    }

    // --- Namaz vakti şehri ---

    public void savePrayerCitySelection(@NonNull PrayerCitySelection selection) {
        prefs.edit()
                .putInt(KEY_PRAYER_CITY_ID, selection.getCityId())
                .putString(KEY_PRAYER_CITY_NAME, selection.getCityName())
                .putInt(KEY_PRAYER_STATE_ID, selection.getStateId())
                .putString(KEY_PRAYER_STATE_NAME, selection.getStateName())
                .putInt(KEY_PRAYER_COUNTRY_ID, selection.getCountryId())
                .putString(KEY_PRAYER_COUNTRY_NAME, selection.getCountryName())
                .apply();
    }

    /** Seçim yapılmamışsa null. */
    @Nullable
    public PrayerCitySelection getPrayerCitySelection() {
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

    /** Taşıma sonucu ("migrated", "declined", "no_match", "no_legacy"); hiç denenmediyse null. */
    @Nullable
    public String getLegacyMigrationState() {
        return prefs.getString(KEY_LEGACY_MIGRATION, null);
    }

    public void setLegacyMigrationState(@NonNull String state) {
        prefs.edit().putString(KEY_LEGACY_MIGRATION, state).apply();
    }

    /**
     * Taşımayı geri alır: yalnızca yeni anahtarlar (prayer_city_* ve taşıma durumu) silinir; eski
     * "city_name" hiç değiştirilmediği için önceki durum aynen geri gelir.
     */
    public void revertLegacyMigration() {
        prefs.edit()
                .remove(KEY_PRAYER_CITY_ID)
                .remove(KEY_PRAYER_CITY_NAME)
                .remove(KEY_PRAYER_STATE_ID)
                .remove(KEY_PRAYER_STATE_NAME)
                .remove(KEY_PRAYER_COUNTRY_ID)
                .remove(KEY_PRAYER_COUNTRY_NAME)
                .remove(KEY_LEGACY_MIGRATION)
                .apply();
    }
}
