package com.islamyolu.app.islamicDays;

public class DiniGun {
    private DiniGunEnum diniGun;
    private String diniTarih;
    private boolean aktifGunMu = false;

    public DiniGun(DiniGunEnum diniGun, String diniTarih) {
        this.diniGun = diniGun;
        this.diniTarih = diniTarih;
    }

    public DiniGunEnum getDiniGun() {
        return diniGun;
    }

    public void setDiniGun(DiniGunEnum diniGun) {
        this.diniGun = diniGun;
    }

    public boolean isAktifGunMu() {
        return aktifGunMu;
    }

    public void setAktifGunMu(boolean aktifGunMu) {
        this.aktifGunMu = aktifGunMu;
    }

    public String getDiniTarih() {
        return diniTarih;
    }

    public void setDiniTarih(String diniTarih) {
        this.diniTarih = diniTarih;
    }

}
