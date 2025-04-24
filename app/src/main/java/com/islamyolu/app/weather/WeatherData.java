package com.islamyolu.app.weather;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WeatherData {
    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("timezone")
    private String timezone;

    @SerializedName("current_weather")
    private CurrentWeather currentWeather;

    @SerializedName("daily")
    private DailyWeatherForecast daily;

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

    // Günlük tahminler
    public static class DailyWeatherForecast {
        @SerializedName("time")
        private List<String> dates;

        @SerializedName("weathercode")
        private List<Integer> weatherCodes;

        @SerializedName("temperature_2m_max")
        private List<Double> maxTemperatures;

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

    public CurrentWeather getCurrentWeather() {
        return currentWeather;
    }

    public String getWeatherDescription() {
        if (currentWeather == null && daily == null)
            return "Bilinmiyor";
        int value = 0;
        if (currentWeather != null)
            value = currentWeather.getWeatherCode();
        else if (daily.getWeatherCodes() != null && !daily.getWeatherCodes().isEmpty() && daily != null)
            value = daily.getWeatherCodes().get(0);

        switch (value) {
            // Açık Hava (0)
            case 0:
                return "Açık hava";

            // Bulutlu Durumlar (1-3, 5, 19)
            case 1:
                return "Az bulutlu";
            case 2:
                return "Parçalı bulutlu";
            case 3:
            case 5:
            case 19:
                return "Hava bulutlu";

            // Kum Fırtınası (8-9, 30-39)
            case 8:
            case 9:
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
                return "Kum fırtınası";

            // Sis (10-12, 28, 40-45)
            case 10:
            case 11:
            case 12:
            case 28:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
                return "Sisli";

            // Yoğun Sis (46-49)
            case 46:
            case 47:
            case 48:
            case 49:
                return "Yoğun sis";

            // Çiseleme (20, 50, 52, 54, 56-60, 62, 64)
            case 20:
            case 50:
            case 52:
            case 54:
            case 56:
            case 57:
            case 58:
            case 59:
            case 60:
            case 62:
            case 64:
                return "Yağmur çiseleme";

            // Yağmur (21, 51, 53, 55, 61, 63, 80, 91, 92)
            case 21:
            case 51:
            case 53:
            case 55:
            case 61:
            case 63:
            case 80:
            case 91:
            case 92:
                return "Yağmurlu";

            // Şiddetli Yağmur (25, 65, 81, 82)
            case 25:
            case 65:
            case 81:
            case 82:
                return "Şiddetli yağmur";

            // Kar (22, 70-77, 85, 86)
            case 22:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 77:
            case 85:
            case 86:
                return "Kar yağışlı";

            // Karışık Kar (23, 26, 27, 66-69, 83, 84, 87-90, 93, 94)
            case 23:
            case 26:
            case 27:
            case 66:
            case 67:
            case 68:
            case 69:
            case 83:
            case 84:
            case 87:
            case 88:
            case 89:
            case 90:
            case 93:
            case 94:
                return "Karla karışık yağış";

            // Kar Kristalleri (24, 76, 78, 79)
            case 24:
            case 76:
            case 78:
            case 79:
                return "Kar kristalleri";

            // Fırtına (17, 18, 13, 29, 98, 99)
            case 13:
            case 17:
            case 18:
            case 29:
            case 98:
            case 99:
                return "Fırtına";

            // Şiddetli Fırtına (95-97)
            case 95:
            case 96:
            case 97:
                return "Şiddetli fırtına";

            // Bilinmeyen Durumlar
            default:
                return "Bilinmiyor";
        }
    }
} 