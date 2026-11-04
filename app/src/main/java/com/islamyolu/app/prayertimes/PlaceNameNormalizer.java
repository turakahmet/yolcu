package com.islamyolu.app.prayertimes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Ülke/eyalet/şehir adlarını GitHub botuyla aynı kuralla normalize eder
 * (places/*.json içindeki "key"/"codeKey" alanlarıyla karşılaştırmak için).
 * Örnek: "Devrekâni" → "DEVREKANI", "München" → "MUNCHEN".
 */
public final class PlaceNameNormalizer {

    private static final Locale TURKISH = Locale.forLanguageTag("tr");
    private static final String TURKISH_CHARS = "ÇĞİÖŞÜÂÎÛ";
    private static final String ASCII_CHARS = "CGIOSUAIU";

    private PlaceNameNormalizer() {
    }

    @NonNull
    public static String normalize(@Nullable String name) {
        if (name == null) {
            return "";
        }
        // 1. Türkçe büyük harf
        String upper = name.replace('i', 'İ').replace('ı', 'I').toUpperCase(TURKISH);

        // 2. Türkçe harfleri ASCII karşılığına çevir
        StringBuilder ascii = new StringBuilder(upper.length());
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            int index = TURKISH_CHARS.indexOf(c);
            ascii.append(index >= 0 ? ASCII_CHARS.charAt(index) : c);
        }

        // 3. Aksanları ayır ve ASCII harf/rakam dışındaki her şeyi at
        return Normalizer.normalize(ascii, Normalizer.Form.NFKD).replaceAll("[^A-Z0-9]", "");
    }
}
