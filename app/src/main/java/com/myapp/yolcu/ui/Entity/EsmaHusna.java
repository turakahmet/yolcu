package com.myapp.yolcu.ui.Entity;

import java.util.ArrayList;
import java.util.List;

public class EsmaHusna {
    private String Name;
    private String Meaning;
    private boolean dahaOnceSorulduMu=false;

    public EsmaHusna(String isim, String anlam) {
        this.Name=isim;
        this.Meaning=anlam;
    }

    public EsmaHusna() {
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getMeaning() {
        return Meaning;
    }

    public void setMeaning(String meaning) {
        Meaning = meaning;
    }

    public boolean isDahaOnceSorulduMu() {
        return dahaOnceSorulduMu;
    }

    public void setDahaOnceSorulduMu(boolean dahaOnceSorulduMu) {
        this.dahaOnceSorulduMu = dahaOnceSorulduMu;
    }

    // İsim ile anlamı getiren metod
    public String getMeaningbyName(String name, List<EsmaHusna> esmaHusnaListlist) {
        for (EsmaHusna esma : esmaHusnaListlist) {
            if (esma.getName().equals(name)) {
                return esma.getMeaning();
            }
        }
        return null; // Eğer isim bulunmazsa null döndür
    }

    public ArrayList<EsmaHusna> getEsmaHusnaListesiniAl() {
        ArrayList<EsmaHusna> esmaHusnaList = new ArrayList<>();

        esmaHusnaList.add(new EsmaHusna("Allah c.c", "Tüm isim ve sıfatları kendinde toplayan."));
        esmaHusnaList.add(new EsmaHusna("er-Rahmân", "Sonsuz merhametiyle lütuf ve ihsanda bulunan."));
        esmaHusnaList.add(new EsmaHusna("er-Rahîm", "Rahmetiyle her şeyi kuşatan."));
        esmaHusnaList.add(new EsmaHusna("el-Melik", "Bütün varlıkların sahibi/hükümdârı."));
        esmaHusnaList.add(new EsmaHusna("el-Kuddûs", "Her türlü eksiklik ve ayıplardan münezzeh olan."));
        esmaHusnaList.add(new EsmaHusna("es-Selâm", "Esenlik ve selamet veren, kullarını selâmete çıkaran."));
        esmaHusnaList.add(new EsmaHusna("el-Mü’min", "Bütün mahlûkâta emniyet/güven veren ve kendisine güvenilen."));
        esmaHusnaList.add(new EsmaHusna("el-Müheymin", "Kâinatı gözeten ve koruyan."));
        esmaHusnaList.add(new EsmaHusna("el-Azîz", "Ulu, galip, her şeye üstün gelen izzet sahibi."));
        esmaHusnaList.add(new EsmaHusna("el-Cebbâr", "Dilediğini yaptırma gücüne sahip olan, istediğini mutlak yapan, dilediğine muktedir olan."));
        esmaHusnaList.add(new EsmaHusna("el-Mütekebbir", "Büyüklüğünü izhar eden, son derece ulu, yüce."));
        esmaHusnaList.add(new EsmaHusna("el-Hâlık", "Yaratan, yoktan var eden."));
        esmaHusnaList.add(new EsmaHusna("el-Bâri", "Her şeyi kusursuz ve uyumlu yaratan."));
        esmaHusnaList.add(new EsmaHusna("el-Musavvir", "Varlıklara şekil veren."));
        esmaHusnaList.add(new EsmaHusna("el-Gaffâr", "Günahları örten ve çokça bağışlayan."));
        esmaHusnaList.add(new EsmaHusna("el-Kahhâr", "Her şeye galip ve hakim olan."));
        esmaHusnaList.add(new EsmaHusna("el-Vehhâb", "Her türlü nimeti karşılıksız veren."));
        esmaHusnaList.add(new EsmaHusna("er-Rezzâk", "Mahlukatın rızkını yaratan ve veren."));
        esmaHusnaList.add(new EsmaHusna("el-Fettâh", "Her türlü zorluğu açan ve kolaylaştıran."));
        esmaHusnaList.add(new EsmaHusna("el-Alîm", "Her şeyi hakkıyla bilen."));
        esmaHusnaList.add(new EsmaHusna("el-Kâbıd", "Rızkı daraltan, her şeyi hükmü altına alan."));
        esmaHusnaList.add(new EsmaHusna("el-Bâsıt", "Rızkı genişleten, her şeyi ihsan eden."));
        esmaHusnaList.add(new EsmaHusna("el-Hâfıd", "Dereceleri düşüren, alçaltan."));
        esmaHusnaList.add(new EsmaHusna("er-Râfi", "Dereceleri yükselten."));
        esmaHusnaList.add(new EsmaHusna("el-Mu‘iz", "İzzet veren, yücelten."));
        esmaHusnaList.add(new EsmaHusna("el-Müzill", "Zelil eden, alçaltan."));
        esmaHusnaList.add(new EsmaHusna("es-Semî", "Her şeyi işiten."));
        esmaHusnaList.add(new EsmaHusna("el-Basîr", "Her şeyi gören."));
        esmaHusnaList.add(new EsmaHusna("el-Hakem", "Mutlak hakim olan."));
        esmaHusnaList.add(new EsmaHusna("el-Adl", "Son derece adil olan."));
        esmaHusnaList.add(new EsmaHusna("el-Latîf", "Her şeye vakıf, ince ve lütufkâr."));
        esmaHusnaList.add(new EsmaHusna("el-Habîr", "Her şeyden haberdar olan."));
        esmaHusnaList.add(new EsmaHusna("el-Halîm", "Yumuşak huylu, hilm sahibi."));
        esmaHusnaList.add(new EsmaHusna("el-Azîm", "Büyük, azametli."));
        esmaHusnaList.add(new EsmaHusna("el-Gafûr", "Günahları bağışlayan."));
        esmaHusnaList.add(new EsmaHusna("eş-Şekûr", "Şükredenleri bolca mükafatlandıran."));
        esmaHusnaList.add(new EsmaHusna("el-Aliy", "Yüce olan."));
        esmaHusnaList.add(new EsmaHusna("el-Kebîr", "Büyüklükte eşi olmayan."));
        esmaHusnaList.add(new EsmaHusna("el-Hafîz", "Koruyucu olan."));
        esmaHusnaList.add(new EsmaHusna("el-Mukît", "Her varlığın azığını veren."));
        esmaHusnaList.add(new EsmaHusna("el-Hasîb", "Her şeyin hesabını gören."));
        esmaHusnaList.add(new EsmaHusna("el-Celîl", "Celâl, azamet ve ululuk sahibi."));
        esmaHusnaList.add(new EsmaHusna("el-Kerîm", "Çok cömert olan."));
        esmaHusnaList.add(new EsmaHusna("er-Rakîb", "Her varlığı gözeten."));
        esmaHusnaList.add(new EsmaHusna("el-Mucîb", "Dualara cevap veren."));
        esmaHusnaList.add(new EsmaHusna("el-Vâsi", "İlmi ve rahmeti geniş olan."));
        esmaHusnaList.add(new EsmaHusna("el-Hakîm", "Her işi hikmetli olan."));
        esmaHusnaList.add(new EsmaHusna("el-Vedûd", "Kullarını çok seven."));
        esmaHusnaList.add(new EsmaHusna("el-Mecîd", "Şanı yüksek olan."));
        esmaHusnaList.add(new EsmaHusna("el-Bâis", "Ölüleri dirilten."));
        esmaHusnaList.add(new EsmaHusna("eş-Şehîd", "Her zaman hazır ve nazır olan."));
        esmaHusnaList.add(new EsmaHusna("el-Hakk", "Varlığı hiç değişmeyen."));
        esmaHusnaList.add(new EsmaHusna("el-Vekîl", "Her şeyi ayakta tutan."));
        esmaHusnaList.add(new EsmaHusna("el-Kavî", "Çok güçlü olan."));
        esmaHusnaList.add(new EsmaHusna("el-Metîn", "Çok kuvvetli olan."));
        esmaHusnaList.add(new EsmaHusna("el-Velî", "İyi kullarının dostu olan."));
        esmaHusnaList.add(new EsmaHusna("el-Hamîd", "Övülmeye layık olan."));
        esmaHusnaList.add(new EsmaHusna("el-Muhsî", "Yarattığı her varlığı ve her şeyi bilen."));
        esmaHusnaList.add(new EsmaHusna("el-Mübdi", "Mahlukatı örneksiz ve kendine has özellikte yaratan."));
        esmaHusnaList.add(new EsmaHusna("el-Muîd", "Yarattıklarını yok ettikten sonra tekrar yaratan."));
        esmaHusnaList.add(new EsmaHusna("el-Muhyî", "Hayat veren, can bağışlayan."));
        esmaHusnaList.add(new EsmaHusna("el-Mümît", "Canlı bir mahlukun hayatını sonlandıran."));
        esmaHusnaList.add(new EsmaHusna("el-Hayy", "Diri, hayat sahibi olan."));
        esmaHusnaList.add(new EsmaHusna("el-Kayyûm", "Her şeyi varlıkta durduran, kaim kılan."));
        esmaHusnaList.add(new EsmaHusna("el-Vâcid", "Dilediğini yaratan, bulan."));
        esmaHusnaList.add(new EsmaHusna("el-Mâcid", "Kadr u şanı büyük olan."));
        esmaHusnaList.add(new EsmaHusna("el-Vâhid", "Tek, eşsiz olan."));
        esmaHusnaList.add(new EsmaHusna("es-Samed", "Hiçbir şeye muhtaç olmayan."));
        esmaHusnaList.add(new EsmaHusna("el-Kâdir", "Gücü her şeye yeten."));
        esmaHusnaList.add(new EsmaHusna("el-Muktedir", "Kudretli olan."));
        esmaHusnaList.add(new EsmaHusna("el-Mukaddim", "İleri geçiren, öne alan."));
        esmaHusnaList.add(new EsmaHusna("el-Muahhir", "Geri bırakan, erteleyen."));
        esmaHusnaList.add(new EsmaHusna("el-Evvel", "İlk, başlangıcı olmayan."));
        esmaHusnaList.add(new EsmaHusna("el-Âhir", "Son, sonu olmayan."));
        esmaHusnaList.add(new EsmaHusna("ez-Zâhir", "Açık olan."));
        esmaHusnaList.add(new EsmaHusna("el-Bâtın", "Gizli olan."));
        esmaHusnaList.add(new EsmaHusna("el-Vâli", "Kainatın işleriyle tek başına meşgul olan."));
        esmaHusnaList.add(new EsmaHusna("el-Müteâlî", "Son derece yüce olan."));
        esmaHusnaList.add(new EsmaHusna("el-Berr", "İyiliği çok olan."));
        esmaHusnaList.add(new EsmaHusna("et-Tevvâb", "Tevbeleri kabul eden."));
        esmaHusnaList.add(new EsmaHusna("el-Müntekim", "Suçluları adaletiyle cezalandıran."));
        esmaHusnaList.add(new EsmaHusna("el-Afüvv", "Affı çok olan."));
        esmaHusnaList.add(new EsmaHusna("er-Raûf", "Çok merhametli, şefkatli olan."));
        esmaHusnaList.add(new EsmaHusna("Mâlikü'l-Mülk", "Mülkün sahibi olan."));
        esmaHusnaList.add(new EsmaHusna("Zü'l-Celâli ve'l-İkrâm", "Celâl ve ikram sahibi olan."));
        esmaHusnaList.add(new EsmaHusna("el-Muksit", "Adaletle hükmeden."));
        esmaHusnaList.add(new EsmaHusna("el-Câmi", "Toplayıp bir araya getiren."));
        esmaHusnaList.add(new EsmaHusna("el-Ganiyy", "Her şeyden müstağni, zengin olan."));
        esmaHusnaList.add(new EsmaHusna("el-Muğni", "İhtiyaçları gideren, zengin kılan."));
        esmaHusnaList.add(new EsmaHusna("el-Mâni", "Bir şeyin meydana gelmesine engel olan."));
        esmaHusnaList.add(new EsmaHusna("ed-Dârr", "Elem ve zarar verici şeyleri yaratan."));
        esmaHusnaList.add(new EsmaHusna("en-Nâfi", "Faydalı şeyleri yaratan."));
        esmaHusnaList.add(new EsmaHusna("en-Nûr", "Alemleri nurlandıran, dilediğine nur veren."));
        esmaHusnaList.add(new EsmaHusna("el-Hâdî", "Hidayetiyle her varlığı doğru yola sevk eden."));
        esmaHusnaList.add(new EsmaHusna("el-Bedî", "Misalsiz, örneksiz yaratan."));
        esmaHusnaList.add(new EsmaHusna("el-Bâkî", "Varlığının sonu olmayan."));
        esmaHusnaList.add(new EsmaHusna("el-Vâris", "Her şeyin asıl sahibi olan."));
        esmaHusnaList.add(new EsmaHusna("er-Reşîd", "Bütün işlerde isabetli olan."));
        esmaHusnaList.add(new EsmaHusna("es-Sabûr", "Çok sabırlı olan."));
        esmaHusnaList.add(new EsmaHusna("es-Sabûr", "Çok sabırlı olan."));
        esmaHusnaList.add(new EsmaHusna("es-Settar", "Kusur ve hataları örten gizleyen."));

        return esmaHusnaList;
    }

}
