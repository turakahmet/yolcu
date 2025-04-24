package com.islamyolu.app.weather;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WeatherDataList {
    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("timezone")
    private String timezone;

    @SerializedName("daily")
    private DailyWeatherForecast daily;

    public static class DailyWeatherForecast {
        @SerializedName("time")
        private List<String> dates;  // Tarihler

        @SerializedName("weathercode")
        private List<Integer> weatherCodes;  // Hava durumu kodları

        @SerializedName("temperature_2m_max")
        private List<Double> maxTemperatures;  // Maksimum sıcaklıklar

        public List<String> getDates() {
            return dates;
        }

        public List<Integer> getWeatherCodes() {
            return weatherCodes;
        }

        public List<Double> getMaxTemperatures() {
            return maxTemperatures;
        }
    }

    public DailyWeatherForecast getDaily() {
        return daily;
    }
}
