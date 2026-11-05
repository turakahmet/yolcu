package com.islamyolu.app.settings;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.islamyolu.app.R;
import com.islamyolu.app.Service.AddressResolver;
import com.islamyolu.app.Service.LocationAccess;
import com.islamyolu.app.Service.LocationService;
import com.islamyolu.app.VakitApi.City;
import com.islamyolu.app.prayertimes.PrayerCitySelection;
import com.islamyolu.app.prayertimes.ui.CitySelectionActivity;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.LocationPreferences;

import java.util.Locale;

/**
 * Diyanet vakit şehri ve hava durumu şehri.
 * Namaz vakitleri yalnızca Diyanet şehir seçiminden gelir; "Konumdan güncelle" yalnızca hava durumu
 * için kullanılan il adını yeniler (eski ekrandaki "Güncelle" ile aynı davranış).
 */
public class LocationSettingsFragment extends Fragment {

    /** İzin, konum açık/kapalı ve konum alma merkezi LocationAccess'te */
    private final LocationAccess locationAccess = LocationAccess.register(this, new LocationAccess.Options(),
            new LocationAccess.Callback() {
                @Override
                public void onLocation(@NonNull Location location) {
                    onWeatherLocation(location);
                }

                @Override
                public void onUnavailable(@NonNull LocationService.Unavailable reason) {
                    showCityNotFound();
                }
            });

    private LocationPreferences locationPrefs;
    private SettingsRowView rowPrayerCity;
    private SettingsRowView rowWeatherCity;
    private MaterialButton updateButton;

    public LocationSettingsFragment() {
        super(R.layout.fragment_settings_location);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        locationPrefs = AppPreferences.from(requireContext()).location();
        rowPrayerCity = view.findViewById(R.id.rowPrayerCity);
        rowWeatherCity = view.findViewById(R.id.rowWeatherCity);
        updateButton = view.findViewById(R.id.updateLocationButton);

        // Adım adım seçim açılır; konum yalnızca o ekrandaki "Mevcut konumumu bul" ile aranır
        rowPrayerCity.setOnClickListener(v -> startActivity(new Intent(requireContext(), CitySelectionActivity.class)));

        showWeatherCity(locationPrefs.getLegacyCityName());
        updateButton.setOnClickListener(v -> {
            rowWeatherCity.setSubtitle(R.string.loadingProcess);
            // Arka arkaya basılmasın; sonuç gelmese de (izin penceresi kapatıldı vb.) 5 sn sonra açılır
            updateButton.setEnabled(false);
            updateButton.postDelayed(() -> updateButton.setEnabled(true), 5000);
            locationAccess.requestSingleLocation(true);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Şehir seçim ekranından dönüldüğünde güncel seçimi göster
        PrayerCitySelection selection = locationPrefs.getPrayerCitySelection();
        rowPrayerCity.setSubtitle(selection != null
                ? getString(R.string.prayer_city_current, selection.getDisplayName())
                : getString(R.string.prayer_city_not_selected));
    }

    /** Konumdan il adını bulur (hava durumu bu ili kullanır). */
    private void onWeatherLocation(Location location) {
        new AddressResolver(requireContext()).resolveAsync(location.getLatitude(), location.getLongitude(),
                Locale.forLanguageTag("tr"), address -> {
                    if (getView() == null) {
                        return; // ekran kapandı
                    }
                    String cityName = address != null ? address.getAdminArea() : null;
                    if (cityName == null || City.fromCityName(cityName) == null) {
                        showCityNotFound();
                        return;
                    }
                    locationPrefs.setLegacyCityName(cityName);
                    showWeatherCity(cityName);
                    updateButton.setEnabled(true);
                });
    }

    private void showWeatherCity(@Nullable String cityName) {
        rowWeatherCity.setSubtitle(cityName != null
                ? getString(R.string.locaitonInfoText, cityName)
                : getString(R.string.settings_weather_city_subtitle));
    }

    private void showCityNotFound() {
        if (getView() == null) return;
        rowWeatherCity.setSubtitle(R.string.sehirBulunamadi);
        updateButton.setEnabled(true);
    }
}
