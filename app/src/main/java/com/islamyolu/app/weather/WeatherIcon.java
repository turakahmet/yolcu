package com.islamyolu.app.weather;

import com.islamyolu.app.R;

import java.time.LocalTime;

public enum WeatherIcon {
    CLEAR_DAY(R.drawable.day_clear, new int[]{0}, true),
    CLEAR_NIGHT(R.drawable.night_clear, new int[]{0}, false),

    FEW_CLOUDS_DAY(R.drawable.day_few_clouds, new int[]{1}, true),
    FEW_CLOUDS_NIGHT(R.drawable.night_few_clouds, new int[]{1}, false),

    PARTLY_CLOUDY_DAY(R.drawable.day_partly_cloudy, new int[]{2}, true),
    PARTLY_CLOUDY_NIGHT(R.drawable.night_partly_cloudy, new int[]{2}, false),

    CLOUDY(R.drawable.cloudy, new int[]{3, 5, 19}, true),
    CLOUDY_NIGHT(R.drawable.night_few_clouds, new int[]{3, 5, 19}, false),

    SANDSTORM(R.drawable.sandstorm, new int[]{8, 9, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39}, true),
    SANDSTORM_NIGHT(R.drawable.sandstorm, new int[]{8, 9, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39}, false),

    FOG(R.drawable.fog, new int[]{10, 11, 12, 28, 40, 41, 42, 43, 44, 45}, true),
    FOG_NIGHT(R.drawable.fog, new int[]{10, 11, 12, 28, 40, 41, 42, 43, 44, 45}, false),

    HEAVY_FOG(R.drawable.heavy_fog, new int[]{46, 47, 48, 49}, true),
    HEAVY_FOG_NIGHT(R.drawable.heavy_fog, new int[]{46, 47, 48, 49}, false),

    DRIZZLE(R.drawable.drizzle_light, new int[]{20, 50, 52, 54, 59, 58, 57, 56, 60, 62, 64}, true),
    DRIZZLE_NIGHT(R.drawable.drizzle_light, new int[]{20, 50, 52, 54, 59, 58, 57, 56, 60, 62, 64}, false),

    RAIN_LIGHT(R.drawable.rain_light, new int[]{21, 51, 53, 55, 61, 63, 80, 91, 92}, true),
    RAIN_LIGHT_NIGHT(R.drawable.night_rain, new int[]{61, 63, 92}, false),

    RAIN_HEAVY(R.drawable.rain_heavy, new int[]{25, 65, 81, 82}, true),
    RAIN_HEAVY_NIGHT(R.drawable.rain_heavy, new int[]{25, 65, 81, 82}, false),

    SNOW_LIGHT(R.drawable.snow_light, new int[]{22, 70, 71, 72, 73, 74, 75, 77, 85, 86}, true),
    SNOW_LIGHT_NIGHT(R.drawable.night_snow, new int[]{22, 70, 71, 72, 73, 74, 75, 77, 85, 86}, false),
    SNOW_MIXED(R.drawable.snow_mixed, new int[]{23, 26, 27, 66, 67, 68, 69, 83, 84, 87, 88, 89, 90, 93, 94}, true),
    SNOW_MIXED_NIGHT(R.drawable.snow_mixed_night, new int[]{23, 26, 27, 66, 67, 68, 69, 83, 84, 87, 88, 89, 90, 93, 94}, false),
    SNOW_CRYSTAL(R.drawable.snow_crystals, new int[]{24, 76, 78, 79}, false),

    THUNDERSTORM(R.drawable.thunderstorm_light, new int[]{17, 18, 13, 29, 98, 99}, true),
    THUNDERSTORM_HEAVY(R.drawable.thunderstorm_heavy, new int[]{95, 96, 97}, true),
    UNKNOWN(R.drawable.unknown_weather, new int[]{-1}, true);

    private final int resourceId;
    private final int[] weatherCodes;
    private final boolean isDay;

    WeatherIcon(int resourceId, int[] weatherCodes, boolean isDay) {
        this.resourceId = resourceId;
        this.weatherCodes = weatherCodes;
        this.isDay = isDay;
    }

    public int getResourceId() {
        return resourceId;
    }

    public static int getWeatherIconResource(int weatherCode, boolean isDay) {
        for (WeatherIcon icon : WeatherIcon.values()) {
            for (int code : icon.weatherCodes) {
                if (code == weatherCode && (icon.isDay == isDay || weatherCode >= 3)) {
                    return icon.getResourceId();
                }
            }
        }
        return UNKNOWN.getResourceId();
    }

    public static int getWeatherBackgroundResource(int weatherCode) {
        LocalTime simdikiSaat = LocalTime.now();

        LocalTime geceBaslangic = LocalTime.of(19, 0);  // 19:00
        LocalTime gunduzBaslangic = LocalTime.of(7, 0); // 07:00

        if (simdikiSaat.isAfter(geceBaslangic) || simdikiSaat.isBefore(gunduzBaslangic)) {
            return R.drawable.dashboard_weather_4;
        } else {
            int value = getWeatherIconResource(weatherCode, true);

            if (value == R.drawable.rain_light ||
                    value == R.drawable.drizzle_light ||
                    value == R.drawable.rain_heavy ||
                    value == R.drawable.thunderstorm_light ||
                    value == R.drawable.thunderstorm_heavy) {
                return R.drawable.dashboard_weather_2;
            } else if (value == R.drawable.snow_light ||
                    value == R.drawable.night_snow ||
                    value == R.drawable.snow_mixed ||
                    value == R.drawable.snow_mixed_night ||
                    value == R.drawable.snow_crystals) {
                return R.drawable.dashboard_weather_3;
            } else {
                return R.drawable.dashboard_weather_1;
            }
        }
    }
}

