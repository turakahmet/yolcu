package com.myapp.yolcu.ui.muze;

import android.content.Context;

import com.myapp.yolcu.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MuzeBilgi {
    private String baslik;
    private String aciklama;
    private Integer resimKaynak;
    private ArrayList<Integer> digerGorseller;

    public MuzeBilgi(String baslik, String aciklama,Integer resimKaynak, ArrayList<Integer> digerGorseller) {
        this.baslik=baslik;
        this.aciklama=aciklama;
        this.resimKaynak = resimKaynak;
        this.digerGorseller=digerGorseller;
    }

    public MuzeBilgi() {

    }

    public String getBaslik() {
        return baslik;
    }

    public void setBaslik(String baslik) {
        this.baslik = baslik;
    }

    public String getAciklama() {
        return aciklama;
    }

    public void setAciklama(String aciklama) {
        this.aciklama = aciklama;
    }

    public Integer getResimKaynak() {
        return resimKaynak;
    }

    public void setResimKaynak(Integer resimKaynak) {
        this.resimKaynak = resimKaynak;
    }

    public ArrayList<Integer> getDigerGorseller() {
        return digerGorseller;
    }

    public void setDigerGorseller(ArrayList<Integer> digerGorseller) {
        this.digerGorseller = digerGorseller;
    }

    public List<MuzeBilgi> MuzeBilgiListesiniAl(Context context) {
        ArrayList<MuzeBilgi> muzeBilgis = new ArrayList<>();

        muzeBilgis.add(new MuzeBilgi("Sakal-i Şerif", context.getString(R.string.sakaliserif), R.drawable.sakali_serif,new ArrayList<>(Arrays.asList(R.drawable.sakali_serif_1,R.drawable.sakali_serif_2,R.drawable.sakali_serif_3))));
        muzeBilgis.add(new MuzeBilgi("Hırka-i Şerif", context.getString(R.string.hirkaiiserif), R.drawable.hirkai_serif_1,new ArrayList<>(Arrays.asList(R.drawable.hirkai_serif_2,R.drawable.hirkai_serif_3,R.drawable.hirkai_serif_4))));
        muzeBilgis.add(new MuzeBilgi("Kadem-i Şerif", context.getString(R.string.kademiserif), R.drawable.kademi_serif_2,new ArrayList<>(Arrays.asList(R.drawable.kademi_serif_1,R.drawable.kademi_serif_3))));
        muzeBilgis.add(new MuzeBilgi("Sancak-ı Şerif", context.getString(R.string.sancakiserif), R.drawable.sancaki_serif_1,new ArrayList<>(Arrays.asList(R.drawable.sancaki_serif_2,R.drawable.sancaki_serif_3))));
        muzeBilgis.add(new MuzeBilgi("Na'l-i Şerif", context.getString(R.string.naliserif), R.drawable.nali_serif_1,new ArrayList<>(Collections.singletonList(R.drawable.nali_serif_2))));
        muzeBilgis.add(new MuzeBilgi("Mushaf-ı Şerifler", context.getString(R.string.musafserif), R.drawable.mushafi_serif_1,new ArrayList<>(Arrays.asList(R.drawable.mushafi_serif_2,R.drawable.mushafi_serif_3))));
        muzeBilgis.add(new MuzeBilgi("Kisve-i Şerif", context.getString(R.string.kisveiserif), R.drawable.kisvei_serif_1,new ArrayList<>(Arrays.asList(R.drawable.kisvei_serif_2,R.drawable.kisvei_serif_3,R.drawable.kisvei_serif_4))));
        muzeBilgis.add(new MuzeBilgi("Peygamberimizin Evi", context.getString(R.string.efendimizevi), R.drawable.efendimiz_1,new ArrayList<>(Arrays.asList(R.drawable.efendimiz_2,R.drawable.efendimiz_3,R.drawable.efendimiz_4))));
        muzeBilgis.add(new MuzeBilgi("Peygamberimizin Kılıç ve Yayı", context.getString(R.string.kilicveyay), R.drawable.kilic_1,new ArrayList<>(Arrays.asList(R.drawable.kilic_2,R.drawable.kilic_3,R.drawable.kilic_4))));

        return muzeBilgis;
    }

}
