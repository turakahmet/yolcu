package com.islamyolu.app.prayertimes.location;

import android.location.Address;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Geocoder (Locale.ENGLISH) adresinin eşleştirmede kullanılan alanları.
 * android.location.Address'ten bağımsızdır; böylece eşleştirme saf Java ile test edilir.
 */
public final class AddressParts {

    @Nullable final String countryName;
    /** Türkiye'de il */
    @Nullable final String adminArea;
    /** Türkiye'de ilçe */
    @Nullable final String subAdminArea;
    /** Diğer ülkelerde çoğunlukla şehir */
    @Nullable final String locality;

    public AddressParts(@Nullable String countryName, @Nullable String adminArea,
                        @Nullable String subAdminArea, @Nullable String locality) {
        this.countryName = countryName;
        this.adminArea = adminArea;
        this.subAdminArea = subAdminArea;
        this.locality = locality;
    }

    @Nullable
    public String getAdminArea() {
        return adminArea;
    }

    @Nullable
    public String getSubAdminArea() {
        return subAdminArea;
    }

    @Nullable
    public String getLocality() {
        return locality;
    }

    /** Geocoder'ın Locale.ENGLISH ile döndürdüğü adresten. */
    @NonNull
    public static AddressParts from(@NonNull Address address) {
        return new AddressParts(address.getCountryName(), address.getAdminArea(),
                address.getSubAdminArea(), address.getLocality());
    }
}
