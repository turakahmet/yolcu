package com.myapp.yolcu.ui.Entity;

import android.content.Context;

import com.myapp.yolcu.R;

import java.util.ArrayList;
import java.util.List;

public class Ibadet {
    private String baslik;
    private String aciklama1;
    private String aciklama2;
    private String kaynak;
    private int resimKaynak;

    public Ibadet(String baslik,String aciklama1,String kaynak, String aciklama2, int resimKaynak) {
        this.baslik=baslik;
        this.aciklama1=aciklama1;
        this.aciklama2=aciklama2;
        this.kaynak=kaynak;
        this.resimKaynak = resimKaynak;
    }

    public Ibadet() {

    }

    public String getBaslik() {
        return baslik;
    }

    public void setBaslik(String baslik) {
        this.baslik = baslik;
    }

    public String getAciklama1() {
        return aciklama1;
    }

    public void setAciklama1(String aciklama1) {
        this.aciklama1 = aciklama1;
    }

    public String getAciklama2() {
        return aciklama2;
    }

    public void setAciklama2(String aciklama2) {
        this.aciklama2 = aciklama2;
    }

    public int getResimKaynak() {
        return resimKaynak;
    }

    public void setResimKaynak(int resimKaynak) {
        this.resimKaynak = resimKaynak;
    }

    public String getKaynak() {
        return kaynak;
    }

    public void setKaynak(String kaynak) {
        this.kaynak = kaynak;
    }

    public List<Ibadet> KirkHadisListesiniAl(Context context) {
        ArrayList<Ibadet> ibadetList = new ArrayList<>();

        ibadetList.add(new Ibadet("İhlâs ve Salih Amel",context.getString(R.string.ihlas_ve_salih_amel),context.getString(R.string.ihlas_ve_salih_amel_K),context.getString(R.string.ihlas_ve_salih_amel_T), R.drawable.k_hadis_1));
        ibadetList.add(new Ibadet("Tevbe", context.getString(R.string.tevbe),context.getString(R.string.tevbe_K),context.getString(R.string.tevbe_T),R.drawable.k_hadis_2));
        ibadetList.add(new Ibadet("Kanaatkârlık", context.getString(R.string.kanaatkarlik),context.getString(R.string.kanaatkarlik_K),context.getString(R.string.kanaatkarlik_T), R.drawable.k_hadis_3));
        ibadetList.add(new Ibadet("Sabır ve Şükür", context.getString(R.string.sabirvesukur),context.getString(R.string.sabirvesukur_K),context.getString(R.string.sabirvesukur_T), R.drawable.k_hadis_4));
        ibadetList.add(new Ibadet("Musibetlere Sabır", context.getString(R.string.musibetleresabir),context.getString(R.string.musibetleresabir_K),context.getString(R.string.musibetleresabir_T), R.drawable.k_hadis_5));
        ibadetList.add(new Ibadet("Peygamberimiz'den Üç Tavsiye", context.getString(R.string.uctavsiye),context.getString(R.string.uctavsiye_K),context.getString(R.string.uctavsiye_T), R.drawable.k_hadis_6));
        ibadetList.add(new Ibadet("Akıllı Kişi", context.getString(R.string.akillikisi),context.getString(R.string.akillikisi_K),context.getString(R.string.akillikisi_T), R.drawable.k_hadis_7));
        ibadetList.add(new Ibadet("Camilerle Dost Olmak", context.getString(R.string.camilerledostolmak),context.getString(R.string.camilerledostolmak_K),context.getString(R.string.camilerledostolmak_T), R.drawable.k_hadis_8));
        ibadetList.add(new Ibadet("Cuma Namazı ve Hutbe", context.getString(R.string.cumanamazihutbe),context.getString(R.string.cumanamazihutbe_K),context.getString(R.string.cumanamazihutbe_T), R.drawable.k_hadis_9));
        ibadetList.add(new Ibadet("Günahlardan Arınma Yolu", context.getString(R.string.gunahlardanarinma),context.getString(R.string.gunahlardanarinma_K),context.getString(R.string.gunahlardanarinma_T), R.drawable.k_hadis_9_1));
        ibadetList.add(new Ibadet("Bid’atlardan Sakınmak", context.getString(R.string.bidatlardansakinma),context.getString(R.string.bidatlardansakinma_K),context.getString(R.string.bidatlardansakinma_T), R.drawable.k_hadis_10));
        ibadetList.add(new Ibadet("Müslümanların Dayanışması", context.getString(R.string.muslumandayanismasi),context.getString(R.string.muslumandayanismasi_K),context.getString(R.string.muslumandayanismasi_T), R.drawable.k_hadis_11));
        ibadetList.add(new Ibadet("Müslüman Müslümanın Kardeşidir", context.getString(R.string.muslumankardesligi),context.getString(R.string.muslumankardesligi_K),context.getString(R.string.muslumankardesligi_T), R.drawable.k_hadis_12));
        ibadetList.add(new Ibadet("Yetimi Gözetmek", context.getString(R.string.yetimigozetmek),context.getString(R.string.yetimigozetmek_K),context.getString(R.string.yetimigozetmek_T), R.drawable.k_hadis_13));
        ibadetList.add(new Ibadet("Misafire İkram", context.getString(R.string.misafireikram),context.getString(R.string.misafireikram_K),context.getString(R.string.misafireikram_T), R.drawable.k_hadis_14));
        ibadetList.add(new Ibadet("Büyüklere Saygı, Küçüklere Sevgi", context.getString(R.string.buyukleresaygi),context.getString(R.string.buyukleresaygi_K),context.getString(R.string.buyukleresaygi_T), R.drawable.k_hadis_15));
        ibadetList.add(new Ibadet("Yaşlılara Hürmet Göstermek", context.getString(R.string.yasliyahurmet),context.getString(R.string.yasliyahurmet_K),context.getString(R.string.yasliyahurmet_T), R.drawable.k_hadis_16));
        ibadetList.add(new Ibadet("Tevbe Kapısı", context.getString(R.string.tevbekapisi),context.getString(R.string.tevbekapisi_K),context.getString(R.string.tevbekapisi_T), R.drawable.k_hadis_17));
        ibadetList.add(new Ibadet("Yaptıklarımız Bizimle Gider", context.getString(R.string.bizimlegider),context.getString(R.string.bizimlegider_K),context.getString(R.string.bizimlegider_T), R.drawable.k_hadis_18));
        ibadetList.add(new Ibadet("İnfak", context.getString(R.string.infak),context.getString(R.string.infak_K),context.getString(R.string.infak_T), R.drawable.k_hadis_19));
        ibadetList.add(new Ibadet("Sadaka", context.getString(R.string.sadaka),context.getString(R.string.sadaka_K),context.getString(R.string.sadaka_T), R.drawable.k_hadis_20));
        ibadetList.add(new Ibadet("Kolaylık", context.getString(R.string.kolaylik),context.getString(R.string.kolaylik_K),context.getString(R.string.kolaylik_T), R.drawable.k_hadis_21));
        ibadetList.add(new Ibadet("Yiğit Adam", context.getString(R.string.yigitadam), context.getString(R.string.yigitadam_K),context.getString(R.string.yigitadam_T), R.drawable.k_hadis_22));
        ibadetList.add(new Ibadet("İmanın Olgunluğu", context.getString(R.string.imaninolgunlugu),context.getString(R.string.imaninolgunlugu_K),context.getString(R.string.imaninolgunlugu_T), R.drawable.k_hadis_23));
        ibadetList.add(new Ibadet("Özü ve Sözü Bir Olma", context.getString(R.string.sozubirolma),context.getString(R.string.sozubirolma_K),context.getString(R.string.sozubirolma_T), R.drawable.k_hadis_24));
        ibadetList.add(new Ibadet("Görgü Kuralları: Âdâbı Muâşeret", context.getString(R.string.gorgu),context.getString(R.string.gorgu_K),context.getString(R.string.gorgu_T), R.drawable.k_hadis_25));
        ibadetList.add(new Ibadet("Müslümanın Müslümana Karşı Sorumlulukları", context.getString(R.string.sorumluluk),context.getString(R.string.sorumluluk_K),context.getString(R.string.sorumluluk_T), R.drawable.k_hadis_26));
        ibadetList.add(new Ibadet("En Değerli Ameller", context.getString(R.string.degerliAmeller),context.getString(R.string.degerliAmeller_K),context.getString(R.string.degerliAmeller_T), R.drawable.k_hadis_27));
        ibadetList.add(new Ibadet("Peygamberimiz'e Salavat Getirmek", context.getString(R.string.slavat),context.getString(R.string.slavat_K),context.getString(R.string.slavat_T), R.drawable.k_hadis_28));
        ibadetList.add(new Ibadet("Ramazan Ayının Fazileti", context.getString(R.string.ramazanFazileti),context.getString(R.string.ramazanFazileti_K),context.getString(R.string.ramazanFazileti_T), R.drawable.k_hadis_29));
        ibadetList.add(new Ibadet("Kadir Gecesi", context.getString(R.string.kadirGecesi),context.getString(R.string.kadirGecesi_K),context.getString(R.string.kadirGecesi_T), R.drawable.k_hadis_30));
        ibadetList.add(new Ibadet("Kadir Gecesi Duası", context.getString(R.string.kadirGecesiDuasi),context.getString(R.string.kadirGecesiDuasi_K), context.getString(R.string.kadirGecesiDuasi_T), R.drawable.k_hadis_31));
        ibadetList.add(new Ibadet("Ramazan Ayının Kıymeti", context.getString(R.string.ramazaninKiymeti),context.getString(R.string.ramazaninKiymeti_K),context.getString(R.string.ramazaninKiymeti_T), R.drawable.k_hadis_32));
        ibadetList.add(new Ibadet("Allah Yolunda Harcamak", context.getString(R.string.hakYolunaHarcamak),context.getString(R.string.hakYolunaHarcamak_K),context.getString(R.string.hakYolunaHarcamak_T), R.drawable.k_hadis_33));
        ibadetList.add(new Ibadet("Alışveriş Ahlakı", context.getString(R.string.alisverisAhlaki),context.getString(R.string.alisverisAhlaki_K),context.getString(R.string.alisverisAhlaki_T), R.drawable.k_hadis_34));
        ibadetList.add(new Ibadet("Kutlu İlim Yolculuğu", context.getString(R.string.ilimYolculugu),context.getString(R.string.ilimYolculugu_K),context.getString(R.string.ilimYolculugu_T), R.drawable.k_hadis_35));
        ibadetList.add(new Ibadet("Secdelerle Allah'a Yaklaşmak", context.getString(R.string.yakinlasmak),context.getString(R.string.yakinlasmak_K), context.getString(R.string.yakinlasmak_T), R.drawable.k_hadis_36));
        ibadetList.add(new Ibadet("Peygamberimiz'in Dilinden Dualar", context.getString(R.string.peygamberdenDualar),context.getString(R.string.peygamberdenDualar_K),context.getString(R.string.peygamberdenDualar_T), R.drawable.k_hadis_37));
        ibadetList.add(new Ibadet("Subhânallah", context.getString(R.string.subhanallah),context.getString(R.string.subhanallah_K),context.getString(R.string.subhanallah_T), R.drawable.k_hadis_38));
        ibadetList.add(new Ibadet("Müslümanın Basireti", context.getString(R.string.basiret),context.getString(R.string.basiret_K),context.getString(R.string.basiret_T), R.drawable.k_hadis_39));

        return ibadetList;
    }

}
