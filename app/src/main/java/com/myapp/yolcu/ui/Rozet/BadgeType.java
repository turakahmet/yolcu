package com.myapp.yolcu.ui.Rozet;

import com.myapp.yolcu.R;

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
    KELIME_YARISMA_KAZANAN("Sabırla Başarı Rozeti", "'Kelime-i Hikmet' yarışmasında tüm soruları doğru yanıtla", R.drawable.medal_2);
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

