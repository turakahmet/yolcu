package com.islamyolu.app.prayertimes.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;
import com.islamyolu.app.prayertimes.remote.CountriesResponse;
import com.islamyolu.app.prayertimes.remote.PlacesResponse;

import org.junit.Test;

import java.util.List;

/** §8 konumdan şehir eşleştirme; veri biçimi GitHub Pages v2 dosyalarıyla aynıdır. */
public class PlaceMatcherTest {

    private static final Gson GSON = new Gson();

    private static final List<CountriesResponse.Country> COUNTRIES = GSON.fromJson("{\"countries\":["
            + "{\"id\":33,\"name\":\"ABD\",\"code\":\"USA\",\"key\":\"ABD\",\"codeKey\":\"USA\"},"
            + "{\"id\":13,\"name\":\"ALMANYA\",\"code\":\"GERMANY\",\"key\":\"ALMANYA\",\"codeKey\":\"GERMANY\"},"
            + "{\"id\":2,\"name\":\"TÜRKİYE\",\"code\":\"TÜRKİYE\",\"key\":\"TURKIYE\",\"codeKey\":\"TURKIYE\"},"
            + "{\"id\":40,\"name\":\"KOLOMBIYA\",\"code\":\"KOLOMBIYA\",\"key\":\"KOLOMBIYA\",\"codeKey\":\"KOLOMBIYA\"}"
            + "]}", CountriesResponse.class).getCountries();

    private static final PlacesResponse TURKEY = GSON.fromJson("{\"countryId\":2,\"states\":["
            + "{\"id\":500,\"name\":\"ADANA\",\"code\":\"ADANA\",\"key\":\"ADANA\",\"cities\":["
            + "  {\"id\":9146,\"name\":\"ADANA\",\"code\":\"ADANA\",\"key\":\"ADANA\"},"
            + "  {\"id\":9147,\"name\":\"ALADAĞ\",\"code\":\"ALADAG\",\"key\":\"ALADAG\"}]},"
            + "{\"id\":539,\"name\":\"İSTANBUL\",\"code\":\"ISTANBUL\",\"key\":\"ISTANBUL\",\"cities\":["
            + "  {\"id\":9541,\"name\":\"İSTANBUL\",\"code\":\"ISTANBUL\",\"key\":\"ISTANBUL\"},"
            + "  {\"id\":9542,\"name\":\"KARTAL\",\"code\":\"KARTAL\",\"key\":\"KARTAL\"}]},"
            + "{\"id\":506,\"name\":\"ANKARA\",\"code\":\"ANKARA\",\"key\":\"ANKARA\",\"cities\":["
            + "  {\"id\":9206,\"name\":\"ANKARA\",\"code\":\"ANKARA\",\"key\":\"ANKARA\"},"
            + "  {\"id\":9207,\"name\":\"KARTAL\",\"code\":\"KARTAL\",\"key\":\"KARTAL\"}]}"
            + "]}", PlacesResponse.class);

    private static final PlacesResponse GERMANY = GSON.fromJson("{\"countryId\":13,\"states\":["
            + "{\"id\":800,\"name\":\"BAYERN\",\"code\":\"BAYERN\",\"key\":\"BAYERN\",\"cities\":["
            + "  {\"id\":11000,\"name\":\"MÜNCHEN\",\"code\":\"MUENCHEN\",\"key\":\"MUNCHEN\"}]},"
            + "{\"id\":801,\"name\":\"BERLIN\",\"code\":\"BERLIN\",\"key\":\"BERLIN\",\"cities\":["
            + "  {\"id\":11100,\"name\":\"BERLIN\",\"code\":\"BERLIN\",\"key\":\"BERLIN\"}]}"
            + "]}", PlacesResponse.class);

    // --- Ülke ---

    @Test
    public void ulke_codeKeyVeTakmaAdIleBulunur() {
        assertEquals(2, country("Türkiye").getId());
        assertEquals(2, country("Turkey").getId());
        assertEquals(13, country("Germany").getId());
        assertEquals(33, country("United States").getId());
        assertEquals(40, country("Colombia").getId());
    }

    @Test
    public void ulke_bilinmiyorsaNull() {
        assertNull(PlaceMatcher.matchCountry(new AddressParts("Atlantis", null, null, null), COUNTRIES));
        assertNull(PlaceMatcher.matchCountry(new AddressParts(null, null, null, null), COUNTRIES));
    }

    // --- Türkiye: il = adminArea, ilçe = subAdminArea ---

    @Test
    public void turkiye_ilceTekseSehirBulunur() {
        PlaceMatcher.PlaceMatch match = place(new AddressParts("Türkiye", "Adana", "Aladağ", "Aladağ"), 2, TURKEY);
        assertEquals(PlaceMatcher.Level.CITY, match.level);
        assertEquals(9147, match.city.getId());
        assertEquals(500, match.state.getId());
    }

    @Test
    public void turkiye_merkezIlceListedeYoksaIlSecimeAcilir() {
        // Seyhan, Diyanet listesinde ADANA kaydının içindedir
        PlaceMatcher.PlaceMatch seyhan = place(new AddressParts("Türkiye", "Adana", "Seyhan", null), 2, TURKEY);
        assertEquals(PlaceMatcher.Level.STATE, seyhan.level);
        assertEquals(500, seyhan.state.getId());
        assertNull(seyhan.city);

        PlaceMatcher.PlaceMatch merkez = place(new AddressParts("Türkiye", "Adana", "Merkez", null), 2, TURKEY);
        assertEquals(PlaceMatcher.Level.STATE, merkez.level);
    }

    @Test
    public void turkiye_ilceAyniIldeAranir() {
        // KARTAL hem İstanbul'da hem Ankara'da (test verisi); il bilindiği için tek aday kalır
        PlaceMatcher.PlaceMatch match = place(new AddressParts("Türkiye", "İstanbul", "Kartal", null), 2, TURKEY);
        assertEquals(PlaceMatcher.Level.CITY, match.level);
        assertEquals(9542, match.city.getId());
    }

    @Test
    public void turkiye_ilBilinmezVeIkiAdayVarsaUlkeSeviyesindeSecilir() {
        PlaceMatcher.PlaceMatch match = place(new AddressParts("Türkiye", null, "Kartal", null), 2, TURKEY);
        assertEquals(PlaceMatcher.Level.COUNTRY, match.level);
        assertNull(match.city);
    }

    // --- Diğer ülkeler: şehir = locality ---

    @Test
    public void almanya_localityVeNormalizasyonIleBulunur() {
        PlaceMatcher.PlaceMatch match = place(new AddressParts("Germany", "Bavaria", "Upper Bavaria", "München"), 13, GERMANY);
        assertEquals(PlaceMatcher.Level.CITY, match.level);
        assertEquals(11000, match.city.getId());
        assertEquals(800, match.state.getId());
    }

    @Test
    public void almanya_codeIleDeBulunur() {
        // Bazı Geocoder'lar "Muenchen" döndürür; code alanı MUENCHEN
        PlaceMatcher.PlaceMatch match = place(new AddressParts("Germany", null, null, "Muenchen"), 13, GERMANY);
        assertEquals(PlaceMatcher.Level.CITY, match.level);
        assertEquals(11000, match.city.getId());
    }

    @Test
    public void eslesmeYoksaUlkeSeviyesindeKalir() {
        PlaceMatcher.PlaceMatch match = place(new AddressParts("Germany", "Hessen", null, "Kassel"), 13, GERMANY);
        assertEquals(PlaceMatcher.Level.COUNTRY, match.level);
        assertNull(match.state);
    }

    private static CountriesResponse.Country country(String geocoderName) {
        CountriesResponse.Country c = PlaceMatcher.matchCountry(new AddressParts(geocoderName, null, null, null), COUNTRIES);
        assertNotNull(geocoderName, c);
        return c;
    }

    private static PlaceMatcher.PlaceMatch place(AddressParts address, int countryId, PlacesResponse places) {
        return PlaceMatcher.matchPlace(address, countryId, places);
    }
}
