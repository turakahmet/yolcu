---
paths:
  - "app/src/main/java/**/*.java"
  - "app/src/main/res/**/*.xml"
  - "app/src/test/**"
  - "app/src/androidTest/**"
  - "app/schemas/**"
  - "app/build.gradle*"
  - "gradle/libs.versions.toml"
---

# Namaz Vakitleri — Room ve Çevrim Dışı Veri Kuralları

Bu kurallar namaz vakitlerinin GitHub Pages'ten indirilip Room'a kaydedilmesini ve internetsiz
okunmasını tanımlar. Uygulamada şu an Room yoktur; ilk kurulum bu kurallara göre yapılır.

## 0. Başlamadan önce (zorunlu)

- Mevcut namaz vakti kodunu bul ve incele: vakitleri kendi hesaplayan sınıf (lar), seçili
  konum/şehrin SharedPreferences'ta nasıl tutulduğu, vakitleri okuyan ekranlar, widget'lar,
  bildirim/alarm zamanlayıcıları, `BOOT_COMPLETED` alıcıları ve mevcut Retrofit kurulumu.
- Çağrı noktalarının listesini ve plana dair etki alanını çıkar; dosya/sınıf adı uydurma.
- Mevcut hesaplama mantığının akıbetini (yedek olarak kalması / kaldırılması) **kullanıcıya sor**;
  sormadan kaldırma veya değiştirme (bkz. §7).
- Room, WorkManager ve Retrofit bağımlılıklarını `gradle/libs.versions.toml` üzerinden ekle;
  sürümleri katalogda kontrol et, katalogda yoksa uyumlu güncel kararlı sürümü öner.

## 1. Veri kaynağı (GitHub Pages, v2)

Taban adres: `https://turakahmet.github.io/yolcu/v2/` — kod içinde tek bir sabitte/yapılandırmada
tutulur (ileride alan adı değişebilir; dağınık string kullanma). Tüm dosyalar herkese açık ve
statiktir; uygulama Diyanet'e **asla** doğrudan bağlanmaz.

| Dosya                     | İçerik                             | Uygulamada kullanım                              |
|---------------------------|------------------------------------|--------------------------------------------------|
| `countries.json`          | Ülke listesi                       | Ülke seçimi, konumdan ülke eşleştirme            |
| `places/{countryId}.json` | Ülkenin eyaletleri ve şehirleri    | Eyalet → şehir seçimi, konumdan şehir eşleştirme |
| `cities.json`             | Tüm şehirlerin düz listesi (büyük) | İsteğe bağlı "şehir ara"                         |
| `times/{cityId}.json`     | Şehrin ~1 yıllık vakitleri         | **Room'a yazılan asıl veri**                     |
| `index.json`              | Botun durum dosyası                | Uygulama kullanmaz                               |

### `times/{cityId}.json`

```json
{
  "version": 2,
  "cityId": 9146,
  "name": "ADANA",
  "state": "ADANA",
  "stateId": 500,
  "country": "TÜRKİYE",
  "countryId": 2,
  "updatedAt": "2026-11-02T00:41:10+00:00",
  "validFrom": "2026-11-01",
  "validTo": "2027-11-01",
  "fields": [
    "date",
    "fajr",
    "sunrise",
    "dhuhr",
    "asr",
    "maghrib",
    "isha",
    "hijri",
    "qibla",
    "gmt"
  ],
  "days": [
    [
      "2026-11-01",
      "05:34",
      "06:55",
      "12:27",
      "15:22",
      "17:49",
      "19:05",
      "21.5.1448",
      "11:34",
      3
    ]
  ]
}
```

- `days` satırları **dizidir**; sütunları sabit indeksle değil, `fields` içindeki adlarla eşle (bot
  ileride sona alan ekleyebilir). Bilinmeyen alanları yok say; zorunlu alan eksikse dosyayı reddet.
- `fajr`=İmsak, `sunrise`=Güneş, `dhuhr`=Öğle, `asr`=İkindi, `maghrib`=Akşam, `isha`=Yatsı,
  `hijri`=gün.ay.yıl (ay adı uygulamada string kaynaklarından üretilir), `qibla`=Kıble saati.
- Vakitler `HH:mm` biçiminde **şehrin yerel saatidir**. Bir vakit `null` olabilir (kutup
  bölgelerinde yazın vakit oluşmaz).
- `gmt`: o gün o şehirde geçerli UTC farkı (saat). Yaz saatinde değişir ve kesirli olabilir (ör.
  3.5).
- Dosya dünden başlar (`validFrom` = bot çalıştığı günün bir gün öncesi).
- Bazı şehirler için Diyanet veri yayımlamaz; bu şehirlerin `times` dosyası yoktur → **HTTP 404**.

### `places/{countryId}.json` ve `countries.json`

```json
{
  "countryId": 13,
  "name": "ALMANYA",
  "code": "GERMANY",
  "states": [
    {
      "id": 800,
      "name": "BAYERN",
      "code": "BAYERN",
      "key": "BAYERN",
      "cities": [
        {
          "id": 11000,
          "name": "MÜNCHEN",
          "code": "MUENCHEN",
          "key": "MUNCHEN"
        }
      ]
    }
  ]
}
```

`countries.json` öğeleri:
`id, name (Türkçe), code (İngilizce, ör. GERMANY), key, codeKey, states, cities`.
`key`/`codeKey` normalize edilmiş adlardır (§8).

## 2. Room şeması

Veritabanı tek bir `RoomDatabase` sınıfından yönetilir (gereksiz singleton yerine uygulama düzeyinde
tek örnek; mevcut DI/uygulama sınıfı yapısına uy). `exportSchema = true`, şema JSON'ları
`app/schemas/` altında sürüm kontrolüne girer.

En az şu iki tablo (adları mevcut adlandırma düzenine uyarla):

**`prayer_day`** — günlük vakitler

- `cityId` (int), `date` (String `yyyy-MM-dd` veya epoch day; projede tek bir tarih temsili seç)
- `fajr, sunrise, dhuhr, asr, maghrib, isha` (String `HH:mm`, **nullable**)
- `hijri` (String, nullable), `qibla` (String, nullable)
- `gmtOffsetMinutes` (int; `gmt × 60`, kesirli farklar için dakika)
- Birincil anahtar `(cityId, date)`; `date` üzerinde indeks.

**`prayer_city`** — indirilen şehrin meta ve senkron durumu

- `cityId` (PK), `name, state, stateId, country, countryId`
- `schemaVersion`, `serverUpdatedAt`, `validFrom`, `validTo`, `syncedAt`, `etag` (nullable)

Kurallar:

- `allowMainThreadQueries` ve `fallbackToDestructiveMigration*` **kullanma**. Her şema
  değişikliğinde
  `Migration` yaz ve migration testi ekle.
- Seçili şehir (kullanıcı tercihi) mevcut merkezi tercih sınıfında (SharedPreferences sarmalayıcısı)
  kalabilir; vakit verisinin kendisi SharedPreferences'ta tutulmaz. Eski SharedPreferences'taki
  namaz vakti verisi varsa tek seferlik, geri alınabilir bir geçişle ele al ve kullanıcıya sor.

## 3. İndirme, doğrulama ve yazma

İndirme/yazma tek bir depo (repository) sınıfından yapılır; UI, widget ve bildirim katmanları
Retrofit'e veya DAO'ya doğrudan erişmez.

Bir `times/{cityId}.json` indirildiğinde, Room'a yazmadan önce **tamamı** doğrulanır:

1. `version == 2`; `cityId` istenen şehirle aynı.
2. `fields` gerekli tüm adları içeriyor.
3. `days` boş değil, tarihler artan sırada ve **ardışık** (boşluk/tekrar yok); en az 60 gün.
4. Vakit değerleri `HH:mm` veya `null`; `gmt` sayısal.
5. `validTo` bugünden ileride.

Doğrulama başarısızsa veya ağ hatası olursa **mevcut Room verisine dokunulmaz**; hata kaydedilir
(merkezi hata/log yapısı), sessizce yutulmaz.

Doğrulama başarılıysa **tek bir `@Transaction`** içinde:

- O şehrin günleri upsert edilir (`OnConflictStrategy.REPLACE` veya `@Upsert`).
- `prayer_city` güncellenir (`serverUpdatedAt`, `validFrom/To`, `syncedAt`, `etag`).
- O şehrin 2 günden eski satırları silinir.
- Kullanıcı şehir değiştirdiyse **yeni şehrin verisi başarıyla yazıldıktan sonra** eski şehrin
  verisi silinir (önce silip sonra indirme yapılmaz).

Senkronizasyon idempotenttir: aynı dosya iki kez yazılırsa sonuç değişmez.

## 4. Senkronizasyon zamanlaması (WorkManager)

- `PeriodicWorkRequest` (haftada bir), `NetworkType.CONNECTED` kısıtı, `enqueueUniquePeriodicWork`
  ile tek bir benzersiz iş; üstel backoff.
- Worker yalnızca gerektiğinde indirir: seçili şehrin Room'daki son tarihi bugünden **60 günden
  azsa**, şehir değiştiyse, şema sürümü değiştiyse veya veri hiç yoksa.
- `If-None-Match` ile `etag` gönderilir; **304** yanıtında yalnızca `syncedAt` güncellenir.
- Şehir değişiminde ve ilk kurulumda ayrıca `OneTimeWorkRequest` tetiklenir.
- Worker başarılı yazmadan sonra bildirim/alarm zamanlamasını ve widget'ları günceller.
- 429/5xx/ağ hatasında `Result.retry()`; 404'te (şehrin verisi yok) `Result.failure()` ve
  kullanıcıya bilgi (§6). Hiçbir durumda Diyanet API'si çağrılmaz.

## 5. Okuma, saat dilimi ve gösterim

- UI vakitleri DAO'dan `LiveData` (veya mevcut projede kullanılan gözlemlenebilir yapı) ile okur;
  Room sorguları ana thread'de çalışmaz.
- Vakitler **şehrin yerel saatiyle** gösterilir; cihazın saat dilimiyle yeniden hesaplanmaz.
- Bir vaktin kesin anı (geri sayım, bildirim, alarm): `LocalDate(date) + LocalTime(vakit)` →
  `ZoneOffset.ofTotalSeconds(gmtOffsetMinutes * 60)` ile `Instant`. Her gün kendi ofsetini kullanır;
  yaz saati geçişleri böylece doğru işlenir. (`java.time` minSdk 27'de kullanılabilir.)
- "Bugün" şehrin yerel tarihine göre belirlenir (Room'daki o günün ofsetiyle), cihaz tarihine göre
  değil.
- `null` vakit "—" olarak gösterilir; o vakit için bildirim/alarm kurulmaz; geri sayım bir sonraki
  dolu vakte geçer.
- Hicri ay adları ve vakit adları `strings.xml` (Türkçe + İngilizce) kaynaklarından gelir.

## 6. Durumlar ve kullanıcı mesajları

| Durum                          | Davranış                                                                          |
|--------------------------------|-----------------------------------------------------------------------------------|
| Room'da veri var, internet yok | Normal çalış; hiçbir uyarı gösterme                                               |
| Veri 60 günden az kaldı        | Arka planda sessizce yenile; kullanıcıya gösterme                                 |
| Veri bitti, internet yok       | Boş ekran yerine "Vakitler güncel değil, internete bağlanın" uyarısı              |
| İlk kurulum, internet yok      | Şehir seçimi ve indirme için internet gerektiğini açıkla                          |
| `times/{cityId}.json` 404      | "Bu şehir için Diyanet vakit yayımlamıyor" + aynı eyaletteki diğer şehirleri öner |
| Doğrulama/ağ hatası            | Mevcut veriyle devam; hata kaydı; arka planda yeniden dene                        |

Tüm metinler `strings.xml`'de (TR + EN).

## 7. Mevcut hesaplama mantığı

- Uygulama şu an vakitleri kendi hesaplıyor. Diyanet verisi **birincil** kaynaktır.
- Hesaplama mantığı yalnızca kullanıcı onayıyla ve açıkça ayrılmış bir **yedek** olarak kalabilir
  (ör. Room'da o gün için veri yoksa). Yedekten gelen vakitler UI'da işaretlenir; iki kaynak
  sessizce karıştırılmaz ve aynı gün için çelişkili iki değer üretilmez.
- Karar verilmeden hesaplama kodu silinmez.

## 8. Konumdan şehir eşleştirme

- Konum izni yoksa veya eşleşme bulunamazsa kullanıcıya ülke → eyalet/il → şehir/ilçe seçtirilir.
  Konum kontrolü projedeki merkezi konum sınıfından yapılır.
- `Geocoder` (arka plan thread'inde) **`Locale.ENGLISH`** ile çağrılır.
- Adlar botla **aynı kuralla** normalize edilir (tek bir yardımcı metotta):
    1. Türkçe büyük harfe çevir: `i → İ`, `ı → I`, ardından `toUpperCase(new Locale("tr"))`
    2. `Ç Ğ İ Ö Ş Ü Â Î Û` → `C G I O S U A I U`
    3. `Normalizer.normalize(s, Normalizer.Form.NFKD)`, ardından ASCII harf/rakam dışındaki her şeyi
       at Örnek: `Devrekâni` → `DEVREKANI`, `München` → `MUNCHEN`. Birim testle doğrula.
- Ülke: Geocoder'ın İngilizce ülke adının normalize hali `codeKey` ile, olmazsa `key` ile
  karşılaştırılır; uyuşmayanlar için küçük bir takma ad tablosu tutulur (ör. `TURKIYE` → `TURKEY`).
- Şehir: Türkiye'de ilçe genelde `subAdminArea`, il `adminArea`; diğer ülkelerde şehir çoğunlukla
  `locality`. İlgili ülkenin `places/{countryId}.json` dosyasında önce şehir `key`, sonra `code`
  normalize haliyle, sonra eyalet adıyla aranır.
- "Merkez" ilçeler, birden fazla aday veya eşleşmeme durumunda kullanıcıya seçtirilir; yanlış
  eşleşmeyi sessizce kabul etme.

## 9. Testler

Bireysel geliştirilen bir proje; testler az ve yüksek faydalı tutulur. Kapsamlı UI veya
enstrümantasyon testi yazma.

- Zorunlu birim testleri (`app/src/test`, saf Java):
    - Vakit + gmt → Instant dönüşümü ve "bugün" hesabı (yaz saati geçişi dahil)
    - JSON doğrulayıcı (eksik/tekrar eden gün, yanlış cityId, yanlış sürüm, null vakit)
    - Ad normalizasyonu (ör. Devrekâni → DEVREKANI, München → MUNCHEN)
- İlk Room şema değişikliğinde tek bir migration testi ekle.
- Diğer her şey elle doğrulanır: veri indir → uçak modu → ana ekran, widget, bildirim, yeniden
  başlatma, şehir değişimi. Görev sonunda bu elle kontrol listesini ver.