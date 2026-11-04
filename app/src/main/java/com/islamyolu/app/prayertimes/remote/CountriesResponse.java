package com.islamyolu.app.prayertimes.remote;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/** countries.json */
public class CountriesResponse {

    private int version;
    @Nullable private String generatedAt;
    @Nullable private List<Country> countries;

    public int getVersion() {
        return version;
    }

    @Nullable
    public String getGeneratedAt() {
        return generatedAt;
    }

    public List<Country> getCountries() {
        return countries != null ? countries : Collections.emptyList();
    }

    public static class Country {
        private int id;
        /** Türkçe ad (ör. ALMANYA) */
        @Nullable private String name;
        /** İngilizce ad (ör. GERMANY) */
        @Nullable private String code;
        /** Normalize edilmiş Türkçe ad (bkz. PlaceNameNormalizer) */
        @Nullable private String key;
        /** Normalize edilmiş İngilizce ad; Geocoder ülke adıyla önce bu karşılaştırılır */
        @Nullable private String codeKey;
        /** Eyalet sayısı */
        private int states;
        /** Şehir sayısı */
        private int cities;

        public int getId() {
            return id;
        }

        @Nullable
        public String getName() {
            return name;
        }

        @Nullable
        public String getCode() {
            return code;
        }

        @Nullable
        public String getKey() {
            return key;
        }

        @Nullable
        public String getCodeKey() {
            return codeKey;
        }

        public int getStates() {
            return states;
        }

        public int getCities() {
            return cities;
        }
    }
}
