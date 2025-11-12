package com.islamyolu.app.islamicDays;

public enum DiniGunEnum {
    UC_AYLARIN_BASLANGICI("Üç Ayların Başlangıcı"),
    REGAIB_KANDILI("Regaib Kandili"),
    MIRAC_KANDILI("Miraç Kandili"),
    BERAT_KANDILI("Berat Kandili"),
    RAMAZAN_BASLANGICI("Ramazan Başlangıcı"),
    KADIR_GECESI("Kadir Gecesi"),
    RAMAZAN_BAYRAMI_AREFE("Ramazan Bayramı Arefesi"),
    RAMAZAN_BAYRAMI_1("Ramazan Bayramı (1. Gün)"),
    RAMAZAN_BAYRAMI_2("Ramazan Bayramı (2. Gün)"),
    RAMAZAN_BAYRAMI_3("Ramazan Bayramı (3. Gün)"),
    KURBAN_BAYRAMI_AREFE("Kurban Bayramı Arefesi"),
    KURBAN_BAYRAMI_1("Kurban Bayramı (1. Gün)"),
    KURBAN_BAYRAMI_2("Kurban Bayramı (2. Gün)"),
    KURBAN_BAYRAMI_3("Kurban Bayramı (3. Gün)"),
    KURBAN_BAYRAMI_4("Kurban Bayramı (4. Gün)"),
    HICRI_YILBASI("Hicri Yılbaşı"),
    ASURE_GUNU("Aşure Günü"),
    MEVLID_KANDILI("Mevlid Kandili");

    private final String ad;

    DiniGunEnum(String ad) {
        this.ad = ad;
    }

    public static DiniGunEnum fromValue(String diniGun) {
        for (DiniGunEnum value : values()) {
            if (value.getAd().equals(diniGun)) {
                return value;
            }
        }
        return null;
    }

    public String getAd() {
        return ad;
    }

}
