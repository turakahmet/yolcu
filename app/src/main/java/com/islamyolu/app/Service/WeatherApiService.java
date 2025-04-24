package com.islamyolu.app.Service;

import com.islamyolu.app.weather.WeatherData;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {
    @GET("forecast")
    Call<WeatherData> getWeather(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("current_weather") boolean currentWeather,
            @Query("timezone") String timezone
    );

    @GET("forecast")
    Call<WeatherData> getDailyWeatherForecastByDate(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("daily") String dailyFields,  // Örnek: "weathercode,temperature_2m_max"
            @Query("start_date") String startDate,  // Format: YYYY-MM-DD
            @Query("end_date") String endDate,      // Format: YYYY-MM-DD
            @Query("timezone") String timezone
    );

    @GET("forecast")
    Call<WeatherData> getDailyWeatherAndForecastByDate(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("current_weather") boolean currentWeather,
            @Query("daily") String dailyFields,  // Örnek: "weathercode,temperature_2m_max"
            @Query("start_date") String startDate,  // Format: YYYY-MM-DD
            @Query("end_date") String endDate,      // Format: YYYY-MM-DD
            @Query("timezone") String timezone
    );
}
