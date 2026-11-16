package com.islamyolu.app.main;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.animation.PathInterpolator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.islamyolu.app.Entity.Hadis;
import com.islamyolu.app.R;
import com.islamyolu.app.Rozet.Badge;
import com.islamyolu.app.Service.BadgeService;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.LocationPreferences;
import com.islamyolu.app.VakitApi.City;
import com.islamyolu.app.databinding.FragmentDashboardBinding;
import com.islamyolu.app.islamicDays.IslamicDayActivity;
import com.islamyolu.app.prayertimes.ui.CitySelectionActivity;
import com.islamyolu.app.prayertimes.ui.PrayerTimesViewModel;
import com.islamyolu.app.weather.WeatherData;
import com.islamyolu.app.weather.WeatherIcon;
import com.islamyolu.app.weather.WeatherService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public class Dashboard extends Fragment {


    private CardView erkekCardView, kizCardView, duaCardView, takvimCardView, yarismaCardView, rozetBasari, ibadetCardView;
    private FragmentDashboardBinding binding;
    private BadgeService badgeService;
    private TextView erkekBebekLabel, kizBebekLabel;
    private TextView badgeName, hadisBilgi, hadisMetin;
    private TextView badgeDescription;
    private TextView weatherTemp, weatherDesc, weatherCity, dahaFazla;
    private ImageView badgeIcon, weatherIcon;
    private ImageButton eBabyRefresh, kBabyRefresh, wheatherRefresh, hadisYenile, hadisPaylas;
    private Dialog mDialog;
    private String eisimAnlami, kisimAnlami;
    private View weatherFrame, weatherPanel;
    /** Hissedilen / Nem / Rüzgar sığmazsa bugünün altına indirir */
    private WeatherStatsLayout weatherStatsLayout;
    /** Vakitlerin altındaki miladi/hicri tarih paneli */
    @Nullable private HomeDatePanel datePanel;
    boolean acikMi = false;
    private LocationPreferences sharedPrefs;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        initializeViews(root);
        initializeServices(root.getContext());
        initializeWeather(root, root.getContext(), WeatherService.MAX_AGE_APP_MS);
        initializeBadgeList();
        initializeNames();
        initializeHadis(root.getContext());


        buttonClicker();
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Hava kartındaki şehir adı: seçili namaz vakti şehri (MainActivity ile aynı ViewModel)
        // Tarih paneli: şehrin bugünü ve Diyanet hicri tarihi aynı vakit verisinden
        datePanel = new HomeDatePanel(view);
        new ViewModelProvider(requireActivity()).get(PrayerTimesViewModel.class).getState()
                .observe(getViewLifecycleOwner(), state -> {
                    weatherCity.setText(state.selection != null
                            ? state.selection.getCityName()
                            : getString(R.string.prayer_choose_city));
                    datePanel.setTimeline(state.status == PrayerTimesViewModel.Status.OK ? state.timeline : null);
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Uygulama gece yarısından sonra öne gelirse tarih güncellensin
        if (datePanel != null) {
            datePanel.refresh();
        }
    }
    private void initializeHadis(Context context) {
        List<Hadis> hadisList = loadHadisListFromAssets(context);
        if (hadisList != null && !hadisList.isEmpty()) {
            Hadis randomHadis = getRandomHadis(hadisList);
            hadisMetin.setText(randomHadis.getMetin());
            hadisBilgi.setText(randomHadis.getAltBilgi());
        }
    }

    /**
     * Hava kartı çevrim dışı önceliklidir: son kayıtlı veri hemen gösterilir; internet varsa ve veri
     * {@code maxAgeMs}'den eskiyse sunucudan yenilenir (uygulama her açıldığında en fazla 15 dk eski).
     */
    private void initializeWeather(View root, Context context, long maxAgeMs) {
        WeatherService weatherService = new WeatherService(context);
        sharedPrefs = AppPreferences.from(context).location();
        String cityName = sharedPrefs.getLegacyCityName();
        if (cityName == null || cityName.isEmpty()) {
            weatherPanel.setVisibility(View.VISIBLE);
            weatherFrame.setVisibility(View.INVISIBLE);
            return;
        } else {
            weatherPanel.setVisibility(View.GONE);
            weatherFrame.setVisibility(View.VISIBLE);
        }
        City city = City.fromCityName(cityName);
        if (city == null) {
            weatherDesc.setText(getString(R.string.weather_unavailable, cityName));
            return;
        }
        weatherService.load(city.getLatitude(), city.getLongitude(), maxAgeMs, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherData weatherData) {
                // Yanıt geldiğinde ekran kapanmış olabilir
                if (binding == null || !isAdded()) {
                    return;
                }
                showWeather(root, weatherData);
            }

            @Override
            public void onError(String error) {
                // Hiç kayıtlı veri yok ve indirilemedi (ör. ilk açılış, internet yok)
                Log.w("WeatherService", "Hava durumu alınamadı: " + error);
                if (binding != null && isAdded()) {
                    weatherDesc.setText(getString(R.string.weather_unavailable, cityName));
                }
            }
        });
    }

    private void showWeather(View root, WeatherData weatherData) {
        WeatherData.CurrentWeather current = weatherData.getCurrentWeather();
        if (current != null) {
            weatherTemp.setText(formatTemp(current.getTemperature()));
            weatherIcon.setImageResource(WeatherIcon.getWeatherIconResource(current.getWeatherCode(), current.isDay()));
            weatherDesc.setText(weatherData.getWeatherDescription());
            Double feels = current.getApparentTemperature();
            Integer humidity = current.getHumidity();
            Double wind = current.getWindSpeed();
            setStatValue(root, R.id.weather_stat_feels, feels != null ? formatTempWhole(feels) : null);
            setStatValue(root, R.id.weather_stat_humidity,
                    humidity != null ? getString(R.string.weather_humidity_format, humidity) : null);
            setStatValue(root, R.id.weather_stat_wind,
                    wind != null ? getString(R.string.weather_wind_format, wind) : null);
            // Yeni değerler daha geniş olabilir: yan yana sığıyor mu yeniden bak
            weatherStatsLayout.update();
        }

        WeatherData.DailyWeatherForecast forecast = weatherData.getDaily();
        if (forecast == null || forecast.getDates() == null || forecast.getWeatherCodes() == null
                || forecast.getMaxTemperatures() == null) {
            return;
        }
        List<String> days = forecast.getDates();
        List<Integer> codes = forecast.getWeatherCodes();
        List<Double> temps = forecast.getMaxTemperatures();
        int size = Math.min(days.size(), Math.min(codes.size(), temps.size()));
        // Bugünden sonraki günler (kayıtlı veri birkaç gün eskiyse geçmiş günler atlanır)
        LocalDate today = LocalDate.now();
        int cell = 0;
        for (int i = 0; i < size && cell < FORECAST_CELLS.length; i++) {
            LocalDate date = parseDate(days.get(i));
            if (date == null || !date.isAfter(today)) {
                continue;
            }
            View cellView = root.findViewById(FORECAST_CELLS[cell++]);
            ((TextView) cellView.findViewById(R.id.forecast_day))
                    .setText(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()));
            ((ImageView) cellView.findViewById(R.id.forecast_icon))
                    .setImageResource(WeatherIcon.getWeatherIconResource(codes.get(i), true));
            ((TextView) cellView.findViewById(R.id.forecast_temp)).setText(formatTempWhole(temps.get(i)));
        }
    }

    /** Hissedilen / Nem / Rüzgar hücresine ikon ve etiketi bir kez yazar (initializeViews). */
    private static void bindStat(View root, int cellId, int iconRes, int labelRes) {
        View cell = root.findViewById(cellId);
        ((ImageView) cell.findViewById(R.id.weather_stat_icon)).setImageResource(iconRes);
        ((TextView) cell.findViewById(R.id.weather_stat_label)).setText(labelRes);
    }

    /** Değer yoksa (eski kayıt) yer tutucu "—" gösterilir */
    private void setStatValue(View root, int cellId, @Nullable String value) {
        TextView view = root.findViewById(cellId).findViewById(R.id.weather_stat_value);
        view.setText(value != null ? value : getString(R.string.weather_temp_placeholder));
    }
    /** 5 günlük tahmin hücreleri (item_weather_forecast_day) */
    private static final int[] FORECAST_CELLS = {
            R.id.weather_day_cell_1, R.id.weather_day_cell_2, R.id.weather_day_cell_3,
            R.id.weather_day_cell_4, R.id.weather_day_cell_5
    };

    /** Anlık sıcaklık tek ondalıkla, uygulama diline göre: "18,4°" / "18.4°" */
    private String formatTemp(double celsius) {
        return getString(R.string.weather_temp_format, celsius);
    }

    /** Hissedilen ve 5 günlük tahmin tam sayıya yuvarlanır: "3°" (sade görünüm; -0,4 → "0°") */
    private String formatTempWhole(double celsius) {
        return getString(R.string.weather_temp_whole_format, (int) Math.round(celsius));
    }

    /** "2026-11-06" → tarih; okunamazsa null */
    @Nullable
    private static LocalDate parseDate(String isoDate) {
        try {
            return LocalDate.parse(isoDate);
        } catch (DateTimeParseException e) {
            Log.w("WeatherService", "Tahmin tarihi okunamadı: " + isoDate, e);
            return null;
        }
    }

    private void initializeNames() {
        eBabyRefreshClick();
        kBabyRefreshClick();
    }

    private void initializeBadgeList() {
        List<Badge> allBadges = badgeService.getDisableBadges();
        // Eğer listede rozet varsa rastgele birini seç
        if (allBadges != null && !allBadges.isEmpty()) {
            // Rastgele index üret
            Random random = new Random();
            int randomIndex = random.nextInt(allBadges.size());

            // Rastgele seçilen rozet
            Badge selectedBadge = allBadges.get(randomIndex);

            // UI bileşenlerini doldur
            badgeName.setText(selectedBadge.getBadgeType().getLabel());
            badgeDescription.setText(selectedBadge.getBadgeType().getDescription());
            badgeIcon.setImageResource(selectedBadge.getBadgeType().getIconResId());
            rozetBasari.setVisibility(View.VISIBLE);
        } else {
            // Kazanılmamış rozet kalmadı: boş/yer tutucu kart gösterme
            rozetBasari.setVisibility(View.GONE);
        }
    }

    private void initializeServices(Context context) {
        badgeService = new BadgeService(context);
    }

    private void buttonClicker() {
        yarismaCardView.setOnClickListener(v -> yarismaFragment());
        takvimCardView.setOnClickListener(v -> takvimFragment());
        ibadetCardView.setOnClickListener(v -> ibadetFragment());
        eBabyRefresh.setOnClickListener(v -> eBabyRefreshClick());
        kBabyRefresh.setOnClickListener(v -> kBabyRefreshClick());
        erkekCardView.setOnClickListener(v -> eCardViewPopup());
        kizCardView.setOnClickListener(v -> kCardViewPopup());
        wheatherRefresh.setOnClickListener(v -> wheatherRefreshClick());
        duaCardView.setOnClickListener(v -> dahaFazlaClick());
        hadisYenile.setOnClickListener(v -> hadisYenileClick());
        hadisPaylas.setOnClickListener(v -> hadisPaylasClick());
    }

    private void hadisPaylasClick() {
        // 1. LayoutInflater ile özel layout'u yükle
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View shareView = inflater.inflate(R.layout.ibadet_share_card, new FrameLayout(requireContext()), false);

        // 2. Verileri layout içine doldur
        TextView rivayet = shareView.findViewById(R.id.shareBaslik);
        TextView hadis = shareView.findViewById(R.id.shareAciklama);
        ImageView ivImage = shareView.findViewById(R.id.shareImage);
        ivImage.setVisibility(View.GONE);

        String formattedText = getString(R.string.hadis_format, getString(R.string.hadis_header), hadisBilgi.getText());
        rivayet.setText(formattedText);
        hadis.setText(hadisMetin.getText());

        // 3. Ölçülmeyen view'dan bitmap al (önce ölç!)
        DisplayMetrics displayMetrics = requireContext().getResources().getDisplayMetrics();
        int width = displayMetrics.widthPixels;  // ekran genişliği
        int height = displayMetrics.heightPixels; // ekran yüksekliği

// View'i ölç ve yerleştir
        shareView.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST)
        );
        shareView.layout(0, 0, shareView.getMeasuredWidth(), shareView.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(shareView.getMeasuredWidth(), shareView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        shareView.draw(canvas);

        // 4. Bitmap'i paylaş
        try {
            File file = new File(requireContext().getCacheDir(), "islamyolu.png");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();

            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            requireContext().startActivity(Intent.createChooser(shareIntent, "Paylaş"));

        } catch (IOException e) {
            Log.d("CardAdapter", "Hata oluştu:" + e.getMessage());
        }
    }

    private void hadisYenileClick() {
        initializeHadis(getContext());

    }

    /** Hadis kartı açılıp kapanırken kartın ve altındaki panellerin kayma süresi */
    private static final long HADIS_TOGGLE_DURATION_MS = 300;

    private void dahaFazlaClick() {
        // Kök, kartların ortak üst katmanı: kart büyürken/küçülürken altındaki kısayol ve rozet
        // panelleri de aynı anda yumuşakça kayar (yalnızca kart kök olunca alttakiler bir anda atlıyordu).
        // Boyut değişimi ve düğmelerin belirmesi birlikte çalışır; ardışık AutoTransition'daki bekleme yok.
        ViewGroup sceneRoot = (ViewGroup) duaCardView.getParent();
        TransitionSet transition = new TransitionSet()
                .setOrdering(TransitionSet.ORDERING_TOGETHER)
                .addTransition(new ChangeBounds())
                .addTransition(new Fade())
                .setDuration(HADIS_TOGGLE_DURATION_MS)
                .setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f));
        TransitionManager.beginDelayedTransition(sceneRoot, transition);

        if (acikMi) {
            hadisMetin.setMaxLines(5);
            hadisMetin.setEllipsize(TextUtils.TruncateAt.END);
            dahaFazla.setText(getText(R.string.devamhadis));
            acikMi = false;
            hadisPaylas.setVisibility(View.GONE);
            hadisYenile.setVisibility(View.GONE);
        } else {
            hadisMetin.setMaxLines(Integer.MAX_VALUE);
            hadisMetin.setEllipsize(null);
            dahaFazla.setText(getText(R.string.dahaazhadis));
            acikMi = true;
            hadisPaylas.setVisibility(View.VISIBLE);
            hadisYenile.setVisibility(View.VISIBLE);
        }
    }

    private void wheatherRefreshClick() {
        initializeWeather(binding.getRoot(), getContext(), WeatherService.MAX_AGE_FORCE);
    }

    private void kCardViewPopup() {
        // Yeni bir Dialog oluştur
        mDialog = new Dialog(requireContext());
        Objects.requireNonNull(mDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // custom_popup.xml layoutunu dialog'a yükle
        mDialog.setContentView(R.layout.isim_info_popup);
        // Dialog dışına tıklanıldığında kapansın
        mDialog.setCancelable(true);
        Button closeButton = mDialog.findViewById(R.id.closeButton);
        ImageView image = mDialog.findViewById(R.id.imagePopUp);
        image.setImageResource(R.drawable.k_baby);

        TextView text = mDialog.findViewById(R.id.hintPopUpTextHeader);

        String metin = getString(R.string.isim_aciklama,
                kizBebekLabel.getText(),
                getString(R.string.isimtagAnlam),
                kisimAnlami);
        text.setText(metin);

        closeButton.setOnClickListener(view -> mDialog.dismiss());
        mDialog.show();
    }

    private void eCardViewPopup() {
        // Yeni bir Dialog oluştur
        mDialog = new Dialog(requireContext());
        Objects.requireNonNull(mDialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // custom_popup.xml layoutunu dialog'a yükle
        mDialog.setContentView(R.layout.isim_info_popup);
        // Dialog dışına tıklanıldığında kapansın
        mDialog.setCancelable(true);
        Button closeButton = mDialog.findViewById(R.id.closeButton);
        ImageView image = mDialog.findViewById(R.id.imagePopUp);
        image.setImageResource(R.drawable.e_baby);

        TextView text = mDialog.findViewById(R.id.hintPopUpTextHeader);
        String metin = getString(R.string.isim_aciklama,
                erkekBebekLabel.getText(),
                getString(R.string.isimtagAnlam),
                eisimAnlami);
        text.setText(metin);

        closeButton.setOnClickListener(view -> mDialog.dismiss());
        mDialog.show();
    }

    private void kBabyRefreshClick() {
        String[] kbabyNames = getResources().getStringArray(R.array.kbebek_isim);
        Random kRandom = new Random();
        int kRandomIndex = kRandom.nextInt(kbabyNames.length);
        String kRandomName = kbabyNames[kRandomIndex];
        String[] parcalar = kRandomName.split("#", 2); // Sadece 2 parçaya ayır
        if (parcalar.length == 2) {
            String isim = parcalar[0].trim();
            String anlam = parcalar[1].trim();
            // Uzun isim küçültülmez; kartta iki satıra iner
            kizBebekLabel.setText(isim);
            kisimAnlami = anlam;
        }
    }

    private void eBabyRefreshClick() {
        String[] ebabyNames = getResources().getStringArray(R.array.ebebek_isim);
        Random eRandom = new Random();
        int eRandomIndex = eRandom.nextInt(ebabyNames.length);
        String eRandomName = ebabyNames[eRandomIndex];
        String[] parcalar = eRandomName.split("#", 2); // Sadece 2 parçaya ayır

        if (parcalar.length == 2) {
            String isim = parcalar[0].trim();
            String anlam = parcalar[1].trim();
            // Uzun isim küçültülmez; kartta iki satıra iner
            erkekBebekLabel.setText(isim);
            eisimAnlami = anlam;
        }
    }

    private List<Hadis> loadHadisListFromAssets(Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("hadisler.json");
            InputStreamReader reader = new InputStreamReader(inputStream);

            Type listType = new TypeToken<List<Hadis>>() {
            }.getType();
            return new Gson().fromJson(reader, listType);

        } catch (Exception e) {
            Log.e("HadisJsonParser", "Hata oluştu", e);
            return null;
        }
    }

    private Hadis getRandomHadis(List<Hadis> hadisList) {
        Random random = new Random();
        int index = random.nextInt(hadisList.size());
        return hadisList.get(index);
    }

    private void ibadetFragment() {
        NavController navController = NavHostFragment.findNavController(this);
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(navController.getGraph().getStartDestinationId(), true)
                .build();
        navController.navigate(R.id.nav_gallery, null, options); // nav_gallery = menu'deki ibadet id'si
    }

    // Takvim ekranı şehir bilgisi kullanmaz; konum şartı olmadan herkes açabilir.
    private void takvimFragment() {
        Intent intent = new Intent(getContext(), IslamicDayActivity.class);
        startActivity(intent);
    }

    private void yarismaFragment() {
        NavController navController = NavHostFragment.findNavController(this);
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.nav_dashboard, true) // Dashboard'u tamamen temizle
                .build();
        navController.navigate(R.id.nav_home, null, options);
    }


    private void initializeViews(View root) {
        erkekCardView = root.findViewById(R.id.erkekCardView);
        kizCardView = root.findViewById(R.id.kizCardView);
        duaCardView = root.findViewById(R.id.duaCardView);
        takvimCardView = root.findViewById(R.id.takvimCardView);
        yarismaCardView = root.findViewById(R.id.yarismaCardView);
        rozetBasari = root.findViewById(R.id.rozetBasari);
        ibadetCardView = root.findViewById(R.id.ibadetCardView);
        setupShortcutCard(ibadetCardView, R.drawable.d_ibadet, R.string.dashboard_ibadet_label);
        setupShortcutCard(takvimCardView, R.drawable.d_takvim, R.string.dashboard_takvim_label);
        setupShortcutCard(yarismaCardView, R.drawable.d_yarisma, R.string.dashboard_yarisma_label);
        badgeName = root.findViewById(R.id.badgeName);
        badgeDescription = root.findViewById(R.id.badgeDescription);
        badgeIcon = root.findViewById(R.id.badgeIcon);
        setupNameCard(erkekCardView, R.string.home_name_boy_title, R.drawable.e_baby,
                R.color.color_name_boy_accent, R.string.home_name_boy_refresh_cd);
        setupNameCard(kizCardView, R.string.home_name_girl_title, R.drawable.k_baby,
                R.color.color_name_girl_accent, R.string.home_name_girl_refresh_cd);
        // "Erkek ismi" / "Kız ismi": ikisi de tek satır ya da ikisi de iki satır (uyumlu görünüm)
        MatchedTitleLines.attach(erkekCardView.findViewById(R.id.name_card_title),
                kizCardView.findViewById(R.id.name_card_title));
        erkekBebekLabel = erkekCardView.findViewById(R.id.name_card_value);
        kizBebekLabel = kizCardView.findViewById(R.id.name_card_value);
        eBabyRefresh = erkekCardView.findViewById(R.id.name_card_refresh);
        kBabyRefresh = kizCardView.findViewById(R.id.name_card_refresh);
        weatherFrame = root.findViewById(R.id.weatherFrame);
        weatherIcon = root.findViewById(R.id.weather_icon);
        weatherTemp = root.findViewById(R.id.weather_temp);
        weatherDesc = root.findViewById(R.id.weather_desc);
        weatherCity = root.findViewById(R.id.weather_city);
        bindStat(root, R.id.weather_stat_feels, R.drawable.ic_weather_feels, R.string.weather_feels_like);
        bindStat(root, R.id.weather_stat_humidity, R.drawable.ic_weather_humidity, R.string.weather_humidity);
        bindStat(root, R.id.weather_stat_wind, R.drawable.ic_weather_wind, R.string.weather_wind);
        weatherStatsLayout = new WeatherStatsLayout(root.findViewById(R.id.weather_top_row),
                root.findViewById(R.id.weather_today), root.findViewById(R.id.weather_stats_divider),
                root.findViewById(R.id.weather_stats_row), root.findViewById(R.id.weather_stat_feels),
                root.findViewById(R.id.weather_stat_humidity), root.findViewById(R.id.weather_stat_wind));
        // Şehre dokununca Diyanet şehir seçimi (namaz vakitleri ve hava aynı şehirden)
        root.findViewById(R.id.weather_city_layout).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CitySelectionActivity.class)));
        wheatherRefresh = root.findViewById(R.id.wheatherRefresh);
        weatherPanel = root.findViewById(R.id.weatherPanel);
        hadisBilgi = root.findViewById(R.id.hadisBilgi);
        hadisMetin = root.findViewById(R.id.hadisMetin);
        dahaFazla = root.findViewById(R.id.dahaFazla);
        hadisYenile = root.findViewById(R.id.hadisYenile);
        hadisPaylas = root.findViewById(R.id.hadisPaylas);
    }

    /** Ortak kısayol kartı (item_home_shortcut_card): arka plan görseli ve başlık */
    private static void setupShortcutCard(View card, int imageRes, int labelRes) {
        ((ImageView) card.findViewById(R.id.shortcut_image)).setImageResource(imageRes);
        ((TextView) card.findViewById(R.id.shortcut_label)).setText(labelRes);
    }

    /** Ortak isim kartı (item_home_name_card): başlık, ikon, başlık vurgu rengi, yenile açıklaması */
    private void setupNameCard(View card, int titleRes, int iconRes, int accentColorRes, int refreshCdRes) {
        TextView title = card.findViewById(R.id.name_card_title);
        title.setText(titleRes);
        title.setTextColor(ContextCompat.getColor(card.getContext(), accentColorRes));
        ((ImageView) card.findViewById(R.id.name_card_icon)).setImageResource(iconRes);
        card.findViewById(R.id.name_card_refresh).setContentDescription(getString(refreshCdRes));
    }

}
