package com.islamyolu.app.zekat;

public class Currency {
    private String kod;
    private String aciklama;
    private double alis;  // Yeni JSON'dan gelen alış fiyatı için eklendi
    private double satis;
    private String guncellenmeZamani; // Yeni JSON'daki tarih/saat bilgisi için eklendi

    public String getKod() {
        return kod;
    }

    public void setKod(String kod) {
        this.kod = kod;
    }

    public String getAciklama() {
        return aciklama;
    }

    public void setAciklama(String aciklama) {
        this.aciklama = aciklama;
    }

    public double getAlis() {
        return alis;
    }

    public void setAlis(double alis) {
        this.alis = alis;
    }

    public double getSatis() {
        return satis;
    }

    public void setSatis(double satis) {
        this.satis = satis;
    }

    public String getGuncellenmeZamani() {
        return guncellenmeZamani;
    }

    public void setGuncellenmeZamani(String guncellenmeZamani) {
        this.guncellenmeZamani = guncellenmeZamani;
    }

    @Override
    public String toString() {
        return "Currency{" +
                "kod='" + kod + '\'' +
                ", aciklama='" + aciklama + '\'' +
                ", alis=" + alis +
                ", satis=" + satis +
                ", guncellenmeZamani='" + guncellenmeZamani + '\'' +
                '}';
    }
}