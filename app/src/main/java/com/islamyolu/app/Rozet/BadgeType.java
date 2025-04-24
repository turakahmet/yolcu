package com.islamyolu.app.Rozet;

import com.islamyolu.app.R;

public enum BadgeType {
    BILGI_YARISMA_1("İslam Yolcusu Rozeti", "'Kim hidayete erişmek ister?' yarışmasında ilk 3 soruyu tamamla", R.drawable.medal),
    BILGI_YARISMA_2("İlim Peşinde Rozeti", "'Kim hidayete erişmek ister?' yarışmasında 10. soruya ulaş", R.drawable.medal),
    BILGI_YARISMA_3("Öncü Rozeti", "'Kim hidayete erişmek ister?' yarışmasında, 20 saniyeden önce tüm soruları doğru yanıtla", R.drawable.medal),
    BILGI_YARISMA_4("Salih Amel Rozeti", "'Kim hidayete erişmek ister?' yarışmasında, Joker kullanmadan tüm soruları doğru yanıtla", R.drawable.medal),
    BILGI_YARISMA_5("Hidayet Yolu Rozeti", "10 defa 'Kim hidayete erişmek ister?' yarışmasına katıl", R.drawable.medal),
    BILGI_YARISMA_KAZANAN("Hidayet Rozeti", "'Kim hidayete erişmek ister?' yarışmasında tüm soruları doğru yanıtla", R.drawable.medal),
    KELIME_YARISMA_1("Tevhid Yolcusu Rozeti", "'Kelime-i Hikmet' yarışmasında harf almadan soru yanıtla", R.drawable.medal_2),
    KELIME_YARISMA_2("İlim Sevdalısı Rozeti", "'Kelime-i Hikmet' yarışmasında 10. soruya ulaş", R.drawable.medal_2),
    KELIME_YARISMA_3("Fayda Peşinde Rozeti", "'Kelime-i Hikmet' yarışmasında, 20 saniyeden önce tüm soruları doğru yanıtla", R.drawable.medal_2),
    KELIME_YARISMA_4("İhsan Rozeti", "'Kelime-i Hikmet' yarışmasında, Joker kullanmadan tüm soruları doğru yanıtla", R.drawable.medal_2),
    KELIME_YARISMA_5("Takva Yolcusu Rozeti", "10 defa 'Kelime-i Hikmet' yarışmasına katıl", R.drawable.medal_2),
    KELIME_YARISMA_KAZANAN("Sabırla Başarı Rozeti", "'Kelime-i Hikmet' yarışmasında tüm soruları doğru yanıtla", R.drawable.medal_2),
    ESMA_YARISMA_1("Şükredenler Rozeti", "'Esma-i Hüsna Keşfi' yarışmasında 33 adet doğru yanıt ver", R.drawable.medal_3),
    ESMA_YARISMA_2("Feyz ve Bereket Rozeti", "'Esma-i Hüsna Keşfi' yarışmasında 66 adet doğru yanıt ver", R.drawable.medal_3),
    ESMA_YARISMA_3("Nur Yolu Rozeti", "'Esma-i Hüsna Keşfi' yarışmasında 99 adet doğru yanıt ver", R.drawable.medal_3),
    ESMA_YARISMA_4("Hakkın Yolunda Rozeti", "'Esma-i Hüsna Keşfi' yarışmasında yanlış eşleştirmeden 50 adet doğru yanıt ver", R.drawable.medal_3),
    ESMA_YARISMA_5("Tefekkür Rozeti", "'Esma-i Hüsna Keşfi' yarışmasında yanlış eşleştirmeden tüm eşleştirmeleri doğru yanıtla", R.drawable.medal_3),
    ESMA_YARISMA_KAZANAN("Zikr Yolu Rozeti", "'Kelime-i Hikmet' yarışmasında tüm eşleştirmeleri doğru yanıtla", R.drawable.medal_3),
    KURAN_YARISMA_1("İrfan Yolcusu Rozeti", "'Kur-an Yolculuğu' yarışmasında Normal seviyede 10 adet doğru yanıt ver", R.drawable.medal_4),
    KURAN_YARISMA_2("Takva Rozeti", "'Kur-an Yolculuğu' yarışmasında Zor seviyede 10 adet doğru yanıt ver", R.drawable.medal_4),
    KURAN_YARISMA_3("Kalp Anahtarı Rozeti", "10 defa 'Kur-an Yolculuğu' yarışmasına katıl", R.drawable.medal_4),

    SIYER_YARISMA_1("Darü’l-Erkam Rozeti", "'Siyer-i Nebi' yarışmasında Normal seviyede 10 adet doğru yanıt ver", R.drawable.medal_5),
    SIYER_YARISMA_2("Veda Hutbesi Rozeti", "'Siyer-i Nebi' yarışmasında Zor seviyede 10 adet doğru yanıt ver", R.drawable.medal_5),
    SIYER_YARISMA_3("Ashab-ı Suffa Rozeti ", "10 defa 'Siyer-i Nebi' yarışmasına katıl", R.drawable.medal_5),

    PEYGAMBERLER_YARISMA_1("Tufan Bilgini Rozeti", "'Peygamberler Tarihi' yarışmasında Normal seviyede 10 adet doğru yanıt ver", R.drawable.medal_6),
    PEYGAMBERLER_YARISMA_2("Ahir Zaman Rozeti", "'Peygamberler Tarihi' yarışmasında Zor seviyede 10 adet doğru yanıt ver", R.drawable.medal_6),
    PEYGAMBERLER_YARISMA_3("Nebilerin İzinde Rozeti", "10 defa 'Peygamberler Tarihi' yarışmasına katıl", R.drawable.medal_6),

    ISLAMTARIHI_YARISMA_1("Suffa Talebesi Rozeti", "'İslam Tarihi' yarışmasında Normal seviyede 10 adet doğru yanıt ver", R.drawable.medal_7),
    ISLAMTARIHI_YARISMA_2("Kufe Bilgini Rozeti", "'İslam Tarihi' yarışmasında Zor seviyede 10 adet doğru yanıt ver", R.drawable.medal_7),
    ISLAMTARIHI_YARISMA_3("Asr-ı Saadet Rozeti", "10 defa 'İslam Tarihi' yarışmasına katıl", R.drawable.medal_7);

    // Diğer rozetler

    private final String label;
    private final String description;
    private final int iconResId;

    BadgeType(String label, String description, int iconResId) {
        this.label = label;
        this.description = description;
        this.iconResId = iconResId;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public int getIconResId() {
        return iconResId;
    }
}

