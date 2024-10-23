package com.myapp.yolcu.ui.VakitApi;

import java.text.Normalizer;

public enum City {
    ADANA(37.0, 35.3, "01"),
    ADIYAMAN(37.5, 38.3, "02"),
    AFYONKARAHISAR(38.7, 30.5, "03"),
    AGRI(39.7, 43.0, "04"),
    AMASYA(40.7, 35.8, "05"),
    ANKARA(39.9, 32.9, "06"),
    ANTALYA(36.9, 30.7, "07"),
    ARTVIN(41.2, 41.2, "08"),
    AYDIN(37.8, 27.3, "09"),
    BALIKESIR(39.6, 27.9, "10"),
    BARTIN(41.6, 32.3, "74"),
    BATMAN(37.9, 41.1, "72"),
    BAYBURT(40.2, 40.2, "69"),
    BILECIK(40.2, 29.9, "11"),
    BINGOL(38.9, 40.5, "12"),
    BITLIS(38.4, 42.2, "13"),
    BOLU(40.6, 31.5, "14"),
    BURDUR(37.5, 30.3, "15"),
    BURSA(40.2, 29.1, "16"),
    CANAKKALE(39.9, 26.4, "17"),
    CANKIRI(40.6, 33.6, "18"),
    DENIZLI(37.5, 29.1, "20"),
    DIYARBAKIR(37.9, 40.2, "21"),
    EDMEN(39.7, 43.4, "04"),  // Hatalı plaka kodu, gözden geçirilmeli
    ELAZIG(38.7, 39.2, "23"),
    ERZINCAN(39.0, 39.0, "24"),
    ERZURUM(39.9, 41.3, "25"),
    ESKISEHIR(39.8, 30.5, "26"),
    GAZIANTEP(37.0, 37.0, "27"),
    GIRESUN(40.9, 38.4, "28"),
    GUMUSHANE(40.5, 39.5, "29"),
    HAKKARI(37.6, 43.7, "30"),
    HATAY(36.2, 36.1, "31"),
    ISPARTA(37.8, 30.5, "32"),
    ISTANBUL(41.0, 28.9, "34"),
    IZMIR(38.4, 27.1, "35"),
    KARABUK(41.2, 32.6, "78"),
    KARAMAN(37.0, 33.0, "70"),
    KARS(40.6, 43.1, "36"),
    KASTAMONU(41.3, 33.8, "37"),
    KAYSERI(38.7, 35.5, "38"),
    KIRKLARELI(41.7, 27.2, "39"),
    KIRSEHIR(39.1, 33.6, "40"),
    KUTAHYA(39.4, 29.9, "43"),
    MALATYA(38.4, 38.3, "44"),
    MANISA(38.6, 27.4, "45"),
    MARDIN(37.3, 40.7, "47"),
    MUGLA(37.2, 28.3, "48"),
    NEVSEHIR(38.6, 34.7, "50"),
    NIGDE(37.9, 34.7, "51"),
    ORDU(40.9, 37.9, "52"),
    RIZE(41.0, 40.5, "53"),
    SAKARYA(40.7, 30.4, "54"),
    SIIRT(37.5, 41.9, "56"),
    SINOP(42.0, 35.2, "57"),
    SANLIURFA(37.2, 38.8, "63"),
    TUNCELI(39.1, 39.5, "62"),
    USAK(38.4, 29.4, "64"),
    VAN(38.5, 43.4, "65"),
    YALOVA(40.7, 29.3, "77"),
    ZONGULDAK(41.4, 31.8, "67");

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
