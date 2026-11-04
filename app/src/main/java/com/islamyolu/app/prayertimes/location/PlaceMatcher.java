package com.islamyolu.app.prayertimes.location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.islamyolu.app.prayertimes.PlaceNameNormalizer;
import com.islamyolu.app.prayertimes.remote.CountriesResponse;
import com.islamyolu.app.prayertimes.remote.PlacesResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Geocoder adresini countries.json / places/{countryId}.json kayıtlarıyla eşler (§8).
 * Adlar botla aynı kuralla normalize edilir. Yanlış eşleşme sessizce kabul edilmez:
 * tek ve kesin aday yoksa sonuç, kullanıcının seçim yapacağı en yakın seviyeyi bildirir.
 */
public final class PlaceMatcher {

    /** Türkiye'de il merkezini ifade eden, Diyanet listesinde karşılığı olmayan ilçe adı. */
    static final String CENTER_DISTRICT = "MERKEZ";
    private static final int TURKEY_COUNTRY_ID = 2;

    /**
     * Geocoder'ın İngilizce ülke adı (normalize) → countries.json codeKey.
     * Yalnızca codeKey/key ile doğrudan eşleşmeyen ülkeler için.
     */
    private static final Map<String, String> COUNTRY_ALIASES = new HashMap<>();

    static {
        alias("TURKIYE", "TURKEY");
        alias("USA", "UNITEDSTATES", "UNITEDSTATESOFAMERICA");
        alias("BOSNIAHERZEGOVINA", "BOSNIAANDHERZEGOVINA");
        alias("CZECHREPUBLIC", "CZECHIA");
        alias("BOLIVYA", "BOLIVIA");
        alias("BUTAN", "BHUTAN");
        alias("CAD", "CHAD");
        alias("CIBUTI", "DJIBOUTI");
        alias("DOGUTIMOR", "TIMORLESTE", "EASTTIMOR");
        alias("DOMINIK", "DOMINICA");
        alias("DOMINIKCUMHURIYETI", "DOMINICANREPUBLIC");
        alias("DEMOKRATICREPUBLICOFTHECONGO", "DEMOCRATICREPUBLICOFTHECONGO", "CONGOKINSHASA");
        alias("EKVATOR", "ECUADOR");
        alias("EKVATORGINESI", "EQUATORIALGUINEA");
        alias("ERITRE", "ERITREA");
        alias("ESTONYA", "ESTONIA");
        alias("FILDISISAHILI", "COTEDIVOIRE", "IVORYCOAST");
        alias("FILISTIN", "PALESTINE", "PALESTINIANTERRITORIES", "STATEOFPALESTINE");
        alias("GAMBIYA", "GAMBIA", "THEGAMBIA");
        alias("GRONLAND", "GREENLAND");
        alias("GUADELOPE", "GUADELOUPE");
        alias("GUAMISLAND", "GUAM");
        alias("IZLANDA", "ICELAND");
        alias("JAMAIKA", "JAMAICA");
        alias("KAMBOCYA", "CAMBODIA");
        alias("KARADAG", "MONTENEGRO");
        alias("KATAR", "QATAR");
        alias("KIRGIZHSTAN", "KYRGYZSTAN");
        alias("KOLOMBIYA", "COLOMBIA");
        alias("KOMORLAR", "COMOROS");
        alias("KOSOVA", "KOSOVO");
        alias("KOSTARIKA", "COSTARICA");
        alias("LESOTO", "LESOTHO");
        alias("LIBERYA", "LIBERIA");
        alias("MADAGASKAR", "MADAGASCAR");
        alias("MAKAO", "MACAO", "MACAU");
        alias("MACEDONIA", "NORTHMACEDONIA");
        alias("MALAVI", "MALAWI");
        alias("MALDIVLER", "MALDIVES");
        alias("MANISLAND", "ISLEOFMAN");
        alias("MARTINIK", "MARTINIQUE");
        alias("MAURITIUSADASI", "MAURITIUS");
        alias("MIKRONEZYA", "MICRONESIA");
        alias("MOLDAVYA", "MOLDOVA");
        alias("MONTSERRATUK", "MONTSERRAT");
        alias("MORITANYA", "MAURITANIA");
        alias("MOZAMBIK", "MOZAMBIQUE");
        alias("NAMIBYA", "NAMIBIA");
        alias("NIJER", "NIGER");
        alias("NIKARAGUA", "NICARAGUA");
        alias("ORTAAFRIKACUMHURIYETI", "CENTRALAFRICANREPUBLIC");
        alias("PAPUAYENIGINE", "PAPUANEWGUINEA");
        alias("PITCAIRNADASI", "PITCAIRNISLANDS");
        alias("RUANDA", "RWANDA");
        alias("SEYSELADALARI", "SEYCHELLES");
        alias("SIRBISTAN", "SERBIA");
        alias("STLUCIA", "SAINTLUCIA");
        alias("SURINAM", "SURINAME");
        alias("SVALBARD", "SVALBARDANDJANMAYEN");
        alias("TANZANYA", "TANZANIA");
        alias("TRINIDATVETOBAGO", "TRINIDADANDTOBAGO");
        alias("TUNUSIA", "TUNISIA");
        alias("UMMAN", "OMAN");
        alias("VATIKAN", "VATICANCITY", "HOLYSEE");
        alias("YENIKALEDONYA", "NEWCALEDONIA");
        alias("YESILBURUN", "CAPEVERDE", "CABOVERDE");
        alias("ZAMBIYA", "ZAMBIA");
        alias("ZIMBABVE", "ZIMBABWE");
        alias("ANTIGUAVEBARBUDA", "ANTIGUAANDBARBUDA");
        alias("RUSSIA", "RUSSIANFEDERATION");
    }

    private static void alias(String codeKey, String... geocoderNames) {
        for (String name : geocoderNames) {
            COUNTRY_ALIASES.put(name, codeKey);
        }
    }

    /** Eşleşmenin ulaştığı en ayrıntılı seviye. */
    public enum Level {
        /** Tek ve kesin şehir/ilçe bulundu (yine de kullanıcıya onaylatılır) */
        CITY,
        /** Eyalet/il bulundu; şehir/ilçe kullanıcıya seçtirilir */
        STATE,
        /** Yalnızca ülke bulundu; eyalet/il kullanıcıya seçtirilir */
        COUNTRY
    }

    public static final class PlaceMatch {
        @NonNull public final Level level;
        @Nullable public final PlacesResponse.State state;
        @Nullable public final PlacesResponse.City city;

        PlaceMatch(@NonNull Level level, @Nullable PlacesResponse.State state, @Nullable PlacesResponse.City city) {
            this.level = level;
            this.state = state;
            this.city = city;
        }
    }

    private PlaceMatcher() {
    }

    /** Önce codeKey (İngilizce), sonra key (Türkçe), sonra takma ad tablosu. Bulunamazsa null. */
    @Nullable
    public static CountriesResponse.Country matchCountry(@NonNull AddressParts address,
                                                         @NonNull List<CountriesResponse.Country> countries) {
        String name = PlaceNameNormalizer.normalize(address.countryName);
        if (name.isEmpty()) {
            return null;
        }
        String alias = COUNTRY_ALIASES.get(name);
        for (CountriesResponse.Country country : countries) {
            if (name.equals(country.getCodeKey())) return country;
        }
        for (CountriesResponse.Country country : countries) {
            if (name.equals(country.getKey())) return country;
        }
        if (alias != null) {
            for (CountriesResponse.Country country : countries) {
                if (alias.equals(country.getCodeKey()) || alias.equals(country.getKey())) return country;
            }
        }
        return null;
    }

    /**
     * Ülkenin places dosyasında eyalet/il ve şehir/ilçe arar.
     * Türkiye'de ilçe genelde subAdminArea, il adminArea; diğer ülkelerde şehir çoğunlukla locality.
     * Şehir önce key, sonra code (normalize) ile aranır.
     */
    @NonNull
    public static PlaceMatch matchPlace(@NonNull AddressParts address, int countryId,
                                        @NonNull PlacesResponse places) {
        boolean turkey = countryId == TURKEY_COUNTRY_ID;
        List<String> stateNames = names(turkey
                ? Collections.singletonList(address.adminArea)
                : Arrays.asList(address.adminArea, address.subAdminArea));
        List<String> cityNames = names(turkey
                ? Arrays.asList(address.subAdminArea, address.locality)
                : Arrays.asList(address.locality, address.subAdminArea));

        PlacesResponse.State state = places.getStates().size() == 1
                ? places.getStates().get(0)
                : findState(places, stateNames);
        List<PlacesResponse.State> scope = state != null
                ? Collections.singletonList(state)
                : places.getStates();

        for (String cityName : cityNames) {
            if (CENTER_DISTRICT.equals(cityName)) {
                // "Merkez" ilçe: Diyanet listesinde il adıyla geçer; kullanıcı seçer
                continue;
            }
            List<PlacesResponse.State> owners = new ArrayList<>();
            List<PlacesResponse.City> matches = findCities(scope, cityName, owners);
            if (matches.size() == 1) {
                return new PlaceMatch(Level.CITY, owners.get(0), matches.get(0));
            }
            if (matches.size() > 1) {
                // Birden fazla aday: hepsi aynı eyaletteyse o eyaletin listesi gösterilir
                PlacesResponse.State common = allSame(owners) ? owners.get(0) : state;
                return new PlaceMatch(common != null ? Level.STATE : Level.COUNTRY, common, null);
            }
        }
        return new PlaceMatch(state != null ? Level.STATE : Level.COUNTRY, state, null);
    }

    @Nullable
    private static PlacesResponse.State findState(PlacesResponse places, List<String> stateNames) {
        for (String name : stateNames) {
            PlacesResponse.State found = null;
            for (PlacesResponse.State state : places.getStates()) {
                if (name.equals(state.getKey()) || name.equals(PlaceNameNormalizer.normalize(state.getCode()))
                        || name.equals(PlaceNameNormalizer.normalize(state.getName()))) {
                    if (found != null) {
                        found = null; // aynı adlı iki eyalet: belirsiz
                        break;
                    }
                    found = state;
                }
            }
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static List<PlacesResponse.City> findCities(List<PlacesResponse.State> scope, String name,
                                                       List<PlacesResponse.State> owners) {
        List<PlacesResponse.City> byKey = new ArrayList<>();
        List<PlacesResponse.State> keyOwners = new ArrayList<>();
        List<PlacesResponse.City> byCode = new ArrayList<>();
        List<PlacesResponse.State> codeOwners = new ArrayList<>();
        for (PlacesResponse.State state : scope) {
            for (PlacesResponse.City city : state.getCities()) {
                if (name.equals(city.getKey())) {
                    byKey.add(city);
                    keyOwners.add(state);
                } else if (name.equals(PlaceNameNormalizer.normalize(city.getCode()))) {
                    byCode.add(city);
                    codeOwners.add(state);
                }
            }
        }
        if (!byKey.isEmpty()) {
            owners.addAll(keyOwners);
            return byKey;
        }
        owners.addAll(codeOwners);
        return byCode;
    }

    private static boolean allSame(List<PlacesResponse.State> states) {
        for (PlacesResponse.State state : states) {
            if (state != states.get(0)) return false;
        }
        return !states.isEmpty();
    }

    private static List<String> names(List<String> raw) {
        List<String> result = new ArrayList<>();
        for (String value : raw) {
            String normalized = PlaceNameNormalizer.normalize(value);
            if (!normalized.isEmpty() && !result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result;
    }
}
