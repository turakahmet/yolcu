package com.islamyolu.app.prayertimes;

/**
 * Kerahat vakitlerinin uygulamadaki TEK tanımı (ana ekran vurgusu ve kerahat bildirimi aynı değeri kullanır).
 * <ul>
 *   <li>Güneş doğduktan sonraki {@link #MINUTES} dakika</li>
 *   <li>Öğle ve akşam vaktinden önceki {@link #MINUTES} dakika</li>
 * </ul>
 */
public final class Kerahat {

    /** Kerahat süresi (dakika) — önceki sürümdeki değer */
    public static final int MINUTES = 45;

    private Kerahat() {
    }

    /** Bu vakitten önceki {@link #MINUTES} dakika kerahat mi (öğle ve akşam). */
    public static boolean precedes(PrayerSlot slot) {
        return slot == PrayerSlot.OGLE || slot == PrayerSlot.AKSAM;
    }
}
