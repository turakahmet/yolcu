package com.islamyolu.app.prayertimes.remote;

import androidx.annotation.NonNull;

import com.islamyolu.app.Utils.APIUrl;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Namaz vakitleri servisinin Retrofit kurulumu. Örnek uygulama sınıfında bir kez oluşturulur.
 * Gövde loglanmaz (yıllık vakit dosyaları büyüktür ve log'a veri dökülmemeli).
 */
public final class PrayerTimesApiClient {

    private static final long CONNECT_TIMEOUT_SECONDS = 15;
    private static final long READ_TIMEOUT_SECONDS = 30;

    private PrayerTimesApiClient() {
    }

    @NonNull
    public static PrayerTimesApi create() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(APIUrl.PRAYER_TIMES_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
                .create(PrayerTimesApi.class);
    }
}
