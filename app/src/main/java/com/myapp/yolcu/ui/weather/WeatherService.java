package com.myapp.yolcu.ui.weather;

import android.util.Log;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class WeatherService {
    private static final String TAG = "WeatherService";
    private static final String BASE_URL = "https://api.open-meteo.com/v1/";
    private final WeatherApi weatherApi;

    public interface WeatherApi {
        @GET("forecast")
        Call<WeatherData> getWeather(
            @Query("latitude") double latitude,
            @Query("longitude") double longitude,
            @Query("current_weather") boolean currentWeather,
            @Query("timezone") String timezone
        );
    }

    public WeatherService() {
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

        weatherApi = retrofit.create(WeatherApi.class);
    }

    public void getWeatherData(double latitude, double longitude, WeatherCallback callback) {
        Log.d(TAG, "Hava durumu verisi alınıyor: lat=" + latitude + ", lon=" + longitude);
        
        weatherApi.getWeather(latitude, longitude, true, "auto")
            .enqueue(new Callback<WeatherData>() {
                @Override
                public void onResponse(Call<WeatherData> call, Response<WeatherData> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, "Hava durumu verisi başarıyla alındı");
                        callback.onSuccess(response.body());
                    } else {
                        Log.e(TAG, "Hava durumu verisi alınamadı: " + response.message());
                        callback.onError("Veri alınamadı");
                    }
                }

                @Override
                public void onFailure(Call<WeatherData> call, Throwable t) {
                    Log.e(TAG, "Hava durumu API hatası: " + t.getMessage());
                    callback.onError("Bağlantı hatası");
                }
            });
    }

    public interface WeatherCallback {
        void onSuccess(WeatherData weatherData);
        void onError(String error);
    }
} 