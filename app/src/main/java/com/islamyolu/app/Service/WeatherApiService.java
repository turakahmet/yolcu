package com.islamyolu.app.Service;

import com.islamyolu.app.weather.WeatherData;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherApiService {

    /**
     * Anlık durum ve günlük tahmin tek istekte (Open-Meteo, ücretsiz, anahtarsız).
     *
     * @param current      ör. "temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code,is_day"
     * @param daily        ör. "weather_code,temperature_2m_max"
     * @param forecastDays bugün dahil gün sayısı
     */
    @GET("forecast")
    Call<WeatherData> getForecast(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("current") String current,
            @Query("daily") String daily,
            @Query("forecast_days") int forecastDays,
            @Query("timezone") String timezone
    );
}
