package com.myapp.yolcu.ui.weather;

import com.google.gson.annotations.SerializedName;

public class WeatherData {
    @SerializedName("latitude")
    private double latitude;
    
    @SerializedName("longitude")
    private double longitude;
    
    @SerializedName("timezone")
    private String timezone;
    
    @SerializedName("current_weather")
    private CurrentWeather currentWeather;

    public static class CurrentWeather {
        @SerializedName("temperature")
        private double temperature;
        
        @SerializedName("weathercode")
        private int weatherCode;
        
        @SerializedName("is_day")
        private int isDay;

        public double getTemperature() {
            return temperature;
        }

        public int getWeatherCode() {
            return weatherCode;
        }

        public boolean isDay() {
            return isDay == 1;
        }
    }

    public CurrentWeather getCurrentWeather() {
        return currentWeather;
    }

    public String getWeatherDescription() {
        if (currentWeather == null) return "Bilinmiyor";
        
        switch (currentWeather.getWeatherCode()) {
            case 0: return "Açık hava";
            case 1: return "Hafif bulutlu";
            case 2: return "Parçalı bulutlu";
            case 3: return "Kapalı";
            case 45: return "Sisli";
            case 48: return "Yoğun sis";
            case 51: return "Hafif çiseleme";
            case 53: return "Orta çiseleme";
            case 55: return "Yoğun çiseleme";
            case 61: return "Hafif yağmur";
            case 63: return "Orta yağmur";
            case 65: return "Yoğun yağmur";
            case 71: return "Hafif kar";
            case 73: return "Orta kar";
            case 75: return "Yoğun kar";
            case 95: return "Hafif fırtına";
            case 96: return "Fırtına ve dolu";
            case 99: return "Yoğun fırtına";
            default: return "Bilinmiyor";
        }
    }
} 