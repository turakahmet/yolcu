package com.islamyolu.app.weather;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.islamyolu.app.Service.NetworkServiceUtil;
import com.islamyolu.app.Service.WeatherApiService;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.WeatherPreferences;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WeatherService {
    private static final String TAG = "WeatherService";
    private static final String BASE_URL = "https://api.open-meteo.com/v1/";
    private final WeatherApiService weatherApi;
    private final Context context;

    public WeatherService(Context context) {
        this.context = context;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        weatherApi = retrofit.create(WeatherApiService.class);
    }

    /** Uygulama açılışında: veri bundan eskiyse internet varken yenilenir */
    public static final long MAX_AGE_APP_MS = 15 * 60 * 1000L;
    /** Widget güncellemesinde: saatte bir yeterli (widget dakikada bir çizilir, API'yi yormasın) */
    public static final long MAX_AGE_WIDGET_MS = 60 * 60 * 1000L;
    /** Kullanıcı yenile dediğinde */
    public static final long MAX_AGE_FORCE = 0L;

    private static final String CURRENT_FIELDS =
            "temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code,is_day";
    private static final String DAILY_FIELDS = "weather_code,temperature_2m_max";
    /** Bugün + 5 gün (tahmin satırında bugünden sonraki 5 gün gösterilir) */
    private static final int FORECAST_DAYS = 6;

    /**
     * Hava durumunu çevrim dışı öncelikli yükler:
     * <ul>
     *   <li>Bu konumun kayıtlı verisi {@code maxAgeMs}'den yeniyse yalnızca o verilir.</li>
     *   <li>Eskiyse önce kayıtlı veri hemen verilir, internet varsa sunucudan yenilenip
     *       {@code onSuccess} bir kez daha çağrılır. Bu yüzden callback iki kez çağrılabilir.</li>
     *   <li>İnternet yoksa veya istek başarısızsa son veri ekranda kalır; hiç veri yoksa
     *       {@code onError}.</li>
     * </ul>
     * Kayıtlı veri çağıran thread'de, sunucu cevabı ana thread'de verilir.
     */
    public void load(double latitude, double longitude, long maxAgeMs, @NonNull WeatherCallback callback) {
        String locationKey = locationKey(latitude, longitude);
        WeatherData cached = getCached(locationKey);
        long age = System.currentTimeMillis() - getLastUpdateTime();
        if (cached != null) {
            callback.onSuccess(cached);
            if (age < maxAgeMs) {
                return;
            }
        }
        if (!NetworkServiceUtil.isConnected(context)) {
            Log.d(TAG, "İnternet yok; son kayıtlı hava durumu kullanılıyor");
            if (cached == null) {
                callback.onError("İnternet yok, kayıtlı veri yok");
            }
            return;
        }
        weatherApi.getForecast(latitude, longitude, CURRENT_FIELDS, DAILY_FIELDS, FORECAST_DAYS, "auto")
                .enqueue(new Callback<WeatherData>() {
                    @Override
                    public void onResponse(@NonNull Call<WeatherData> call, @NonNull Response<WeatherData> response) {
                        WeatherData data = response.body();
                        if (response.isSuccessful() && data != null && data.getCurrentWeather() != null) {
                            AppPreferences.from(context).weather().save(new Gson().toJson(data),
                                    System.currentTimeMillis(), locationKey);
                            callback.onSuccess(data);
                        } else {
                            Log.e(TAG, "Hava durumu alınamadı: HTTP " + response.code());
                            if (cached == null) {
                                callback.onError("Veri alınamadı");
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<WeatherData> call, @NonNull Throwable t) {
                        Log.e(TAG, "Hava durumu API hatası: " + t.getMessage(), t);
                        if (cached == null) {
                            callback.onError("Bağlantı hatası");
                        }
                    }
                });
    }

    /** Konum anahtarı: ~1 km hassasiyet yeterli (aynı şehirde küçük kaymalar yeni istek açmasın) */
    @NonNull
    private static String locationKey(double latitude, double longitude) {
        return String.format(Locale.ROOT, "%.2f,%.2f", latitude, longitude);
    }

    /** Bu konuma ait, yeni biçimde (hissedilen/nem/rüzgar alanlı istekle) kaydedilmiş veri; yoksa null */
    @Nullable
    private WeatherData getCached(@NonNull String locationKey) {
        WeatherPreferences prefs = AppPreferences.from(context).weather();
        if (!locationKey.equals(prefs.getLocationKey())) {
            return null;
        }
        String json = prefs.getDataJson();
        if (json == null) {
            return null;
        }
        try {
            WeatherData data = new Gson().fromJson(json, WeatherData.class);
            return data != null && data.getCurrentWeather() != null ? data : null;
        } catch (JsonParseException e) {
            Log.w(TAG, "Kayıtlı hava durumu okunamadı; yeniden indirilecek", e);
            return null;
        }
    }

    /** Son başarılı güncelleme zamanı (epoch ms) */
    private long getLastUpdateTime() {
        return AppPreferences.from(context).weather().getLastUpdate();
    }

    public interface WeatherCallback {
        void onSuccess(WeatherData weatherData);

        void onError(String error);
    }
} 