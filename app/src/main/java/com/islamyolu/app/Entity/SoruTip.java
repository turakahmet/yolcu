package com.islamyolu.app.Entity;

public enum SoruTip {
    KURAN,
    SIYER,
    ISLAM_TARIHI,
    PEYGAMBERLER_TARIHI;

    public static SoruTip fromValue(String value) {
        for (SoruTip soruTip : values()) {
            if (soruTip.name().equalsIgnoreCase(value)) {
                return soruTip;
            }
        }
        return null;
    }
}
