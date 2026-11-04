package com.islamyolu.app.prayertimes.ui;

import android.app.Application;
import android.location.Address;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.islamyolu.app.IslamYoluApplication;
import com.islamyolu.app.R;
import com.islamyolu.app.Service.LocationService;
import com.islamyolu.app.Service.NetworkServiceUtil;
import com.islamyolu.app.Service.SharedPrefsHelperService;
import com.islamyolu.app.main.MainActivity;
import com.islamyolu.app.prayertimes.PlaceNameNormalizer;
import com.islamyolu.app.prayertimes.PrayerCitySelection;
import com.islamyolu.app.prayertimes.PrayerSyncResult;
import com.islamyolu.app.prayertimes.PrayerTimesRepository;
import com.islamyolu.app.prayertimes.RemoteResult;
import com.islamyolu.app.Service.AddressResolver;
import com.islamyolu.app.prayertimes.location.AddressParts;
import com.islamyolu.app.prayertimes.location.PlaceMatcher;
import com.islamyolu.app.prayertimes.remote.CountriesResponse;
import com.islamyolu.app.prayertimes.remote.PlacesResponse;
import com.islamyolu.app.prayertimes.sync.PrayerTimesSyncScheduler;
import com.islamyolu.app.prayertimes.sync.PrayerTimesUpdateNotifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ülke → Eyalet/İl → Şehir/İlçe seçimi. Durum burada tutulur; ekran döndürme, bölünmüş ekran
 * ve katlanma gibi yapılandırma değişikliklerinde seçim kaybolmaz.
 * Ağ işleri tek bir arka plan thread'inde, repository üzerinden yapılır.
 */
public class CitySelectionViewModel extends AndroidViewModel {

    public enum Level {COUNTRY, STATE, CITY}

    /** Kullanıcıya gösterilecek mesaj (metin kaynağı + isteğe bağlı şehir adı). */
    public static final class Message {
        @StringRes final int textRes;
        @Nullable final String argument;
        final boolean retryable;

        Message(@StringRes int textRes, @Nullable String argument, boolean retryable) {
            this.textRes = textRes;
            this.argument = argument;
            this.retryable = retryable;
        }
    }

    /** Başarılı indirme sonrası bir kez tüketilen olay. */
    public static final class Completion {
        @NonNull final String cityName;
        private boolean handled;

        Completion(@NonNull String cityName) {
            this.cityName = cityName;
        }

        boolean consume() {
            if (handled) return false;
            handled = true;
            return true;
        }
    }

    private static final int TURKEY_COUNTRY_ID = 2;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private final PrayerTimesRepository repository;
    private final SharedPrefsHelperService preferences;
    private final NetworkServiceUtil network;
    private final AddressResolver addressResolver;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Level> level = new MutableLiveData<>(Level.COUNTRY);
    private final MutableLiveData<List<PlaceItem>> items = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Message> message = new MutableLiveData<>(null);
    private final MutableLiveData<String[]> selection = new MutableLiveData<>(new String[2]);
    private final MutableLiveData<Completion> completion = new MutableLiveData<>(null);
    private final MutableLiveData<LocationSuggestion> locationSuggestion = new MutableLiveData<>(null);
    /** Arka plan thread'i de okuyup yazar (konum akışı ülke listesini kendisi indirebilir) */
    private volatile List<CountriesResponse.Country> countries = Collections.emptyList();
    // Aşağıdakiler yalnızca ana thread'de okunur/yazılır
    @Nullable private Message messageAfterLoad;
    @Nullable private PlacesResponse places;
    @Nullable private CountriesResponse.Country country;
    @Nullable private PlacesResponse.State state;
    /** Konum akışı arka plan thread'inden de okur */
    private final Set<Integer> unavailableCityIds = ConcurrentHashMap.newKeySet();
    @Nullable private Runnable retryAction;
    private boolean started;

    public CitySelectionViewModel(@NonNull Application application) {
        super(application);
        repository = IslamYoluApplication.from(application).getPrayerTimesRepository();
        preferences = new SharedPrefsHelperService(application, MainActivity.LOCATION_PREFS);
        network = NetworkServiceUtil.getInstance(application);
        addressResolver = new AddressResolver(application);
    }

    public LiveData<Level> getLevel() {
        return level;
    }

    public LiveData<List<PlaceItem>> getItems() {
        return items;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<Message> getMessage() {
        return message;
    }

    /** Adım göstergesi için seçilenler: [ülke, il/eyalet]; seçilmeyen null. */
    public LiveData<String[]> getSelection() {
        return selection;
    }

    /** Adım göstergesinde tamamlanmış bir adıma dokunulunca o adıma döner. */
    public void goToLevel(@NonNull Level target) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        if (target == Level.COUNTRY) {
            showCountries();
        } else if (target == Level.STATE && places != null && places.getStates().size() > 1) {
            showStates();
        }
    }

    public LiveData<Completion> getCompletion() {
        return completion;
    }

    /** Konumdan tek ve kesin şehir bulunduğunda kullanıcıya onaylatılacak öneri; yoksa null. */
    public LiveData<LocationSuggestion> getLocationSuggestion() {
        return locationSuggestion;
    }

    /** Ekran ilk açıldığında bir kez ülke listesini yükler. */
    public void start() {
        if (!started) {
            started = true;
            loadCountries();
        }
    }

    public void retry() {
        if (retryAction != null) {
            retryAction.run();
        }
    }

    public void onItemClicked(@NonNull PlaceItem item) {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        Level current = level.getValue();
        if (current == Level.COUNTRY) {
            selectCountry(item.id);
        } else if (current == Level.STATE) {
            selectState(item.id);
        } else if (current == Level.CITY) {
            selectCity(item);
        }
    }

    /** Bir üst seviyeye döner; ülke listesindeyse false (ekran kapanabilir). */
    public boolean goBack() {
        if (Boolean.TRUE.equals(loading.getValue())) {
            return true;
        }
        Level current = level.getValue();
        if (current == Level.CITY && places != null && places.getStates().size() > 1) {
            showStates();
            return true;
        }
        if (current == Level.CITY || current == Level.STATE) {
            showCountries();
            return true;
        }
        return false;
    }

    // --- Konumdan şehir bulma (§8) ---

    /**
     * "Konumunuz bu mu?" penceresi için bulunan konum. Kullanıcı onaylamadan hiçbir şey seçilmez;
     * ekran döndürülse de cevaplanana kadar korunur.
     */
    public static final class LocationSuggestion {
        /** Türkiye'de satırlar "Şehir / İlçe", diğer ülkelerde "Eyalet / Şehir" */
        final boolean turkey;
        @NonNull final String countryName;
        @Nullable final String stateName;
        /** Konumdaki ilçe/şehir (Diyanet listesinde olmayabilir, ör. Gaziosmanpaşa) */
        @Nullable final String districtName;
        /**
         * Konumdaki ilçe listede yoksa vakitleri kullanılacak il merkezi (ör. İSTANBUL); pencerede
         * kullanıcıya açıkça gösterilir. Kesin eşleşmede null.
         */
        @Nullable final String centerName;
        // Onaylanırsa uygulanacak eşleşme
        @NonNull private final CountriesResponse.Country country;
        @NonNull private final PlacesResponse places;
        @NonNull private final PlaceMatcher.PlaceMatch match;
        /** Onayda vakitleri indirilecek şehir; bulunamadıysa null (liste en yakın adımdan açılır) */
        @Nullable private final PlaceItem target;

        LocationSuggestion(boolean turkey, @NonNull String countryName, @Nullable String stateName,
                           @Nullable String districtName, @Nullable String centerName,
                           @NonNull CountriesResponse.Country country, @NonNull PlacesResponse places,
                           @NonNull PlaceMatcher.PlaceMatch match, @Nullable PlaceItem target) {
            this.turkey = turkey;
            this.countryName = countryName;
            this.stateName = stateName;
            this.districtName = districtName;
            this.centerName = centerName;
            this.country = country;
            this.places = places;
            this.match = match;
            this.target = target;
        }
    }

    /** Konum izni istenirken / konum alınırken bekleme gösterilir. */
    public void onLocatingStarted() {
        started = true;
        locationSuggestion.setValue(null);
        message.setValue(new Message(R.string.city_selection_locating, null, false));
        loading.setValue(true);
    }

    /** İzin reddedildi, servis kapalı kaldı veya konum belirlenemedi: kullanıcı listeden seçer. */
    public void onLocationUnavailable(@NonNull LocationService.Unavailable reason) {
        int text;
        switch (reason) {
            case PERMISSION_DENIED:
                text = R.string.city_selection_location_denied;
                break;
            case SERVICES_OFF:
                text = R.string.city_selection_location_off_list;
                break;
            default:
                text = R.string.city_selection_location_unavailable;
                break;
        }
        Message info = new Message(text, null, false);
        loading.setValue(false);
        if (countries.isEmpty()) {
            messageAfterLoad = info;
            loadCountries();
        } else {
            showCountries();
            message.setValue(info);
        }
    }

    public void onLocationReceived(double latitude, double longitude) {
        if (!network.isConnected()) {
            loading.setValue(false);
            loadCountries(); // internet uyarısını ve tekrar dene seçeneğini gösterir
            return;
        }
        executor.execute(() -> {
            List<CountriesResponse.Country> list = countries;
            if (list.isEmpty()) {
                RemoteResult<List<CountriesResponse.Country>> result = repository.fetchCountries();
                if (!result.isSuccess() || result.getData() == null) {
                    runOnMain(() -> {
                        loading.setValue(false);
                        showRemoteError(result.getStatus(), this::loadCountries);
                    });
                    return;
                }
                list = withTurkeyFirst(result.getData());
                countries = list;
            }

            // §8: eşleştirme için Geocoder her zaman İngilizce
            Address geocoded = addressResolver.resolve(latitude, longitude, Locale.ENGLISH);
            AddressParts address = geocoded != null ? AddressParts.from(geocoded) : null;
            CountriesResponse.Country matched = address != null ? PlaceMatcher.matchCountry(address, list) : null;
            if (matched == null) {
                runOnMain(() -> showLocationFallback(new Message(R.string.city_selection_location_no_match, null, false)));
                return;
            }
            RemoteResult<PlacesResponse> placesResult = repository.fetchPlaces(matched.getId());
            if (!placesResult.isSuccess() || placesResult.getData() == null) {
                runOnMain(() -> showLocationFallback(new Message(R.string.city_selection_load_error, null, false)));
                return;
            }
            PlaceMatcher.PlaceMatch match = PlaceMatcher.matchPlace(address, matched.getId(), placesResult.getData());
            LocationSuggestion suggestion = buildSuggestion(address, matched, placesResult.getData(), match);
            runOnMain(() -> {
                // Bulunan konum kullanıcıya sorulur; onaylamadan hiçbir şey seçilmez
                loading.setValue(false);
                message.setValue(null);
                locationSuggestion.setValue(suggestion);
            });
        });
    }

    /** "Konumu onayla": seçim yapılmış gibi vakitler indirilir; hedef şehir yoksa liste bulunan yerden açılır. */
    public void acceptLocationSuggestion() {
        LocationSuggestion suggestion = locationSuggestion.getValue();
        locationSuggestion.setValue(null);
        if (suggestion == null) {
            return;
        }
        country = suggestion.country;
        places = suggestion.places;
        if (suggestion.target != null && suggestion.match.state != null) {
            state = suggestion.match.state;
            showCities();
            selectCity(suggestion.target);
        } else {
            applyLocationMatch(suggestion.places, suggestion.match);
        }
    }

    /**
     * Pencere bilgisi ve onayda kullanılacak şehir. Konumdaki ilçe Diyanet listesinde yoksa
     * (ör. İstanbul'un merkez ilçeleri) ilin kendi adıyla geçen merkez kaydı hedeflenir.
     */
    private LocationSuggestion buildSuggestion(AddressParts address, CountriesResponse.Country matched,
                                               PlacesResponse matchedPlaces, PlaceMatcher.PlaceMatch match) {
        boolean turkey = matched.getId() == TURKEY_COUNTRY_ID;
        PlacesResponse.State matchedState = match.state;
        String stateName = matchedState != null ? nameOf(matchedState.getName()) : address.getAdminArea();
        String district = match.city != null
                ? nameOf(match.city.getName())
                : (turkey && address.getSubAdminArea() != null ? address.getSubAdminArea() : address.getLocality());
        if (district != null && stateName != null
                && PlaceNameNormalizer.normalize(district).equals(PlaceNameNormalizer.normalize(stateName))) {
            district = null; // "İstanbul, İstanbul" tekrarını gösterme
        }

        PlacesResponse.City targetCity = match.city;
        String centerName = null;
        if (targetCity == null && matchedState != null) {
            targetCity = centerCityOf(matchedState);
            if (targetCity != null) {
                centerName = nameOf(targetCity.getName());
            }
        }
        PlaceItem target = targetCity != null
                ? new PlaceItem(targetCity.getId(), nameOf(targetCity.getName()), unavailableCityIds.contains(targetCity.getId()))
                : null;
        return new LocationSuggestion(turkey, countryDisplayName(matched), stateName, district, centerName,
                matched, matchedPlaces, match, target);
    }

    /** İlin kendi adıyla geçen merkez kaydı (ör. İSTANBUL ilindeki İSTANBUL); tek şehirli ilde o şehir. */
    @Nullable
    private static PlacesResponse.City centerCityOf(PlacesResponse.State state) {
        List<PlacesResponse.City> cities = state.getCities();
        if (cities.size() == 1) {
            return cities.get(0);
        }
        for (PlacesResponse.City city : cities) {
            if (city.getKey() != null && city.getKey().equals(state.getKey())) {
                return city;
            }
        }
        return null;
    }

    /** "Konumunuz bu mu?" → Hayır: adım adım seçim 1. adımdan başlar. */
    public void rejectLocationSuggestion() {
        locationSuggestion.setValue(null);
        showCountries();
        message.setValue(new Message(R.string.city_selection_location_rejected, null, false));
    }

    /** Kesin ilçe bulunamadıysa liste, bulunan en yakın adımdan açılır. */
    private void applyLocationMatch(PlacesResponse matchedPlaces, PlaceMatcher.PlaceMatch match) {
        if (match.level == PlaceMatcher.Level.STATE && match.state != null) {
            // "Merkez" ilçe, birden fazla aday veya listede olmayan ilçe: ilçeyi kullanıcı seçer
            state = match.state;
            showCities();
            message.setValue(new Message(R.string.city_selection_location_pick_city, nameOf(state.getName()), false));
        } else if (matchedPlaces.getStates().size() == 1) {
            state = matchedPlaces.getStates().get(0);
            showCities();
            message.setValue(new Message(R.string.city_selection_location_pick_city, nameOf(state.getName()), false));
        } else {
            showStates();
            message.setValue(new Message(R.string.city_selection_location_pick_state, countryLabel(), false));
        }
    }

    private void showLocationFallback(Message info) {
        loading.setValue(false);
        showCountries();
        message.setValue(info);
    }

    // --- Ülkeler ---

    private void loadCountries() {
        if (!startRemoteWork(this::loadCountries)) {
            messageAfterLoad = null;
            return;
        }
        executor.execute(() -> {
            RemoteResult<List<CountriesResponse.Country>> result = repository.fetchCountries();
            if (result.isSuccess() && result.getData() != null) {
                countries = withTurkeyFirst(result.getData());
            }
            runOnMain(() -> {
                loading.setValue(false);
                if (result.isSuccess() && result.getData() != null) {
                    showCountries();
                    if (messageAfterLoad != null) {
                        message.setValue(messageAfterLoad);
                        messageAfterLoad = null;
                    }
                } else {
                    messageAfterLoad = null;
                    showRemoteError(result.getStatus(), this::loadCountries);
                }
            });
        });
    }

    private void showCountries() {
        country = null;
        places = null;
        state = null;
        List<PlaceItem> list = new ArrayList<>(countries.size());
        for (CountriesResponse.Country c : countries) {
            list.add(new PlaceItem(c.getId(), countryDisplayName(c), false));
        }
        show(Level.COUNTRY, list);
    }

    private void selectCountry(int countryId) {
        for (CountriesResponse.Country c : countries) {
            if (c.getId() == countryId) {
                country = c;
                loadPlaces(c.getId());
                return;
            }
        }
    }

    // --- Eyalet / İl ---

    private void loadPlaces(int countryId) {
        Runnable retry = () -> loadPlaces(countryId);
        if (!startRemoteWork(retry)) return;
        executor.execute(() -> {
            RemoteResult<PlacesResponse> result = repository.fetchPlaces(countryId);
            runOnMain(() -> {
                loading.setValue(false);
                if (result.isSuccess() && result.getData() != null) {
                    places = result.getData();
                    if (places.getStates().size() == 1) {
                        // Tek eyaletli ülkede (ör. Andorra) eyalet adımı atlanır
                        state = places.getStates().get(0);
                        showCities();
                    } else {
                        showStates();
                    }
                } else {
                    showRemoteError(result.getStatus(), retry);
                }
            });
        });
    }

    private void showStates() {
        state = null;
        List<PlaceItem> list = new ArrayList<>();
        if (places != null) {
            for (PlacesResponse.State s : places.getStates()) {
                list.add(new PlaceItem(s.getId(), nameOf(s.getName()), false));
            }
        }
        show(Level.STATE, list);
    }

    private void selectState(int stateId) {
        if (places == null) return;
        for (PlacesResponse.State s : places.getStates()) {
            if (s.getId() == stateId) {
                state = s;
                if (s.getCities().size() == 1) {
                    // İlçesi olmayan il/eyalet (ör. BERLIN): seçim 2. adımda biter
                    PlacesResponse.City only = s.getCities().get(0);
                    selection.setValue(new String[]{countryLabel(), nameOf(s.getName())});
                    selectCity(new PlaceItem(only.getId(), nameOf(only.getName()),
                            unavailableCityIds.contains(only.getId())));
                } else {
                    showCities();
                }
                return;
            }
        }
    }

    // --- Şehir / İlçe ---

    private void showCities() {
        List<PlaceItem> list = new ArrayList<>();
        if (state != null) {
            for (PlacesResponse.City c : state.getCities()) {
                list.add(new PlaceItem(c.getId(), nameOf(c.getName()), unavailableCityIds.contains(c.getId())));
            }
        }
        show(Level.CITY, list);
    }

    private void selectCity(@NonNull PlaceItem item) {
        if (item.unavailable) {
            message.setValue(new Message(R.string.city_selection_no_data_for_city, item.name, false));
            return;
        }
        Runnable retry = () -> selectCity(item);
        if (!startRemoteWork(retry)) return;
        message.setValue(new Message(R.string.city_selection_downloading, item.name, false));

        PrayerCitySelection selection = new PrayerCitySelection(item.id, item.name,
                state != null ? state.getId() : -1, state != null ? nameOf(state.getName()) : null,
                country != null ? country.getId() : -1, country != null ? nameOf(country.getName()) : null);

        executor.execute(() -> {
            PrayerSyncResult result = repository.syncCity(item.id);
            if (result.isSuccess()) {
                // Seçim yalnızca vakitler Room'a yazıldıktan sonra kaydedilir
                preferences.savePrayerCitySelection(selection);
                // Yeni şehrin vakitleriyle alarmlar/widget'lar yenilenir; arka plan senkronu yeni şehre geçer
                PrayerTimesUpdateNotifier.onPrayerTimesUpdated(getApplication());
                PrayerTimesSyncScheduler.onCityChanged(getApplication());
            }
            runOnMain(() -> onCitySynced(item, result, retry));
        });
    }

    private void onCitySynced(PlaceItem item, PrayerSyncResult result, Runnable retry) {
        loading.setValue(false);
        switch (result.getStatus()) {
            case UPDATED:
            case NOT_MODIFIED:
                message.setValue(null);
                completion.setValue(new Completion(item.name));
                break;
            case CITY_HAS_NO_DATA:
                unavailableCityIds.add(item.id);
                if (level.getValue() == Level.CITY) {
                    showCities(); // listede "vakit yayımlanmıyor" olarak işaretlenir
                }
                message.setValue(new Message(R.string.city_selection_no_data_for_city, item.name, false));
                break;
            case NETWORK_ERROR:
                retryAction = retry;
                message.setValue(new Message(R.string.city_selection_no_internet, null, true));
                break;
            default:
                retryAction = result.isRetryable() ? retry : null;
                message.setValue(new Message(R.string.city_selection_download_error, null, result.isRetryable()));
                break;
        }
    }

    // --- Yardımcılar ---

    /** İnternet yoksa §6 mesajını gösterir ve false döner; varsa yüklemeyi başlatır. */
    private boolean startRemoteWork(@NonNull Runnable retry) {
        retryAction = retry;
        if (!network.isConnected()) {
            message.setValue(new Message(R.string.city_selection_no_internet, null, true));
            return false;
        }
        message.setValue(null);
        loading.setValue(true);
        return true;
    }

    private void showRemoteError(RemoteResult.Status status, Runnable retry) {
        retryAction = retry;
        int text = status == RemoteResult.Status.NETWORK_ERROR
                ? R.string.city_selection_no_internet
                : R.string.city_selection_load_error;
        message.setValue(new Message(text, null, true));
    }

    private void show(Level newLevel, List<PlaceItem> list) {
        message.setValue(null);
        level.setValue(newLevel);
        items.setValue(list);
        selection.setValue(new String[]{
                newLevel != Level.COUNTRY ? countryLabel() : null,
                newLevel == Level.CITY && state != null ? nameOf(state.getName()) : null});
    }

    @Nullable
    private String countryLabel() {
        return country != null ? countryDisplayName(country) : null;
    }

    /** Uygulama Türkçe değilse ülkenin İngilizce adı gösterilir. */
    private String countryDisplayName(CountriesResponse.Country c) {
        String language = getApplication().getResources().getConfiguration().getLocales().get(0).getLanguage();
        String name = "tr".equals(language) ? c.getName() : c.getCode();
        return nameOf(name != null ? name : c.getName());
    }

    private static String nameOf(@Nullable String name) {
        return name != null ? name : "";
    }

    private static List<CountriesResponse.Country> withTurkeyFirst(List<CountriesResponse.Country> source) {
        List<CountriesResponse.Country> sorted = new ArrayList<>(source.size());
        for (CountriesResponse.Country c : source) {
            if (c.getId() == TURKEY_COUNTRY_ID) sorted.add(0, c);
            else sorted.add(c);
        }
        return sorted;
    }

    private static void runOnMain(Runnable action) {
        MAIN_HANDLER.post(action);
    }

    @Override
    protected void onCleared() {
        executor.shutdownNow();
    }
}
