package com.islamyolu.app.Entity;

import java.util.List;

public class Question {
    private String soruMetni;  // Sorunun metni
    private int zorlukSeviyesi;  // Sorunun zorluk seviyesi: 1 - Kolay, 2 - Orta, 3 - Zor
    private String[] siklar;  // Sorunun 4 şıkkı
    private String dogruCevap;  // Sorunun doğru cevabı
    private String ipucu;  // Sorunun ipucu
    private List<SoruTip> soruTipleri;//soru tipi

    // Constructor
    public Question(String soruMetni, int zorlukSeviyesi, String[] siklar, String dogruCevap, String ipucu, List<SoruTip> soruTipleri) {
        this.soruMetni = soruMetni;
        this.zorlukSeviyesi = zorlukSeviyesi;
        this.siklar = siklar;
        this.dogruCevap = dogruCevap;
        this.ipucu = ipucu;
        this.soruTipleri = soruTipleri;
    }

    // Getter ve Setter metotları
    public String getSoruMetni() {
        return soruMetni;
    }

    public void setSoruMetni(String soruMetni) {
        this.soruMetni = soruMetni;
    }

    public int getZorlukSeviyesi() {
        return zorlukSeviyesi;
    }

    public void setZorlukSeviyesi(int zorlukSeviyesi) {
        // Zorluk seviyesi sadece 1, 2 veya 3 olabilir
        if (zorlukSeviyesi < 1 || zorlukSeviyesi > 3) {
            throw new IllegalArgumentException("Zorluk seviyesi sadece 1 (Kolay), 2 (Orta), 3 (Zor) olabilir.");
        }
        this.zorlukSeviyesi = zorlukSeviyesi;
    }

    public String[] getSiklar() {
        return siklar;
    }

    public void setSiklar(String[] siklar) {
        this.siklar = siklar;
    }

    public String getDogruCevap() {
        return dogruCevap;
    }

    public void setDogruCevap(String dogruCevap) {
        this.dogruCevap = dogruCevap;
    }

    public String getIpucu() {
        return ipucu;
    }

    public void setIpucu(String ipucu) {
        this.ipucu = ipucu;
    }

    public List<SoruTip> getSoruTipleri() {
        return soruTipleri;
    }

    public void setSoruTipleri(List<SoruTip> soruTipleri) {
        this.soruTipleri = soruTipleri;
    }

    // Zorluk seviyesini string olarak döndürmek için yardımcı bir metot
    public String zorlukSeviyesiniYazdir() {
        switch (zorlukSeviyesi) {
            case 1:
                return "Kolay";
            case 2:
                return "Orta";
            case 3:
                return "Zor";
            default:
                return "Bilinmeyen";
        }
    }

    // Sorunun detaylarını yazdırmak için bir metot
    public void soruBilgileriniYazdir() {
        System.out.println("Soru: " + soruMetni);
        System.out.println("Zorluk: " + zorlukSeviyesiniYazdir());
        System.out.println("Şıklar: ");
        for (int i = 0; i < siklar.length; i++) {
            System.out.println((i + 1) + ". " + siklar[i]);
        }
        System.out.println("Doğru Cevap: " + dogruCevap);
        System.out.println("İpucu: " + ipucu);
    }

    // Doğru cevabın indeksini döndüren metot
    public int getCorrectAnswerIndex() {
        for (int i = 0; i < siklar.length; i++) {
            if (siklar[i].equals(dogruCevap)) {
                return i;  // Doğru cevabın indeksi
            }
        }
        return -1; // Eğer doğru cevap bulunamazsa -1 döner
    }

    public String questionSpaceHint() {
        // Soru metni içindeki boşluk karakterlerini saymak için bir sayaç başlatıyoruz
        int spaceCount = 1;

        // Soru metnindeki her karakteri kontrol ediyoruz
        for (int i = 0; i < this.dogruCevap.length(); i++) {
            if (this.dogruCevap.charAt(i) == ' ') {
                spaceCount++;
            }
        }
        if (spaceCount > 1)
            return "İpucu: " + spaceCount + " Kelime";
        else
            return null;
    }

    public boolean isDogruCevapWordGame() {
        // Yasak karakterler
        String forbiddenChars = "[',\".\\-\\(\\)]"; // Tek tırnak, virgül, çift tırnak, nokta, tire, parantez

        // "dogruCevap" yasak karakter içeriyor mu?
        boolean hasForbiddenChars = dogruCevap.matches(".*" + forbiddenChars + ".*");

        // "dogruCevap" boşluklar hariç 12 karakterden kısa mı?
        boolean isShortAnswer = dogruCevap.replace(" ", "").length() < 12;

        // "soruMetni" içinde 'aşağıdaki' veya 'hangisi' kelimeleri var mı?
        boolean containsInvalidWords = soruMetni.toLowerCase().contains("aşağıdaki") || soruMetni.toLowerCase().contains("hangisi");

        // Tüm koşulları kontrol et ve sonucu döndür
        return !hasForbiddenChars && isShortAnswer && !containsInvalidWords;
    }

    public boolean isNumeric() {
        // dogruCevap değerinin tamamının sayısal olup olmadığını kontrol et
        return dogruCevap.matches("[0-9]+");
    }

}

