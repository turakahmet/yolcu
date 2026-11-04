package com.islamyolu.app.prayertimes;

import android.database.SQLException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import com.islamyolu.app.Utils.AppLogger;
import com.islamyolu.app.prayertimes.db.PrayerCityEntity;
import com.islamyolu.app.prayertimes.db.PrayerDayEntity;
import com.islamyolu.app.prayertimes.db.PrayerTimesDao;
import com.islamyolu.app.prayertimes.remote.CountriesResponse;
import com.islamyolu.app.prayertimes.remote.PlacesResponse;
import com.islamyolu.app.prayertimes.remote.PrayerTimesApi;
import com.islamyolu.app.prayertimes.sync.PrayerSyncPolicy;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Namaz vakitlerinin tek veri kapısı: indir → doğrula → tek transaction'da Room'a yaz.
 * UI, widget ve bildirimler Retrofit'e veya DAO'ya doğrudan bağlanmaz; bu sınıfı kullanır.
 * Herhangi bir hatada mevcut Room verisine dokunulmaz ve hata {@link AppLogger} ile kaydedilir.
 * Metotlar ağ ve disk işi yaptığı için ana thread'de çağrılmaz.
 */
public class PrayerTimesRepository {

    private static final String TAG = "PrayerTimesRepository";
    private static final int HTTP_NOT_MODIFIED = 304;
    private static final int HTTP_NOT_FOUND = 404;

    private final PrayerTimesApi api;
    private final PrayerTimesDao dao;
    private final PrayerTimesParser parser;
    private final PrayerTimesValidator validator;
    private final Clock clock;

    public PrayerTimesRepository(@NonNull PrayerTimesApi api, @NonNull PrayerTimesDao dao,
                                 @NonNull PrayerTimesParser parser, @NonNull PrayerTimesValidator validator,
                                 @NonNull Clock clock) {
        this.api = api;
        this.dao = dao;
        this.parser = parser;
        this.validator = validator;
        this.clock = clock;
    }

    /**
     * Şehrin yıllık vakitlerini indirir ve Room'a yazar. İdempotenttir.
     * Room'da geçerli veri varsa ETag ile koşullu istek yapılır; değişmemişse (304) yalnızca
     * senkron zamanı güncellenir. Yazma başarılıysa diğer şehirlerin verisi silinir.
     */
    @WorkerThread
    @NonNull
    public PrayerSyncResult syncCity(int cityId) {
        String etag = currentEtag(cityId);

        Response<ResponseBody> response;
        try {
            response = api.getTimes(cityId, etag).execute();
        } catch (IOException e) {
            AppLogger.error(TAG, "Vakit dosyası indirilemedi, cityId=" + cityId, e);
            return PrayerSyncResult.of(PrayerSyncResult.Status.NETWORK_ERROR, cityId, PrayerSyncResult.NO_HTTP_CODE);
        }

        int code = response.code();
        if (code == HTTP_NOT_MODIFIED) {
            return markNotModified(cityId);
        }
        if (code == HTTP_NOT_FOUND) {
            AppLogger.info(TAG, "Şehir için vakit dosyası yok (404), cityId=" + cityId);
            return PrayerSyncResult.of(PrayerSyncResult.Status.CITY_HAS_NO_DATA, cityId, code);
        }
        if (!response.isSuccessful() || response.body() == null) {
            closeQuietly(response.errorBody());
            AppLogger.warn(TAG, "Beklenmeyen HTTP yanıtı " + code + ", cityId=" + cityId);
            return PrayerSyncResult.of(PrayerSyncResult.Status.SERVER_ERROR, cityId, code);
        }

        String json;
        try (ResponseBody body = response.body()) {
            json = body.string();
        } catch (IOException e) {
            AppLogger.error(TAG, "Vakit dosyası okunamadı, cityId=" + cityId, e);
            return PrayerSyncResult.of(PrayerSyncResult.Status.NETWORK_ERROR, cityId, code);
        }

        return store(cityId, json, response.headers().get("ETag"));
    }

    // --- Okuma (§5): ekranlar LiveData, widget/bildirim/alarm arka planda ---

    /**
     * "Bugün" ve sıradaki vakit için gereken gün aralığı: UTC tarihinin bir gün öncesi ile iki gün sonrası.
     * Şehrin yerel tarihi UTC'den en fazla bir gün farklı olduğundan bugün, dün ve yarın her zaman içindedir.
     */
    @NonNull
    public static LocalDate[] timelineWindow(@NonNull Instant now) {
        LocalDate utc = now.atOffset(ZoneOffset.UTC).toLocalDate();
        return new LocalDate[]{utc.minusDays(1), utc.plusDays(2)};
    }

    /** Aralıktaki günler; Room güncellenince (senkron, şehir değişimi) kendiliğinden yeni değer yayar. */
    @NonNull
    public LiveData<List<PrayerDayEntity>> observeDays(int cityId, @NonNull LocalDate from, @NonNull LocalDate to) {
        return dao.observeDays(cityId, from.toString(), to.toString());
    }

    /** Widget, kalıcı bildirim ve alarm kurulumu için şu anki vakit akışı. */
    @WorkerThread
    @NonNull
    public PrayerTimeline loadTimeline(int cityId, @NonNull Instant now) {
        LocalDate[] window = timelineWindow(now);
        return PrayerTimeline.of(dao.getDays(cityId, window[0].toString(), window[1].toString()));
    }

    /** Arka plan senkronizasyonu için: bu şehrin indirilmesi gerekiyor mu (§4)? */
    @WorkerThread
    @NonNull
    public PrayerSyncPolicy.Reason syncNeed(int cityId, @NonNull LocalDate today) {
        return PrayerSyncPolicy.evaluate(dao.getCity(cityId), dao.getLastDate(cityId), today);
    }

    /** Ülke listesi (countries.json). Seçim ekranı içindir; Room'a yazılmaz. */
    @WorkerThread
    @NonNull
    public RemoteResult<List<CountriesResponse.Country>> fetchCountries() {
        RemoteResult<CountriesResponse> result = execute(api.getCountries(), "countries.json");
        if (!result.isSuccess() || result.getData() == null) {
            return RemoteResult.failure(result.getStatus());
        }
        return RemoteResult.success(result.getData().getCountries());
    }

    /** Ülkenin eyalet/il ve şehir/ilçe listesi (places/{countryId}.json). */
    @WorkerThread
    @NonNull
    public RemoteResult<PlacesResponse> fetchPlaces(int countryId) {
        return execute(api.getPlaces(countryId), "places/" + countryId + ".json");
    }

    private <T> RemoteResult<T> execute(Call<T> call, String fileName) {
        try {
            Response<T> response = call.execute();
            T body = response.body();
            if (!response.isSuccessful() || body == null) {
                closeQuietly(response.errorBody());
                AppLogger.warn(TAG, fileName + " alınamadı, HTTP " + response.code());
                return RemoteResult.failure(RemoteResult.Status.SERVER_ERROR);
            }
            return RemoteResult.success(body);
        } catch (IOException e) {
            AppLogger.error(TAG, fileName + " indirilemedi", e);
            return RemoteResult.failure(RemoteResult.Status.NETWORK_ERROR);
        } catch (RuntimeException e) {
            // Gson biçim hatası (JsonParseException) vb.
            AppLogger.error(TAG, fileName + " ayrıştırılamadı", e);
            return RemoteResult.failure(RemoteResult.Status.SERVER_ERROR);
        }
    }

    private PrayerSyncResult store(int cityId, String json, @Nullable String etag) {
        CityTimesDto file;
        try {
            file = parser.parse(json);
        } catch (PrayerTimesFormatException e) {
            AppLogger.error(TAG, "Vakit dosyası biçimi hatalı, cityId=" + cityId, e);
            return PrayerSyncResult.of(PrayerSyncResult.Status.INVALID_DATA, cityId, PrayerSyncResult.NO_HTTP_CODE);
        }

        Instant now = clock.instant();
        LocalDate cityToday = cityToday(file, now);
        PrayerTimesValidator.Result validation = validator.validate(file, cityId, cityToday);
        if (!validation.isValid()) {
            AppLogger.warn(TAG, "Vakit dosyası doğrulanamadı, cityId=" + cityId + " " + validation.getErrors());
            return PrayerSyncResult.invalid(cityId, validation.getErrors());
        }

        try {
            dao.replaceCityData(toCityEntity(file, now, etag), toDayEntities(file), cityToday);
        } catch (SQLException | IllegalStateException e) {
            // Transaction geri alındı; önceki veri olduğu gibi duruyor
            AppLogger.error(TAG, "Vakitler Room'a yazılamadı, cityId=" + cityId, e);
            return PrayerSyncResult.of(PrayerSyncResult.Status.STORAGE_ERROR, cityId, PrayerSyncResult.NO_HTTP_CODE);
        }
        return PrayerSyncResult.of(PrayerSyncResult.Status.UPDATED, cityId, 200);
    }

    private PrayerSyncResult markNotModified(int cityId) {
        try {
            if (dao.updateSyncedAt(cityId, clock.millis()) == 0) {
                AppLogger.warn(TAG, "304 alındı ama şehir kaydı yok, cityId=" + cityId);
            }
        } catch (SQLException e) {
            AppLogger.error(TAG, "Senkron zamanı güncellenemedi, cityId=" + cityId, e);
            return PrayerSyncResult.of(PrayerSyncResult.Status.STORAGE_ERROR, cityId, HTTP_NOT_MODIFIED);
        }
        return PrayerSyncResult.of(PrayerSyncResult.Status.NOT_MODIFIED, cityId, HTTP_NOT_MODIFIED);
    }

    /** Yalnızca Room'da bu şehrin aynı şema sürümüyle yazılmış günleri varsa ETag gönderilir. */
    @Nullable
    private String currentEtag(int cityId) {
        PrayerCityEntity city = dao.getCity(cityId);
        if (city == null || city.getSchemaVersion() != PrayerTimesValidator.SUPPORTED_VERSION
                || dao.getLastDate(cityId) == null) {
            return null;
        }
        return city.getEtag();
    }

    /** Dosyadaki günlük gmt farklarıyla şehrin yerel bugünü; dosya bugünü kapsamıyorsa UTC tarihi. */
    private static LocalDate cityToday(CityTimesDto file, Instant now) {
        Map<LocalDate, Integer> offsets = new HashMap<>();
        for (PrayerDayDto day : file.getDays()) {
            try {
                offsets.put(LocalDate.parse(day.getDate()), day.getGmtOffsetMinutes());
            } catch (DateTimeParseException e) {
                // Hatalı tarih doğrulayıcıda INVALID_DATE olarak raporlanır
            }
        }
        LocalDate today = PrayerClock.cityToday(now, offsets::get);
        return today != null ? today : now.atOffset(ZoneOffset.UTC).toLocalDate();
    }

    private static PrayerCityEntity toCityEntity(CityTimesDto file, Instant now, @Nullable String etag) {
        return new PrayerCityEntity(file.getCityId(), file.getName(), file.getState(), file.getStateId(),
                file.getCountry(), file.getCountryId(), file.getVersion(), file.getUpdatedAt(),
                file.getValidFrom(), file.getValidTo(), now.toEpochMilli(), etag);
    }

    private static List<PrayerDayEntity> toDayEntities(CityTimesDto file) {
        List<PrayerDayEntity> days = new ArrayList<>(file.getDays().size());
        for (PrayerDayDto day : file.getDays()) {
            days.add(new PrayerDayEntity(file.getCityId(), day.getDate(), day.getFajr(), day.getSunrise(),
                    day.getDhuhr(), day.getAsr(), day.getMaghrib(), day.getIsha(), day.getHijri(),
                    day.getQibla(), day.getGmtOffsetMinutes()));
        }
        return days;
    }

    private static void closeQuietly(@Nullable ResponseBody body) {
        if (body != null) {
            body.close();
        }
    }
}
