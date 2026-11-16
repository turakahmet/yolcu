# Namaz Vakitleri Veri Kaynağı — Entegrasyon Rehberi

Bu doküman, İslam Yolu uygulamasının kullandığı **GitHub Pages üzerindeki statik namaz vakti
verisini** başka bir uygulamada (Android, iOS, web, masaüstü) kullanmak için gereken her şeyi anlatır.

> Doğrulama: Dosya yapıları ve HTTP davranışı 11 Kasım 2026'da canlı adreslerden kontrol edilmiştir.

---

## 1. Genel bakış

```
Diyanet API ──► GitHub Actions botu (yolcu reposu) ──► GitHub Pages (v2 JSON) ──► Uygulamalar
                 (kimlik bilgileri yalnızca           (herkese açık, statik)        (yalnızca okur)
                  GitHub Secrets'ta)
```

- Uygulamalar **Diyanet'e doğrudan bağlanmaz**; yalnızca yayınlanmış statik JSON dosyalarını okur.
  Böylece istemcide kullanıcı adı, şifre veya token bulunmaz, Diyanet kotası tüketilmez.
- Veri Diyanet'in yayımladığı tüm ülkeleri kapsar: **208 ülke, 8.604 şehir**.
- Her şehir için yaklaşık **1 yıllık** (366 gün) vakit tek dosyada gelir. İlk indirmeden sonra
  uygulama tamamen çevrim dışı çalışabilir.
- Hiyerarşi: **Ülke → Eyalet** (Türkiye'de il) **→ Şehir** (Türkiye'de ilçe).

> Veri Diyanet İşleri Başkanlığı kaynaklıdır. Başka bir uygulamada (özellikle yayımlanan bir
> üründe) kullanmadan önce kaynak gösterme ve kullanım koşullarını kurumun hukuk birimiyle
> netleştirin; bu doküman hukuki görüş değildir.

---

## 2. Taban adres ve dosyalar

**Taban adres:** `https://turakahmet.github.io/yolcu/v2/`

Bu adresi kod içinde **tek bir sabitte / yapılandırmada** tutun (ileride alan adı değişebilir).
Yollar göreli yazılmalıdır: Retrofit'te `@GET("countries.json")` (başında `/` olmadan), aksi halde
`/yolcu/v2/` kısmı kaybolur.

| Dosya                     | Boyut (yaklaşık) | İçerik                                   | Ne zaman kullanılır                         |
|---------------------------|------------------|------------------------------------------|---------------------------------------------|
| `countries.json`          | 22 KB            | Ülke listesi                             | Ülke seçimi, konumdan ülke eşleştirme       |
| `places/{countryId}.json` | 1–60 KB          | Ülkenin eyaletleri ve şehirleri          | Eyalet → şehir seçimi, konumdan şehir bulma |
| `cities.json`             | 505 KB           | Tüm şehirlerin düz listesi               | İsteğe bağlı "şehir ara" özelliği           |
| `times/{cityId}.json`     | ~31 KB           | Şehrin ~1 yıllık vakitleri               | **Asıl veri**; yerel veritabanına yazılır   |
| `index.json`              | ~1 MB            | Botun durum dosyası (şehir başına özet)  | İstemci kullanmaz (izleme/teşhis amaçlı)    |

### HTTP davranışı

| Durum                                   | Yanıt                                                           |
|-----------------------------------------|-----------------------------------------------------------------|
| Normal                                  | `200`, `Content-Type: application/json; charset=utf-8`          |
| Önbellek                                | `Cache-Control: max-age=600` (10 dk), her yanıtta `ETag` var    |
| `If-None-Match: <ETag>` ile tekrar      | Dosya değişmediyse **`304 Not Modified`** (gövde yok)           |
| Diyanet'in veri yayımlamadığı şehir     | `times/{cityId}.json` → **`404`**                               |
| Geçici sunucu sorunu                    | `5xx` görülebilir → üstel bekleme ile yeniden deneyin           |

---

## 3. Dosya biçimleri

Tüm dosyalarda kök nesnede `"version": 2` bulunur. Metinler UTF-8'dir; Türkçe karakterler
(`TÜRKİYE`, `ALADAĞ`) korunur.

### 3.1 `countries.json`

```json
{
  "version": 2,
  "generatedAt": "2026-11-03T08:45:13+00:00",
  "count": 208,
  "countries": [
    { "id": 13, "name": "ALMANYA", "code": "GERMANY", "key": "ALMANYA",
      "codeKey": "GERMANY", "states": 16, "cities": 1241 }
  ]
}
```

| Alan      | Tür    | Açıklama                                                       |
|-----------|--------|----------------------------------------------------------------|
| `id`      | int    | Ülke kimliği (`places/{id}.json` için)                         |
| `name`    | string | Türkçe ad (ör. `ALMANYA`)                                      |
| `code`    | string | İngilizce ad (ör. `GERMANY`)                                   |
| `key`     | string | `name`'in normalize hali (§6)                                  |
| `codeKey` | string | `code`'un normalize hali (§6)                                  |
| `states`  | int    | Eyalet **sayısı** (liste değil)                                |
| `cities`  | int    | Şehir **sayısı** (liste değil)                                 |

### 3.2 `places/{countryId}.json`

```json
{
  "version": 2,
  "generatedAt": "2026-11-03T08:45:13+00:00",
  "countryId": 2,
  "name": "TÜRKİYE",
  "code": "TÜRKİYE",
  "states": [
    {
      "id": 500, "name": "ADANA", "code": "ADANA", "key": "ADANA",
      "cities": [
        { "id": 9146, "name": "ADANA",  "code": "ADANA",  "key": "ADANA" },
        { "id": 9147, "name": "ALADAĞ", "code": "ALADAG", "key": "ALADAG" }
      ]
    }
  ]
}
```

Şehir `id` değeri, vakit dosyasının adıdır: `times/9146.json`.

### 3.3 `cities.json` (düz liste, sütunlu)

```json
{
  "version": 2,
  "generatedAt": "2026-11-03T08:45:13+00:00",
  "countryFilter": [],
  "count": 8604,
  "fields": ["id", "name", "key", "stateId", "state", "countryId", "country"],
  "rows": [
    [8573, "AUBURN", "AUBURN", 581, "ALABAMA", 33, "ABD"]
  ]
}
```

`rows` satırları **dizidir**; sütunları `fields` içindeki adlarla eşleyin (§3.4'teki kural).

### 3.4 `times/{cityId}.json` — asıl veri

```json
{
  "version": 2,
  "cityId": 9146,
  "name": "ADANA",
  "state": "ADANA",
  "stateId": 500,
  "country": "TÜRKİYE",
  "countryId": 2,
  "updatedAt": "2026-11-02T13:38:13+00:00",
  "validFrom": "2026-11-01",
  "validTo": "2027-11-01",
  "fields": ["date", "fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha", "hijri", "qibla", "gmt"],
  "days": [
    ["2026-11-01", "05:35", "06:55", "12:27", "15:23", "17:49", "19:05", "21.5.1448", "11:32", 3]
  ]
}
```

**Kök alanlar**

| Alan                    | Zorunlu | Açıklama                                                     |
|-------------------------|---------|--------------------------------------------------------------|
| `version`               | evet    | Şema sürümü; şu an `2`                                        |
| `cityId`                | evet    | İstenen şehirle aynı olmalı                                   |
| `validFrom` / `validTo` | evet    | `yyyy-MM-dd`; dosyanın kapsadığı aralık                       |
| `fields`, `days`        | evet    | Sütun adları ve günlük satırlar                               |
| `updatedAt`             | hayır   | Botun dosyayı ürettiği an (ISO-8601, UTC)                     |
| `name`, `state`, `stateId`, `country`, `countryId` | hayır | Şehir bilgisi (gösterim için)       |

**Sütunlar (`fields`)**

| Ad        | Anlamı        | Biçim                     | Not                                         |
|-----------|---------------|---------------------------|---------------------------------------------|
| `date`    | Tarih         | `yyyy-MM-dd`              | Zorunlu, boş olamaz                         |
| `fajr`    | İmsak         | `HH:mm` veya `null`       | Zorunlu sütun                               |
| `sunrise` | Güneş         | `HH:mm` veya `null`       | Zorunlu sütun                               |
| `dhuhr`   | Öğle          | `HH:mm` veya `null`       | Zorunlu sütun                               |
| `asr`     | İkindi        | `HH:mm` veya `null`       | Zorunlu sütun                               |
| `maghrib` | Akşam         | `HH:mm` veya `null`       | Zorunlu sütun                               |
| `isha`    | Yatsı         | `HH:mm` veya `null`       | Zorunlu sütun                               |
| `hijri`   | Hicri tarih   | `gün.ay.yıl` (`21.5.1448`)| İsteğe bağlı; ay adı uygulamada üretilir    |
| `qibla`   | Kıble saati   | `HH:mm`                   | İsteğe bağlı                                |
| `gmt`     | UTC farkı     | sayı (saat, kesirli olabilir) | Zorunlu; ör. `3`, `-5`, `3.5`, `5.75`    |

Önemli kurallar:

1. **Sütunları sabit indeksle okumayın.** `fields` dizisinden ad → indeks haritası çıkarın. Bot
   ileride sona yeni alan ekleyebilir; bilinmeyen alanları yok sayın. Zorunlu bir alan eksikse
   dosyayı reddedin.
2. Vakitler **şehrin yerel saatidir**; cihazın saat dilimine göre yeniden hesaplamayın.
3. Bir vakit **`null` olabilir** (kutup bölgelerinde yazın bazı vakitler oluşmaz). Ekranda "—"
   gösterin, o vakit için alarm/bildirim kurmayın.
4. `gmt` o gün o şehirde geçerli UTC farkıdır ve **yaz saatinde değişir**. Dakikaya çevirip
   saklayın: `Math.round(gmt * 60)`.
5. Dosya **dünden başlar** (`validFrom` = botun çalıştığı günün bir gün öncesi) ve ~366 gün sürer.

---

## 4. İndirme sonrası doğrulama (yazmadan önce)

Dosyayı yerel veritabanına yazmadan önce **tamamını** doğrulayın; tek hata varsa hiçbir şey
yazmayın ve **mevcut geçerli veriye dokunmayın**.

| Kontrol                                                        | Hata kodu (İslam Yolu)     |
|----------------------------------------------------------------|----------------------------|
| `version == 2`                                                 | `UNSUPPORTED_VERSION`      |
| `cityId` istenen şehirle aynı                                  | `CITY_ID_MISMATCH`         |
| `days` boş değil                                               | `NO_DAYS`                  |
| En az 60 gün                                                   | `TOO_FEW_DAYS`             |
| Tarihler geçerli `yyyy-MM-dd`                                  | `INVALID_DATE`             |
| Tarihler artan ve **ardışık** (boşluk/tekrar yok)              | `DATES_NOT_CONSECUTIVE`    |
| Vakitler `HH:mm` (`00:00–23:59`) veya `null`                   | `INVALID_TIME`             |
| `gmt` sayısal ve ±18 saat içinde                               | `INVALID_GMT_OFFSET`       |
| `validFrom` / `validTo` geçerli tarih                          | `INVALID_VALIDITY`         |
| `validTo` bugünden ileride                                     | `EXPIRED`                  |

Vakit deseni: `^([01]\d|2[0-3]):[0-5]\d$`

---

## 5. Saat dilimi ve "bugün" hesabı

Bir vaktin kesin anı (geri sayım, bildirim, alarm) için cihaz saat dilimini **kullanmayın**;
o günün kendi `gmt` değerini kullanın. Yaz saati geçişleri böylece doğru işlenir.

```java
// Java 8+ / Android API 26+ (java.time)
Instant prayerInstant(LocalDate date, String hhmm, int gmtOffsetMinutes) {
    return LocalDateTime.of(date, LocalTime.parse(hhmm))
            .toInstant(ZoneOffset.ofTotalSeconds(gmtOffsetMinutes * 60));
}
```

**Şehrin "bugün"ü**, cihazın tarihi değil, şehrin yerel tarihidir (kullanıcı farklı saat
diliminde olabilir). UTC farkı en fazla ±18 saat olduğundan yerel tarih, UTC tarihinin bir gün
öncesi, kendisi veya sonrasıdır. Bir gün, **önceki günün** ofsetiyle gece yarısında başlar ve
**kendi** ofsetiyle ertesi gece yarısında biter; yaz saati geçiş günleri (23/25 saat) böylece
boşluksuz ve çakışmasız kalır.

```java
LocalDate cityToday(Instant now, Map<LocalDate, Integer> offsetMinutesByDate) {
    LocalDate utcDate = now.atOffset(ZoneOffset.UTC).toLocalDate();
    for (int shift = 1; shift >= -1; shift--) {
        LocalDate candidate = utcDate.plusDays(shift);
        Integer offset = offsetMinutesByDate.get(candidate);
        if (offset == null) continue;
        Integer previous = offsetMinutesByDate.get(candidate.minusDays(1));
        int startOffset = previous != null ? previous : offset;

        Instant start = candidate.atStartOfDay().toInstant(ZoneOffset.ofTotalSeconds(startOffset * 60));
        Instant end = candidate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.ofTotalSeconds(offset * 60));
        if (!now.isBefore(start) && now.isBefore(end)) return candidate;
    }
    return null; // veri bu anı kapsamıyor (bitmiş/eksik)
}
```

**Hicri tarih gösterimi:** `"21.5.1448"` → ay numarasını kendi dil kaynaklarınızdaki ay
adlarıyla eşleyin (1 = Muharrem … 12 = Zilhicce) → `21 Cemaziyelevvel 1448`.

---

## 6. Konumdan şehir bulma ve ad normalizasyonu

`key` / `codeKey` alanları botun normalize ettiği adlardır. Kullanıcının konumundan gelen adı
**aynı kuralla** normalize edip karşılaştırın:

1. Türkçe büyük harfe çevir: `i → İ`, `ı → I`, sonra `toUpperCase(tr)`
2. `Ç Ğ İ Ö Ş Ü Â Î Û` → `C G I O S U A I U`
3. Unicode `NFKD` normalizasyonu, ardından `A–Z` ve `0–9` dışındaki her şeyi at

Örnekler: `Devrekâni → DEVREKANI`, `München → MUNCHEN`, `Gulf Shores → GULFSHORES`.

```java
static String normalize(String name) {
    if (name == null) return "";
    String upper = name.replace('i', 'İ').replace('ı', 'I').toUpperCase(Locale.forLanguageTag("tr"));
    String tr = "ÇĞİÖŞÜÂÎÛ", ascii = "CGIOSUAIU";
    StringBuilder sb = new StringBuilder(upper.length());
    for (char c : upper.toCharArray()) {
        int i = tr.indexOf(c);
        sb.append(i >= 0 ? ascii.charAt(i) : c);
    }
    return Normalizer.normalize(sb, Normalizer.Form.NFKD).replaceAll("[^A-Z0-9]", "");
}
```

Eşleştirme sırası (Android `Geocoder` için, `Locale.ENGLISH` ile ve arka planda çağırın):

- **Ülke:** İngilizce ülke adının normalize hali → önce `codeKey`, olmazsa `key`. Uyuşmayanlar
  için küçük bir takma ad tablosu tutun (ör. `TURKIYE → TURKEY`).
- **Şehir:** Türkiye'de ilçe genelde `subAdminArea`, il `adminArea`; diğer ülkelerde şehir
  çoğunlukla `locality`. İlgili `places/{countryId}.json` içinde önce şehir `key`, sonra `code`
  normalize hali, sonra eyalet adıyla arayın.
- Birden fazla aday, "Merkez" ilçeler veya eşleşmeme durumunda **kullanıcıya seçtirin**; yanlış
  eşleşmeyi sessizce kabul etmeyin.

---

## 7. Senkronizasyon önerisi

İslam Yolu'nun uyguladığı ve önerilen akış:

1. **İlk kurulum / şehir değişimi:** `times/{cityId}.json` indir → doğrula → tek işlemde (transaction)
   yerel veritabanına yaz. Eski şehrin verisini **ancak yeni şehir başarıyla yazıldıktan sonra** sil.
2. **Periyodik iş:** Haftada bir, yalnızca internet varken çalışsın. İndirme yalnızca şu
   durumlarda yapılır:
   - Yerelde veri yok veya şehir değişti,
   - Kayıtlı şema sürümü 2 değil,
   - Yereldeki son gün bugünden **60 günden az** ileride.
3. Her istekte önceki `ETag`'i `If-None-Match` başlığıyla gönderin; **304** gelirse yalnızca
   "son senkron zamanı"nı güncelleyin.
4. Yazma işlemi **idempotent** olmalı (aynı dosya iki kez yazılırsa sonuç değişmez): birincil
   anahtar `(cityId, date)` ile upsert; 2 günden eski satırları temizleyin.
5. Hata yönetimi:

| Durum                       | Davranış                                                                 |
|-----------------------------|--------------------------------------------------------------------------|
| `404`                       | Yeniden deneme yok; "Bu şehir için Diyanet vakit yayımlamıyor" + aynı eyaletten öneri |
| `429` / `5xx` / ağ hatası   | Üstel bekleme ile yeniden dene; mevcut veriyle devam                      |
| Doğrulama hatası            | Yazma; hatayı kaydet; mevcut veriyle devam                                |
| Veri bitti, internet yok    | "Vakitler güncel değil, internete bağlanın" uyarısı                       |

### Önerilen yerel şema (Room / SQLite)

```sql
CREATE TABLE prayer_day (
  cityId INTEGER NOT NULL,
  date TEXT NOT NULL,                 -- yyyy-MM-dd
  fajr TEXT, sunrise TEXT, dhuhr TEXT, asr TEXT, maghrib TEXT, isha TEXT,  -- HH:mm, NULL olabilir
  hijri TEXT, qibla TEXT,
  gmtOffsetMinutes INTEGER NOT NULL,
  PRIMARY KEY (cityId, date)
);
CREATE INDEX index_prayer_day_date ON prayer_day(date);

CREATE TABLE prayer_city (
  cityId INTEGER PRIMARY KEY NOT NULL,
  name TEXT, state TEXT, stateId INTEGER, country TEXT, countryId INTEGER,
  schemaVersion INTEGER NOT NULL,
  serverUpdatedAt TEXT, validFrom TEXT, validTo TEXT,
  syncedAt INTEGER NOT NULL,          -- epoch ms
  etag TEXT
);
```

---

## 8. Hızlı başlangıç örnekleri

### Android (Retrofit + OkHttp)

```java
public interface PrayerTimesApi {
    @GET("countries.json")
    Call<CountriesResponse> getCountries();

    @GET("places/{countryId}.json")
    Call<PlacesResponse> getPlaces(@Path("countryId") int countryId);

    /** Ham gövde: sütunlar "fields" adlarıyla ayrıştırılır */
    @GET("times/{cityId}.json")
    Call<ResponseBody> getTimes(@Path("cityId") int cityId,
                                @Nullable @Header("If-None-Match") String etag);
}

Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("https://turakahmet.github.io/yolcu/v2/")   // tek sabitte tutun
        .addConverterFactory(GsonConverterFactory.create())
        .build();
```

Ağ çağrılarını ana thread dışında (WorkManager, executor) yapın.

### Sütunları adla okuma (Gson)

```java
JsonObject root = JsonParser.parseString(json).getAsJsonObject();
Map<String, Integer> col = new HashMap<>();
JsonArray fields = root.getAsJsonArray("fields");
for (int i = 0; i < fields.size(); i++) col.put(fields.get(i).getAsString(), i);

for (JsonElement e : root.getAsJsonArray("days")) {
    JsonArray row = e.getAsJsonArray();
    String date = row.get(col.get("date")).getAsString();
    JsonElement fajr = row.get(col.get("fajr"));
    String imsak = fajr.isJsonNull() ? null : fajr.getAsString();
    int gmtMinutes = (int) Math.round(row.get(col.get("gmt")).getAsDouble() * 60);
    // ...
}
```

### Web / JavaScript

```js
const BASE = "https://turakahmet.github.io/yolcu/v2/";

async function loadTimes(cityId, etag) {
  const res = await fetch(`${BASE}times/${cityId}.json`, {
    headers: etag ? { "If-None-Match": etag } : {},
  });
  if (res.status === 304) return { notModified: true };
  if (res.status === 404) throw new Error("Bu şehir için veri yayımlanmıyor");
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const data = await res.json();
  const col = Object.fromEntries(data.fields.map((name, i) => [name, i]));
  const days = data.days.map((r) => ({
    date: r[col.date],
    fajr: r[col.fajr], sunrise: r[col.sunrise], dhuhr: r[col.dhuhr],
    asr: r[col.asr], maghrib: r[col.maghrib], isha: r[col.isha],
    hijri: col.hijri !== undefined ? r[col.hijri] : null,
    gmtOffsetMinutes: Math.round(r[col.gmt] * 60),
  }));
  return { etag: res.headers.get("ETag"), days };
}
```

---

## 9. Güvenlik ve sınırlar

- İstemciye Diyanet kullanıcı adı, şifre veya token **koymayın**; bunlar yalnızca botun
  GitHub Actions Secrets'ında bulunur.
- Veri statiktir ve herkese açıktır; GitHub Pages'in genel kullanım sınırlarına tabidir. Her
  açılışta indirme yapmayın: yerelde saklayın, `ETag` kullanın, yalnızca gerektiğinde yenileyin.
- `index.json` (~1 MB) istemci için gerekli değildir; uygulamada indirmeyin.

---

## 10. İslam Yolu'ndaki referans kodlar

Aynı mantığın çalışan Java karşılıkları (`app/src/main/java/com/islamyolu/app/`):

| Konu                         | Sınıf                                          |
|------------------------------|------------------------------------------------|
| Taban adres                  | `Utils/APIUrl.PRAYER_TIMES_BASE_URL`           |
| Retrofit arayüzü             | `prayertimes/remote/PrayerTimesApi`            |
| JSON ayrıştırma (`fields`)   | `prayertimes/PrayerTimesParser`                |
| Doğrulama                    | `prayertimes/PrayerTimesValidator`             |
| Ad normalizasyonu            | `prayertimes/PlaceNameNormalizer`              |
| Konumdan şehir eşleştirme    | `prayertimes/location/PlaceMatcher`            |
| Saat dilimi / "bugün"        | `prayertimes/PrayerClock`, `PrayerTimeline`    |
| Senkron kararı (60 gün)      | `prayertimes/sync/PrayerSyncPolicy`            |
| Arka plan senkronu           | `prayertimes/sync/PrayerTimesSyncWorker`       |
| Hicri tarih gösterimi        | `prayertimes/PrayerDisplay.hijri`              |
| Room tabloları               | `prayertimes/db/PrayerDayEntity`, `PrayerCityEntity` |
