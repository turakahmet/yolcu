package com.islamyolu.app.VakitApi;

import java.util.HashMap;

public class CityUrlMapping {

    private HashMap<String, String> cityUrlMap;

    public CityUrlMapping() {
        cityUrlMap = new HashMap<>();
        cityUrlMap.put("Ankara", "https://namazvakitleri.diyanet.gov.tr/tr-TR/9206");
        cityUrlMap.put("İstanbul", "https://namazvakitleri.diyanet.gov.tr/tr-TR/9541");
        cityUrlMap.put("İzmir", "https://namazvakitleri.diyanet.gov.tr/tr-TR/9560");
        cityUrlMap.put("Erzurum", "https://namazvakitleri.diyanet.gov.tr/tr-TR/9451");
    }

    public String getUrlForCity(String city) {
        return cityUrlMap.get(city);
    }
}
