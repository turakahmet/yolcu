package com.islamyolu.app.ibadet;

public class IbadetCardLayout {
    public String imageUrl;
    public String baslik;
    public String okunus;
    public String anlam;

    public IbadetCardLayout(String imageUrl, String baslik, String okunus, String anlam) {
        this.imageUrl = imageUrl;
        this.baslik = baslik;
        this.okunus = okunus;
        this.anlam = anlam;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getBaslik() {
        return baslik;
    }

    public void setBaslik(String baslik) {
        this.baslik = baslik;
    }

    public String getOkunus() {
        return okunus;
    }

    public void setOkunus(String okunus) {
        this.okunus = okunus;
    }

    public String getAnlam() {
        return anlam;
    }

    public void setAnlam(String anlam) {
        this.anlam = anlam;
    }
}
