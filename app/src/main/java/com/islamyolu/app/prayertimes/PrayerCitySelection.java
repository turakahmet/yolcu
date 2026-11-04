package com.islamyolu.app.prayertimes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Kullanıcının Diyanet vakitleri için seçtiği şehir (Ülke → Eyalet/İl → Şehir/İlçe). */
public final class PrayerCitySelection {

    private final int cityId;
    private final String cityName;
    private final int stateId;
    @Nullable private final String stateName;
    private final int countryId;
    @Nullable private final String countryName;

    public PrayerCitySelection(int cityId, @NonNull String cityName, int stateId, @Nullable String stateName,
                               int countryId, @Nullable String countryName) {
        this.cityId = cityId;
        this.cityName = cityName;
        this.stateId = stateId;
        this.stateName = stateName;
        this.countryId = countryId;
        this.countryName = countryName;
    }

    public int getCityId() {
        return cityId;
    }

    @NonNull
    public String getCityName() {
        return cityName;
    }

    public int getStateId() {
        return stateId;
    }

    @Nullable
    public String getStateName() {
        return stateName;
    }

    public int getCountryId() {
        return countryId;
    }

    @Nullable
    public String getCountryName() {
        return countryName;
    }

    /** Kullanıcıya gösterim: "ALADAĞ, ADANA" (şehir ile eyalet aynıysa tek ad). */
    @NonNull
    public String getDisplayName() {
        if (stateName == null || stateName.equals(cityName)) {
            return cityName;
        }
        return cityName + ", " + stateName;
    }
}
