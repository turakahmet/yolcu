package com.islamyolu.app.prayertimes;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.islamyolu.app.IslamYoluApplication;
import com.islamyolu.app.Service.SharedPrefsHelperService;
import com.islamyolu.app.Utils.AppLogger;
import com.islamyolu.app.main.MainActivity;
import com.islamyolu.app.prayertimes.location.PlaceMatcher;
import com.islamyolu.app.prayertimes.remote.PlacesResponse;
import com.islamyolu.app.prayertimes.sync.PrayerTimesSyncScheduler;
import com.islamyolu.app.prayertimes.sync.PrayerTimesUpdateNotifier;

/**
 * Önceki sürümden gelen kullanıcının kayıtlı ilini ("city_name", ör. "Isparta") yeni şehir yapısına
 * (cityId) tek seferlik taşır.
 * <ul>
 *   <li>Eski il adı Diyanet listesindeki ilin merkez kaydıyla eşlenir; kullanıcıya onaylatılır
 *       (yanlış eşleşme sessizce kabul edilmez, §8).</li>
 *   <li>Geri alınabilir: "city_name" hiç değiştirilmez, yalnızca yeni anahtarlar yazılır
 *       ({@link SharedPrefsHelperService#revertLegacyMigration()}).</li>
 *   <li>Eşleşmezse veya kullanıcı reddederse şehir seçimi istenir; bir daha sorulmaz.</li>
 *   <li>İnternet yoksa bir sonraki açılışta yeniden denenir.</li>
 * </ul>
 */
public final class LegacyCityMigration {

    private static final String TAG = "LegacyCityMigration";
    private static final int TURKEY_COUNTRY_ID = 2;

    static final String STATE_MIGRATED = "migrated";
    static final String STATE_DECLINED = "declined";
    static final String STATE_NO_MATCH = "no_match";
    static final String STATE_NO_LEGACY = "no_legacy";

    /** Kullanıcıya sorulacak öneri: eski il ve vakitleri kullanılacak il merkezi. */
    public static final class Offer {
        @NonNull public final String legacyName;
        @NonNull public final PrayerCitySelection selection;

        Offer(@NonNull String legacyName, @NonNull PrayerCitySelection selection) {
            this.legacyName = legacyName;
            this.selection = selection;
        }
    }

    private LegacyCityMigration() {
    }

    /**
     * Taşıma önerisi; gerek yoksa (yeni seçim var, daha önce denenmiş, eski kayıt yok) veya
     * eşleşmezse null. Ağ ve disk kullanır.
     */
    @WorkerThread
    @Nullable
    public static Offer findOffer(@NonNull Context context) {
        SharedPrefsHelperService prefs = prefs(context);
        if (prefs.getPrayerCitySelection() != null || prefs.getLegacyMigrationState() != null) {
            return null;
        }
        String legacyName = prefs.getCityNameFromPreferences();
        if (legacyName == null || legacyName.trim().isEmpty()) {
            prefs.setLegacyMigrationState(STATE_NO_LEGACY);
            return null;
        }
        RemoteResult<PlacesResponse> turkey = IslamYoluApplication.from(context)
                .getPrayerTimesRepository().fetchPlaces(TURKEY_COUNTRY_ID);
        if (!turkey.isSuccess() || turkey.getData() == null) {
            return null; // internet yok: durum yazılmaz, sonraki açılışta yeniden denenir
        }
        PlaceMatcher.PlaceMatch match = PlaceMatcher.matchLegacyProvince(legacyName, turkey.getData());
        if (match == null || match.city == null || match.state == null) {
            AppLogger.info(TAG, "Eski il Diyanet listesinde bulunamadı; şehir seçimi istenecek");
            prefs.setLegacyMigrationState(STATE_NO_MATCH);
            return null;
        }
        return new Offer(legacyName, new PrayerCitySelection(match.city.getId(), nonNull(match.city.getName()),
                match.state.getId(), match.state.getName(), TURKEY_COUNTRY_ID, turkey.getData().getName()));
    }

    /** Kullanıcı onayladı: vakitleri indirir; başarılıysa seçimi kaydeder ve alarmları/widget'ları yeniler. */
    @WorkerThread
    @NonNull
    public static PrayerSyncResult accept(@NonNull Context context, @NonNull Offer offer) {
        PrayerSyncResult result = IslamYoluApplication.from(context).getPrayerTimesRepository()
                .syncCity(offer.selection.getCityId());
        if (result.isSuccess()) {
            SharedPrefsHelperService prefs = prefs(context);
            prefs.savePrayerCitySelection(offer.selection);
            prefs.setLegacyMigrationState(STATE_MIGRATED);
            PrayerTimesUpdateNotifier.onPrayerTimesUpdated(context);
            PrayerTimesSyncScheduler.onCityChanged(context);
        }
        return result;
    }

    /** Kullanıcı başka şehir seçmek istedi: bir daha sorulmaz. */
    public static void decline(@NonNull Context context) {
        prefs(context).setLegacyMigrationState(STATE_DECLINED);
    }

    private static SharedPrefsHelperService prefs(Context context) {
        return new SharedPrefsHelperService(context.getApplicationContext(), MainActivity.LOCATION_PREFS);
    }

    private static String nonNull(@Nullable String value) {
        return value != null ? value : "";
    }
}
