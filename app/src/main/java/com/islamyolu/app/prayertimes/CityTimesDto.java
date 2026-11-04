package com.islamyolu.app.prayertimes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GitHub Pages v2 times/{cityId}.json dosyasının ayrıştırılmış hali.
 * Room'a yazılmadan önce {@link PrayerTimesValidator} ile doğrulanmalıdır.
 */
public final class CityTimesDto {

    private final int version;
    private final int cityId;
    @Nullable private final String name;
    @Nullable private final String state;
    @Nullable private final Integer stateId;
    @Nullable private final String country;
    @Nullable private final Integer countryId;
    @Nullable private final String updatedAt;
    @NonNull private final String validFrom;
    @NonNull private final String validTo;
    @NonNull private final List<PrayerDayDto> days;

    public CityTimesDto(int version, int cityId,
                        @Nullable String name, @Nullable String state, @Nullable Integer stateId,
                        @Nullable String country, @Nullable Integer countryId,
                        @Nullable String updatedAt, @NonNull String validFrom, @NonNull String validTo,
                        @NonNull List<PrayerDayDto> days) {
        this.version = version;
        this.cityId = cityId;
        this.name = name;
        this.state = state;
        this.stateId = stateId;
        this.country = country;
        this.countryId = countryId;
        this.updatedAt = updatedAt;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.days = Collections.unmodifiableList(new ArrayList<>(days));
    }

    public int getVersion() {
        return version;
    }

    public int getCityId() {
        return cityId;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getState() {
        return state;
    }

    @Nullable
    public Integer getStateId() {
        return stateId;
    }

    @Nullable
    public String getCountry() {
        return country;
    }

    @Nullable
    public Integer getCountryId() {
        return countryId;
    }

    @Nullable
    public String getUpdatedAt() {
        return updatedAt;
    }

    @NonNull
    public String getValidFrom() {
        return validFrom;
    }

    @NonNull
    public String getValidTo() {
        return validTo;
    }

    @NonNull
    public List<PrayerDayDto> getDays() {
        return days;
    }
}
