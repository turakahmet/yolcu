package com.myapp.yolcu.ui.zekat;

public class Currency {
    private String kod;
    private String aciklama;
    private Tip tip;
    private double satis;

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

    public double getSatis() {
        return satis;
    }

    public void setSatis(double satis) {
        this.satis = satis;
    }

    public Tip getTip() {
        return tip;
    }

    public void setTip(Tip tip) {
        this.tip = tip;
    }

    enum Tip {
        VARLIK,
        BORC
    }

    @Override
    public String toString() {
        return "Currency{" +
                "kod='" + kod + '\'' +
                ", aciklama='" + aciklama + '\'' +
                ", satis=" + satis +
                '}';
    }
}
