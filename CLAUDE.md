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
- Kod yazarken olabildiğince güncel kütüphaneler kullan deprecated metotlardan kaçın var olanları
  düzenle gerektiğinde yeni versiyona güncelle
- Arayüz tasarımları

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

# İSLAM YOLU – RESPONSIVE UI, DARK MODE VE CİHAZ UYUMLULUK KURALLARI

Bu kurallar Android uygulamasındaki bütün mevcut ve yeni ekranlar için zorunludur.

Amaç; uygulamanın farklı Android cihazlarında, ekran boyutlarında, yönlerde, pencere boyutlarında ve
tema modlarında bozulmadan çalışmasını sağlamaktır.

## 1. Genel Responsive Tasarım

- Sabit ekran genişliği ve yüksekliği kullanma.
- Cihaz modeline veya üreticisine özel tasarım kodu yazma.
- Ekran boyutunu varsayarak XML oluşturma.
- Ölçülerde dp, yazı boyutlarında sp kullan.
- Ekran genişliğine göre bileşenlerin genişlemesini sağla.
- İçerik uzunluğuna göre yüksekliklerin büyümesine izin ver.
- Metinlerin kesilmesini veya üst üste binmesini engelle.
- Uzun başlıklarda gerektiğinde satır kaydır.
- Gereksiz tek satır zorlaması yapma.
- Metinleri küçülterek taşma sorununu gizleme.
- Yatay ve dikey boşlukları tutarlı yönet.
- Ekran içeriği sığmıyorsa uygun ScrollView veya RecyclerView kullan.
- Aynı ekranda gereksiz iç içe kaydırma alanları oluşturma.

## 2. Ekran Boyutu ve Pencere Uyumluluğu

Aşağıdaki ekran senaryolarını destekle:

- Küçük ekranlı telefonlar.
- Standart telefonlar.
- Büyük ekranlı telefonlar.
- Uzun ekran oranları.
- Geniş ekranlı cihazlar.
- Tabletler.
- Katlanabilir telefonlar.
- Yatay ekran kullanımı.
- Android bölünmüş ekran (Split Screen).
- Serbest pencere boyutlandırma destekleyen cihazlar.

Responsive kararlarını yalnızca cihaz modeline göre verme.

Gerçek ekran/pencere genişliğine ve kullanılabilir alana göre düzen belirle.

Android Window Size Class yaklaşımını değerlendir.

Önerilen pencere genişliği sınıfları:

- Compact: 600dp altı.
- Medium: 600dp – 839dp.
- Expanded: 840dp ve üzeri.

Bu değerleri fiziksel cihaz ekranı yerine uygulamanın kullanılabilir pencere genişliği üzerinden
değerlendir.

Compact:

- Tek sütunlu düzen.
- Tam genişliğe yakın kartlar.
- Dikey kaydırma.

Medium:

- İçeriğe uygunsa iki sütunlu düzen.
- Gereksiz genişleyen kartlardan kaçın.
- Dengeli kenar boşlukları.

Expanded:

- Geniş ekranlarda iki sütun veya uygun çok sütunlu düzen.
- Ana içerik için makul maksimum genişlik.
- Gerekirse Navigation Rail veya yan panel.

Sırf ekran geniş diye bütün ekranı yan yana dizme. İçeriğin okunabilirliğini önceliklendir.

## 3. Bölünmüş Ekran Uyumluluğu

Android Split Screen kullanımını destekle.

- Activity yeniden boyutlandırılabilir olmalı.
- Sabit genişlik ve yükseklik kullanma.
- Bölünmüş ekranda kullanılabilir pencere ölçülerini yeniden değerlendir.
- Pencere boyutu değiştiğinde layout doğru yeniden düzenlenmeli.
- Gereksiz Activity yeniden oluşturulmasını önle.
- Ekran durumu ve kullanıcı seçimleri korunmalı.
- Form, dropdown, dialog ve ses oynatıcı taşmamalı.
- Bölünmüş ekranda kartlar pencere genişliğine uyum sağlamalı.
- Dar alanda iki sütunlu yapı tek sütuna dönmeli.
- Dialog ve popup pencereleri kullanılabilir alan içerisinde kalmalı.

Split Screen kullanımını gerçek cihazda test et.

## 4. Ekran Yönü

Portrait ve Landscape yönlerini destekle.

Portrait:

- Öncelikle tek sütunlu tasarım.
- Kartlar dikey sıralanmalı.

Landscape:

- İçeriğe göre yatay yerleşim değerlendirilmeli.
- Gereksiz geniş boşluk oluşmamalı.
- Yüksekliği sınırlı ekranlarda dikey kaydırma çalışmalı.
- Butonlar ve alt kontroller erişilebilir kalmalı.
- Sabit alt alanlar içeriğin üzerini kapatmamalı.

Yalnızca dikey ekran için tasarım yapma.

## 5. Dark Mode ve Light Mode

Uygulama genelinde açık ve koyu tema desteği zorunludur.

Açık tema:

- Arka plan: #F8F6EE
- Surface: #FFFFFF
- Primary: #0F3D2E
- Secondary: #2E7D32
- Accent: #A5D66A
- Ana metin: #26332C
- İkincil metin: #6B7280
- Border: #E5E7EB

Koyu tema için ayrı değerler oluştur:

- Background: #121A16
- Surface: #202B25
- Surface Variant: #29382F
- Primary: #A5D66A
- Secondary: #81C784
- Accent: #B8E986
- Ana metin: #F1F5F2
- İkincil metin: #B0BDB4
- Border: #39483F

Renkler merkezi olarak yönetilmeli.

Aşağıdaki dosyaları kullan:

- res/values/colors.xml
- res/values-night/colors.xml
- res/values/styles.xml
- res/values-night/styles.xml

Tema değişiminde aşağıdaki bileşenlerin tamamını kontrol et:

- Toolbar.
- CardView ve MaterialCardView.
- Button ve MaterialButton.
- Switch ve MaterialSwitch.
- Spinner ve Dropdown.
- TextInputLayout.
- Dialog ve BottomSheet.
- PopupMenu.
- SeekBar.
- TabLayout.
- ProgressBar.
- RecyclerView.
- Navigation bileşenleri.
- Status bar ve Navigation bar.
- Ses oynatıcı.
- Bildirim önizleme alanları.

Tema değişimini yalnızca ekran arka planına uygulama.

## 6. Tema Seçim Sistemi

Uygulamada üç seçenek destekle:

- Sistem Varsayılanı.
- Açık Tema.
- Koyu Tema.

Kullanıcının seçimi kalıcı saklanmalı.

Android sistem teması değiştiğinde Sistem Varsayılanı seçilmişse uygulama otomatik uyum sağlamalı.

Tema seçimi merkezi bir servis üzerinden yönetilmeli.

Önerilen sorumluluk:
AppThemeManager

Tema değişikliğinde gerekli Activity ve bileşenlerin doğru yenilenmesini sağla.

## 7. XML Layout Kuralları

- ConstraintLayout, LinearLayout ve FrameLayout'u ihtiyaca göre kullan.
- Gereksiz iç içe LinearLayout oluşturma.
- ConstraintLayout kullanırken bütün gerekli constraint bağlantılarını tanımla.
- Kullanılmayan veya çakışan constraint bırakma.
- layout_width ve layout_height değerlerini içeriğe uygun belirle.
- wrap_content ve match_parent değerlerini doğru kullan.
- Aşırı nested layout yapısından kaçın.
- Weight kullanımını gerektiğinde ve kontrollü uygula.
- Ekranın tamamını tek bir sabit boyutlu layout içine sıkıştırma.
- RecyclerView öğelerini yeniden kullanılabilir XML bileşenleri olarak oluştur.
- Kart genişliklerini cihaz modeline göre sabitleme.
- Minimum dokunma alanı 48dp olmalı.

## 8. Metin ve Erişilebilirlik

- Bütün yazılar strings.xml üzerinden yönetilmeli.
- Metinlerde sabit genişlik kullanma.
- Büyük yazı boyutlarında taşma olmamalı.
- Font scaling desteği korunmalı.
- textSize değerlerinde sp kullan.
- Gereksiz autosize uygulama.
- Önemli metinleri ellipsize ile gizleme.
- Gerektiğinde çok satırlı görünüm kullan.
- İkonlara anlamlı contentDescription ekle.
- Görsel kontrastı erişilebilir seviyede tut.
- Dokunma alanları yeterli büyüklükte olmalı.

## 9. Sistem Çubukları ve Ekran Güvenli Alanları

- Status bar ve Navigation bar renkleri temaya uygun olmalı.
- Sistem çubuklarının ikonları açık/koyu temaya göre doğru görünmeli.
- Kamera deliği, çentik ve edge-to-edge alanlarını dikkate al.
- Display cutout alanlarına içerik yerleştirme.
- WindowInsets kullanarak güvenli alanları yönet.
- Android 15 ve üzerindeki edge-to-edge davranışını kontrol et.
- Klavye açıldığında alt içeriklerin kapanmasını önle.
- Navigation bar ile uygulama alt bileşenlerinin çakışmasını engelle.

## 10. Ekran Durumu ve Yaşam Döngüsü

- Ekran döndürme durumunda kullanıcı seçimleri kaybolmamalı.
- Activity yeniden oluşturulduğunda UI durumu korunmalı.
- Split Screen pencere boyutu değişiminde içerik doğru yeniden yerleşmeli.
- Ses oynatımı ekran kapandığında uygun şekilde durdurulmalı.
- Dialog ve popup durumları güvenli yönetilmeli.
- Uzun süren işlemler UI thread üzerinde çalıştırılmamalı.

## 11. Performans

- Gereksiz layout derinliği oluşturma.
- Aşırı sayıda nested ScrollView kullanma.
- RecyclerView için ViewHolder yaklaşımını kullan.
- Büyük görselleri uygun boyutta yükle.
- Gereksiz bitmap kaynakları kullanma.
- Vector Drawable tercih et.
- Animasyonları düşük performanslı cihazlarda da akıcı tut.
- Gereksiz sürekli layout güncellemesinden kaçın.

## 12. Test Zorunluluğu

Her yeni veya güncellenen ekran aşağıdaki senaryolarda kontrol edilmeli:

1. Küçük telefon ekranı.
2. Standart telefon ekranı.
3. Büyük telefon ekranı.
4. Tablet veya geniş pencere.
5. Portrait.
6. Landscape.
7. Split Screen.
8. Light Mode.
9. Dark Mode.
10. Büyük yazı boyutu.
11. Klavye açık durumu.
12. Uzun Türkçe metinler.
13. Android 13+.
14. Android 15+ edge-to-edge davranışı.

Gerçek cihaz testleri yapılamıyorsa Android Emulator üzerinde farklı ekran profilleri oluştur.

Bütün ekranlarda:

- Yatay taşma olmamalı.
- Dikey taşma nedeniyle önemli kontroller erişilemez olmamalı.
- Üst üste binme olmamalı.
- Kartlar ve butonlar kesilmemeli.
- Metinler okunabilir olmalı.
- Tema renkleri tutarlı olmalı.

## 13. Geliştirme Kuralı

Mevcut çalışan ekranları topluca değiştirme.

Her ekranı aşamalı güncelle.

Her aşamada:

- XML kontrolü yap.
- Java bağlantılarını kontrol et.
- Light Mode kontrolü yap.
- Dark Mode kontrolü yap.
- Farklı pencere genişliklerini kontrol et.
- Projeyi derle.

Test edilmeyen bir cihazda veya senaryoda tam uyumluluk sağlandığını iddia etme.