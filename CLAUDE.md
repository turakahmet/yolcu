# İslam Yolu — Genel Claude Code Kuralları

## Proje Genel Tanımı

- Proje "İslam Yolu" adında android studio Java + XML Views ile geliştirmiş android tabanlı bir
  mobil uygulama.
- Uygulama temelde İslami içerikleri, namaz vakitleri, islami bilgi yarışması, zekat hesaplama,
  kıble bulucu, İslami dijital resim albümü, dini günler gibi içerikleri sunmakta. Bir puan sistemi
  ve hesap sistemi var fakat local olarak tutulmakta.
- Uygulamanın backend yok sadece android studio üzerinden verileri retroift ile github page den
  verileri json olarak buradan okur. Veri değişikliği buradan yapılıyor.
- Uygulamada ücretsiz olarak google play üzerinden yayında herhangi bir ücretli içerik veya reklam
  yok. Bu nedenle uygulama ücretsiz kaynaklar kullanılarak tasarlanıyor. Bu nedenle bir backend yok.
- Uygulamada local hafıza için sharedpreference kullanılıyor fakat room db geçilmek isteniyor. Bu
  sayede daha fazla veri saklanabilecek yıllık namaz vakitleri gibi.
- Uygulamda ilerde firebase ilede google sign ve online zikir halkası gibi sunucu tabanlı çok işlem
  gerektirmeyecek spark modeli sürece geçebilir.
- Uygulama namaz vakitleri şuan kendi hesaplıyor fakat diyanetten de api alındı. Bu sisteme dahil
  edilerek güzel bir sistem kurulması ve kendi kendini yöneten bakım istemeyen bir tasarım yapılması
  bekleniyor.
- Amacımız önce var olan uygulamayı daha iyi hale getirmek sonra üstüne eklemeler yapmak.

## Proje sabitleri

- Android uygulaması yalnızca **Java + XML Views** kullanır. Kotlin/Compose'a geçme.
- `minSdk 27` korunur. Android 17 (API 37) ve sonraki Android sürümleriyle uyumluluk ayrıca test
  edilir; target/compile SDK ve gerçek Gradle ayarlarını incelemeden varsayma.
- Amaç: ücretsiz/düşük maliyetli, güvenli, hızlı, erişilebilir ve internet olmadan çalışabilen bir
  uygulama.
- Kullanıcı arayüzü metinleri, yorumlar ve proje dokümantasyonu Türkçe yazılır.
- Kod içinde kullanılan metinler string.xml kısmına yaz ve ingilizce kısmınıda yaz
- Namaz vakti, puan sistemi, bildirim, ayar, gezinme ve mevcut ekran davranışlarını incelemeden
  kaldırma veya değiştirme.
- Var olan sistemi daha iyi hale getirmek için sorular sorabilirsin

## Çalışma ve mimari

- Önce mevcut proje yapısını, çağrı noktalarını, manifesti, Version Catalog'u, testleri ve mevcut
  davranışı incele; dosya/sınıf adı uydurma.
- Temiz ve SOLID'e uygun Java yapısına **küçük, geriye uyumlu adımlarla** ilerle. Büyük refactor
  öncesi plan ve etki alanını çıkar; bir defada tüm uygulamayı yeniden yazma.
- Ortak ihtiyaçları merkezi sınıflardan yönet: sharedprefernce, tercihler, hatalar,
  eşleme/gösterimi, tarih-saat, ağ durumu ve veri erişimi. UI katmanı doğrudan altyapı detaylarına
  bağlanmasın.
- Ağ, veritabanı ve uzun işler ana/UI thread'inde çalışmasın. Boş `catch`, sessiz hata yutma, kopya
  kod ve gereksiz singleton kullanma.
- Mevcut `gradle/libs.versions.toml` ve Java/Gradle ayarlarına uy. Yeni kütüphane eklemeden önce
  mevcut bağımlılıkları kontrol et; gereksiz yükseltme yapma. Fakat daha yeni versiyonları uyumlu
  olduğu sürece kullan.
- Modüler yapıda süreci işlet fakat ortak kullanılan yapılarda. sharedprefernce, internet ağı
  kontrol, konum kontrol gibi ortak kontroller tek yerden yapılsın. Tekrar tekrar aynı metotları
  yazma.
- Uygulama android telefonlarda aynı esneklikte çalışmalı bir markada farklı diğerinde farklı
  çalışmamalı. UI tarafında görsellik responsive olmalı ekran yapısı bozulmamalı. Yeni katlanabilir
  telefonlarda bölünmüş ekranlarda da güzel çalışmalı.
- Widget kısmında farklı UI yazılımı kullanan markalarda hepsinde ortak çalışabilmeli.
- Bildirim sistemi yine her marka cihaz ui da güzel şekilde çalışabilmeli.

## Namaz vakitleri ve çevrim dışı kullanım

- Room, uygulamadaki namaz vakitlerinin yerel kaynağıdır. UI, widget ve bildirimler vakitleri
  **yalnızca Room'dan** okur; ağ senkronizasyonu yalnızca Room'u günceller. İnternet yokken uygulama
  Room'daki veriyle tam çalışır.
- Akış: Diyanet API → zamanlanmış GitHub Actions botu (ayrı `yolcu` reposu) → GitHub Pages'teki
  sürümlü (`v2`) şehir başına **1 yıllık** JSON dosyaları → Android (Retrofit) → Room.
- Veri Diyanet'in sunduğu **tüm ülkeleri** kapsar (~208 ülke, ~8.600 şehir). Hiyerarşi:
  Ülke → Eyalet (Türkiye'de il) → Şehir (Türkiye'de ilçe). Kullanıcı yurt dışında da konumdan veya
  elle seçimle kendi şehrini kullanabilmeli.
- Kullanıcı yalnızca seçtiği şehrin verisini Room'da tutar (~365 gün ileri). İlk indirme internet
  ister; sonrasında vakitler çevrim dışı okunur. Arka plan senkronizasyonu veri azaldığında sessizce
  yeniler.
- Senkronizasyon idempotent olmalı; şema sürümü, `cityId`, tarih sürekliliği ve güncelleme zamanı
  doğrulanmalı. Hatalı/eksik indirme mevcut geçerli veriyi silmemeli.
- Room şema değişikliklerinde migration yaz; kullanıcı verisini silecek destructive migration
  kullanma. Mevcut namaz vakti hesaplama mantığını doğrulamadan ikincil ve çelişkili bir kaynak
  oluşturma.
- Veri biçimi, Room şeması, senkronizasyon ve saat dilimi kuralları:
  `.claude/rules/namaz-vakitleri-room.md`.

## Güvenlik ve maliyet

- Diyanet kullanıcı adı/şifresi, access/refresh token APK'ya, uygulama kaynak koduna, loglara veya
  herkese açık JSON'a girmez. Yalnızca GitHub Actions Secrets veya uygun sunucu tarafı secret
  deposunda bulunur.
- Mobil uygulama Diyanet'e doğrudan bağlanmaz; yayınlanan kamuya açık vakit JSON'larını okur. Kota
  sınırını aşma/etrafından dolanma yoktur; kota veya `429` hatasında kontrollü duraklama ve kayıt
  gerekir.
- Firebase/ücretli hizmet/SDK eklemeden önce gerekçeyi, ücretsiz alternatifleri ve maliyeti açıkla;
  onay olmadan ekleme.
- Tersine mühendilikle ilgili güvenlik açıklarını kontrol et.

## Doğrulama ve teslim

- İlgili path-scoped kuralları `.claude/rules/` altından kullan. Ayrıntıyı yalnızca ilgili
  dosya/konu için aç.
- Uygun olduğunda `gradlew.bat test` ve `gradlew.bat assembleDebug` çalıştır. Çalıştırılmayan testi
  başarılı gösterme.
- Her iş sonunda Türkçe özet ver: değişen dosyalar, davranış/şema etkisi, test sonuçları ve kalan
  riskler.