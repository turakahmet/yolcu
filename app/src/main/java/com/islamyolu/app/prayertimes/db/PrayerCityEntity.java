package com.islamyolu.app.prayertimes.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * prayer_city: indirilen şehrin meta bilgisi ve senkronizasyon durumu.
 */
@Entity(tableName = "prayer_city")
public class PrayerCityEntity {

    @PrimaryKey
    private final int cityId;
    @Nullable private final String name;
    @Nullable private final String state;
    @Nullable private final Integer stateId;
    @Nullable private final String country;
    @Nullable private final Integer countryId;
    /** Dosyanın şema sürümü (times/{cityId}.json "version") */
    private final int schemaVersion;
    /** Botun dosyayı ürettiği an (ISO-8601, "updatedAt") */
    @Nullable private final String serverUpdatedAt;
    /** "yyyy-MM-dd" */
    @NonNull private final String validFrom;
    /** "yyyy-MM-dd" */
    @NonNull private final String validTo;
    /** Son başarılı senkronizasyon (epoch milisaniye; 304 yanıtında da güncellenir) */
    private final long syncedAt;
    /** If-None-Match için sunucunun ETag değeri */
    @Nullable private final String etag;

    public PrayerCityEntity(int cityId, @Nullable String name, @Nullable String state,
                            @Nullable Integer stateId, @Nullable String country,
                            @Nullable Integer countryId, int schemaVersion,
                            @Nullable String serverUpdatedAt, @NonNull String validFrom,
                            @NonNull String validTo, long syncedAt, @Nullable String etag) {
        this.cityId = cityId;
        this.name = name;
        this.state = state;
        this.stateId = stateId;
        this.country = country;
        this.countryId = countryId;
        this.schemaVersion = schemaVersion;
        this.serverUpdatedAt = serverUpdatedAt;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.syncedAt = syncedAt;
        this.etag = etag;
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

    public int getSchemaVersion() {
        return schemaVersion;
    }

    @Nullable
    public String getServerUpdatedAt() {
        return serverUpdatedAt;
    }

    @NonNull
    public String getValidFrom() {
        return validFrom;
    }

    @NonNull
    public String getValidTo() {
        return validTo;
    }

    public long getSyncedAt() {
        return syncedAt;
    }

    @Nullable
    public String getEtag() {
        return etag;
    }
}
