package com.islamyolu.app.prayertimes.remote;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/** places/{countryId}.json: ülkenin eyaletleri (Türkiye'de il) ve şehirleri (Türkiye'de ilçe) */
public class PlacesResponse {

    private int version;
    private int countryId;
    @Nullable private String name;
    @Nullable private String code;
    @Nullable private List<State> states;

    public int getVersion() {
        return version;
    }

    public int getCountryId() {
        return countryId;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getCode() {
        return code;
    }

    public List<State> getStates() {
        return states != null ? states : Collections.emptyList();
    }

    public static class State {
        private int id;
        @Nullable private String name;
        @Nullable private String code;
        @Nullable private String key;
        @Nullable private List<City> cities;

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

        public List<City> getCities() {
            return cities != null ? cities : Collections.emptyList();
        }
    }

    public static class City {
        private int id;
        @Nullable private String name;
        @Nullable private String code;
        @Nullable private String key;

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
    }
}
