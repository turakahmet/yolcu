package com.islamyolu.app.islamicDays;

import com.google.gson.annotations.SerializedName;

public class IslamicDayResponse {


    @SerializedName("gun")
    private String gun;

    @SerializedName("ay")
    private String ay;

    @SerializedName("yil")
    private int yil;

    @SerializedName("hafta_gunu")
    private String haftaGunu;

    @SerializedName("dini_gun")
    private String diniGun;

    public String getGun() {
        return gun;
    }

    public String getAy() {
        return ay;
    }

    public int getYil() {
        return yil;
    }

    public String getHaftaGunu() {
        return haftaGunu;
    }

    public String getDiniGun() {
        return diniGun;
    }

    public IslamicDayResponse(String gun, String ay, int yil, String haftaGunu, String diniGun) {
        this.gun = gun;
        this.ay = ay;
        this.yil = yil;
        this.haftaGunu = haftaGunu;
        this.diniGun = diniGun;
    }

    public IslamicDayResponse() {
    }

    @Override
    public String toString() {
        return "IslamicDayResponse{" +
                "gun='" + gun + '\'' +
                ", ay='" + ay + '\'' +
                ", yil=" + yil +
                ", haftaGunu='" + haftaGunu + '\'' +
                ", diniGun='" + diniGun + '\'' +
                '}';
    }

    public String getDiniTarih() {
        return gun + " " + ay + " " + yil + " " + haftaGunu;
    }

}
