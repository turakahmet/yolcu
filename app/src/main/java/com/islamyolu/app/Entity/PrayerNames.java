package com.islamyolu.app.Entity;

public enum PrayerNames {
    IMSAK("İmsak"),
    GUNES("Güneş"),
    OGLE("Öğle"),
    IKINDI("İkindi"),
    AKSAM("Akşam"),
    YATSI("Yatsı");

    private final String value;

    PrayerNames(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PrayerNames fromValue(String value) {
        for (PrayerNames prayer : values()) {
            if (prayer.value.equals(value)) {
                return prayer;
            }
        }
        return null;
    }

}
