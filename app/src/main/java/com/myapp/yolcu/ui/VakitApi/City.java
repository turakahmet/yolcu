package com.myapp.yolcu.ui.VakitApi;

import java.text.Normalizer;

public enum City {
    ADANA(36.985420, 35.325020, "01"),
    ADIYAMAN(37.762990, 38.277298, "02"),
    AFYONKARAHISAR(38.756217, 30.537846, "03"),
    AGRI(39.718993, 43.047663, "04"),
    AMASYA(40.656314, 35.837068, "05"),
    ANKARA(39.942928, 32.860481, "06"),
    ANTALYA(36.896126, 30.713081, "07"),
    ARTVIN(41.181310, 41.820537, "08"),
    AYDIN(37.838044, 27.845571, "09"),
    BALIKESIR(39.644878, 27.885361, "10"),
    BARTIN(41.635830, 32.337910, "74"),
    BATMAN(37.881168, 41.135090, "72"),
    BAYBURT(40.255169, 40.224880, "69"),
    BILECIK(40.142960, 29.979159, "11"),
    BINGOL(38.884720, 40.496391, "12"),
    BITLIS(38.400664, 42.108971, "13"),
    BOLU(40.732006, 31.607052, "14"),
    BURDUR(37.718293, 30.282248, "15"),
    BURSA(40.182816, 29.066148, "16"),
    CANAKKALE(40.146777, 26.408220, "17"),
    CANKIRI(40.600190, 33.616304, "18"),
    CORUM(40.549853, 34.953694, "19"),
    DENIZLI(37.783026, 29.096246, "20"),
    DIYARBAKIR(37.913700, 40.224899, "21"),
    EDIRNE(41.675907, 26.553608, "22"),
    ELAZIG(38.676300, 39.221802, "23"),
    ERZINCAN(39.750000, 39.500000, "24"),
    ERZURUM(39.905994, 41.273784, "25"),
    ESKISEHIR(39.766724, 30.525608, "26"),
    GAZIANTEP(37.062931, 37.378159, "27"),
    GIRESUN(40.917921, 38.389876, "28"),
    GUMUSHANE(40.458673, 39.478961, "29"),
    HAKKARI(37.578120, 43.733805, "30"),
    HATAY(36.202776, 36.160023, "31"),
    ISPARTA(37.764771, 30.556561, "32"),
    MERSIN(36.812103, 34.641464, "33"),
    ISTANBUL(41.076601, 29.052495, "34"),
    IZMIR(38.419220, 27.128720, "35"),
    KARS(40.602220, 43.094722, "36"),
    KASTAMONU(41.388710, 33.782730, "37"),
    KAYSERI(38.731220, 35.478729, "38"),
    KIRKLARELI(41.735080, 27.225240, "39"),
    KIRSEHIR(39.145832, 34.163887, "40"),
    KOCAELI(40.765440, 29.940590, "41"),
    KONYA(37.866667, 32.483333, "42"),
    KUTAHYA(39.424170, 29.983330, "43"),
    MALATYA(38.355190, 38.309460, "44"),
    MANISA(38.619099, 27.428921, "45"),
    KAHRAMANMARAS(37.585831, 36.937149, "46"),
    MARDIN(37.312236, 40.735110, "47"),
    MUGLA(37.215278, 28.363611, "48"),
    MUS(38.743240, 41.506481, "49"),
    NEVSEHIR(38.624420, 34.723969, "50"),
    NIGDE(37.966667, 34.683333, "51"),
    ORDU(40.983334, 37.883331, "52"),
    RIZE(41.020050, 40.523449, "53"),
    SAKARYA(40.756931, 30.402210, "54"),
    SAMSUN(41.286667, 36.33, "55"),
    SIIRT(37.933334, 41.950001, "56"),
    SINOP(42.026859, 35.162529, "57"),
    SIVAS(39.748611, 37.016388, "58"),
    TEKIRDAG(40.978000, 27.511000, "59"),
    TOKAT(40.316666, 36.550000, "60"),
    TRABZON(41.001450, 39.717800, "61"),
    TUNCELI(39.108051, 39.547218, "62"),
    SANLIURFA(37.167080, 38.795940, "63"),
    USAK(38.682301, 29.408190, "64"),
    VAN(38.494570, 43.383230, "65"),
    YOZGAT(39.818081, 34.814690, "66"),
    ZONGULDAK(41.456409, 31.798731, "67"),
    AKSARAY(38.368690, 34.036980, "68"),
    KARAMAN(37.181110, 33.215000, "70"),
    KIRIKKALE(39.846821, 33.515251, "71"),
    SIRNAK(37.418748, 42.491834, "73"),
    ARDAHAN(41.108680, 42.702171, "75"),
    IGDIR(39.923683, 44.045013, "76"),
    YALOVA(40.655312, 29.276933, "77"),
    KARABUK(41.204834, 32.627712, "78"),
    KILIS(36.718399, 37.121220, "79"),
    OSMANIYE(37.074568, 36.247656, "80"),
    DUZCE(40.843849, 31.156540, "81");

    private final double latitude;
    private final double longitude;
    private final String plateCode;

    City(double latitude, double longitude, String plateCode) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.plateCode = plateCode;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getPlateCode() {
        return plateCode;
    }

    //	39.925533,	32.866287
    @Override
    public String toString() {
        return name() + " (Plaka Kodu: " + plateCode + ")";
    }

    public static City fromPlateCode(String plateCode) {
        for (City city : values()) {
            if (city.plateCode.equals(plateCode)) {
                return city;
            }
        }
        return null; // veya isterseniz IllegalArgumentException fırlatabilirsiniz
    }

    // Türkçe karakterleri normalleştir ve büyük harflere dönüştür
    private static String normalizeCityName(String cityName) {
        cityName = Normalizer.normalize(cityName, Normalizer.Form.NFD);
        cityName = cityName.replaceAll("\\p{M}", ""); // Diakritikleri kaldır (örn: ğ, ü)
        return cityName.toUpperCase(); // Büyük harfe çevir
    }

    public static City fromCityName(String cityName) {
        String normalizedCityName = normalizeCityName(cityName);

        for (City city : values()) {
            if (city.name().equals(normalizedCityName)) {
                return city;
            }
        }
        return null; // veya isterseniz IllegalArgumentException fırlatabilirsiniz
    }
}
