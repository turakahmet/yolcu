package com.islamyolu.app.weather;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.islamyolu.app.Service.WeatherApiService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WeatherService {
    private static final String TAG = "WeatherService";
    private static final String BASE_URL = "https://api.open-meteo.com/v1/";
    private static final long UPDATE_INTERVAL = 3600000; // 1 saat (milisaniye cinsinden)
    private final WeatherApiService weatherApi;
    private final Context context;
//
//    public interface WeatherApi {
//        @GET("forecast")
//        Call<WeatherData> getWeather(
//                @Query("latitude") double latitude,
//                @Query("longitude") double longitude,
//                @Query("current_weather") boolean currentWeather,
//                @Query("timezone") String timezone
//        );
//    }

    public WeatherService(Context context) {
        this.context = context;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        weatherApi = retrofit.create(WeatherApiService.class);
    }

    public void getWeatherData(double latitude, double longitude, WeatherCallback callback) {
        Log.d(TAG, "Hava durumu verisi alınıyor: lat=" + latitude + ", lon=" + longitude);

        long lastUpdate = getLastUpdateTime();
        long currentTime = System.currentTimeMillis();

//        // Son güncelleme zamanını kontrol et
//        if (currentTime - lastUpdate < UPDATE_INTERVAL) {
//            Log.d(TAG, "Son güncellemeden bu yana 1 saat geçmedi");
//            // Mevcut veriyi callback ile dön
//            callback.onSuccess(getCurrentWeatherData());
//            return;
//        }

        // 1 saat geçmişse yeni veri al
        weatherApi.getWeather(latitude, longitude, true, "auto")
                .enqueue(new Callback<WeatherData>() {
                    @Override
                    public void onResponse(Call<WeatherData> call, Response<WeatherData> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // Son güncelleme zamanını kaydet
                            saveLastUpdateTime(currentTime);
                            // Yeni veriyi kaydet
                            saveWeatherData(response.body());
                            Log.d(TAG, "Hava durumu verisi başarıyla alındı" + response.body().getWeatherDescription());
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

    public void getDailyWeatherAndForecastByDate(double latitude, double longitude, WeatherCallback callback) {
        Log.d(TAG, "Hava durumu verisi alınıyor: lat=" + latitude + ", lon=" + longitude);
        // Tarih formatı
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // Bugünün tarihi
        LocalDate startDate = LocalDate.now().plusDays(1);
        String startDateStr = startDate.format(formatter);
        // 5 gün sonrası
        LocalDate endDate = startDate.plusDays(4);
        String endDateStr = endDate.format(formatter);

        long lastUpdate = getLastUpdateTime();
        long currentTime = System.currentTimeMillis();

        // Son güncelleme zamanını kontrol et
        if (currentTime - lastUpdate < UPDATE_INTERVAL) {
            Log.d(TAG, "Son güncellemeden bu yana 1 saat geçmedi");
            // Mevcut veriyi callback ile dön
            callback.onSuccess(getCurrentWeatherData());
            return;
        }
        weatherApi.getDailyWeatherAndForecastByDate(latitude, longitude, true, "weathercode,temperature_2m_max", startDateStr, endDateStr, "auto")
                .enqueue(new Callback<WeatherData>() {
                    @Override
                    public void onResponse(Call<WeatherData> call, Response<WeatherData> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            // Son güncelleme zamanını kaydet
                            saveLastUpdateTime(currentTime);
                            WeatherData data = response.body();

                            saveWeatherData(data);
                            Log.d(TAG, "Hava durumu listesi başarıyla alındı");
                            callback.onSuccess(data);
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

    // Son güncelleme zamanını SharedPreferences'a kaydet
    private void saveLastUpdateTime(long time) {
        SharedPreferences prefs = context.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE);
        prefs.edit().putLong("last_update", time).apply();
    }

    // Son güncelleme zamanını SharedPreferences'dan al
    private long getLastUpdateTime() {
        SharedPreferences prefs = context.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE);
        return prefs.getLong("last_update", 0);
    }

    // Hava durumu verisini SharedPreferences'a kaydet
    private void saveWeatherData(WeatherData weatherData) {
        SharedPreferences prefs = context.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = gson.toJson(weatherData);
        prefs.edit().putString("weather_data", json).apply();
    }

    // Kaydedilmiş hava durumu verisini al
    private WeatherData getCurrentWeatherData() {
        SharedPreferences prefs = context.getSharedPreferences("WeatherPrefs", Context.MODE_PRIVATE);
        String json = prefs.getString("weather_data", null);
        if (json != null) {
            Gson gson = new Gson();
            return gson.fromJson(json, WeatherData.class);
        }
        return null;
    }

    public interface WeatherCallback {
        void onSuccess(WeatherData weatherData);

        void onError(String error);
    }
} 