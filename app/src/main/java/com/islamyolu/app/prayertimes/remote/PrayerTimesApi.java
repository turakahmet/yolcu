package com.islamyolu.app.prayertimes.remote;

import androidx.annotation.Nullable;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

/**
 * GitHub Pages v2 statik namaz vakti verisi. Taban adres: APIUrl.PRAYER_TIMES_BASE_URL.
 * Yollar göreli yazılır ("/" ile başlamaz) ki taban adresteki /yolcu/v2/ korunsun.
 * Uygulama Diyanet'e doğrudan bağlanmaz; yalnızca bu herkese açık dosyaları okur.
 */
public interface PrayerTimesApi {

    @GET("countries.json")
    Call<CountriesResponse> getCountries();

    @GET("places/{countryId}.json")
    Call<PlacesResponse> getPlaces(@Path("countryId") int countryId);

    /**
     * Ham gövde döner; sütunlar "fields" adlarıyla eşlendiği için PrayerTimesParser ile ayrıştırılır.
     *
     * @param etag önceki indirmenin ETag değeri; aynıysa sunucu 304 döner. null ise başlık gönderilmez.
     */
    @GET("times/{cityId}.json")
    Call<ResponseBody> getTimes(@Path("cityId") int cityId,
                                @Nullable @Header("If-None-Match") String etag);
}
