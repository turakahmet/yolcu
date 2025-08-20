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
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.islamyolu.app.Entity.Hadis;
import com.islamyolu.app.R;
import com.islamyolu.app.Rozet.Badge;
import com.islamyolu.app.Service.BadgeService;
import com.islamyolu.app.Service.SharedPrefsHelperService;
import com.islamyolu.app.VakitApi.City;
import com.islamyolu.app.databinding.FragmentDashboardBinding;
import com.islamyolu.app.weather.WeatherData;
import com.islamyolu.app.weather.WeatherIcon;
import com.islamyolu.app.weather.WeatherService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
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
    private TextView weatherTemp, locationDesc, weatherDesc, dahaFazla, weatherKonumAlert;
    private ImageView badgeIcon, weatherIcon, weatherBackground;
    private ImageButton eBabyRefresh, kBabyRefresh, wheatherRefresh, hadisYenile, hadisPaylas;
    private Dialog mDialog;
    private String eisimAnlami, kisimAnlami;
    private ProgressBar loadingWeatherIcon;
    private FrameLayout weatherFrame;
    private ConstraintLayout weatherPanel;
    boolean acikMi = false;
    private SharedPrefsHelperService sharedPrefs;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        initializeViews(root);
        initializeServices(root.getContext());
        initializeWeather(root, root.getContext());
        initializeBadgeList();
        initializeNames();
        initializeHadis(root.getContext());


        buttonClicker();
        return root;
    }

    private void initializeHadis(Context context) {
        List<Hadis> hadisList = loadHadisListFromAssets(context);
        if (hadisList != null && !hadisList.isEmpty()) {
            Hadis randomHadis = getRandomHadis(hadisList);
            hadisMetin.setText(randomHadis.getMetin());
            hadisBilgi.setText(randomHadis.getAltBilgi());
        }
    }

    private void initializeWeather(View root, Context context) {
        WeatherService weatherService = new WeatherService(context);
        sharedPrefs = new SharedPrefsHelperService(context, MainActivity.SETTINGS_PREFS);
        String cityName = sharedPrefs.getCityNameFromPreferences();
        if (cityName == null || cityName.isEmpty()) {
            weatherPanel.setVisibility(View.VISIBLE);
            weatherFrame.setVisibility(View.INVISIBLE);
            return;
        } else {
            weatherPanel.setVisibility(View.GONE);
            weatherFrame.setVisibility(View.VISIBLE);
        }
        if (City.fromCityName(cityName) == null) {
            Toast.makeText(getContext(), cityName + " Şehiri için hava durumu bilgisi alınamadı. Sorun devam ederse bizimle iletişime geçiniz.", Toast.LENGTH_LONG).show();
            return;
        }
        double latitude = Objects.requireNonNull(City.fromCityName(cityName)).getLatitude();
        double longitude = Objects.requireNonNull(City.fromCityName(cityName)).getLongitude();
        weatherService.getDailyWeatherAndForecastByDate(latitude, longitude, new WeatherService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherData weatherData) {
                WeatherData.DailyWeatherForecast forecast = weatherData.getDaily();
                WeatherData.CurrentWeather currentWeather = weatherData.getCurrentWeather();
                if (forecast == null || currentWeather == null) {
                    return;
                }

                weatherTemp.setText(String.format(Locale.getDefault(), "%.1f°", currentWeather.getTemperature()));
                weatherIcon.setImageResource(WeatherIcon.getWeatherIconResource(currentWeather.getWeatherCode(), isDayOrNight()));
                weatherDesc.setText(weatherData.getWeatherDescription());
                locationDesc.setText(cityName);
                weatherBackground.setImageResource(WeatherIcon.getWeatherBackgroundResource(currentWeather.getWeatherCode()));

                List<String> days = forecast.getDates();
                List<Integer> codes = forecast.getWeatherCodes();
                List<Double> temps = forecast.getMaxTemperatures();


                // Diğer günler için
                int[] dayIds = {
                        R.id.weather_day_1, R.id.weather_day_2, R.id.weather_day_3,
                        R.id.weather_day_4, R.id.weather_day_5
                };
                int[] iconIds = {
                        R.id.weather_1, R.id.weather_2, R.id.weather_3,
                        R.id.weather_4, R.id.weather_5
                };
                int[] tempIds = {
                        R.id.weather_temp_1, R.id.weather_temp_2, R.id.weather_temp_3,
                        R.id.weather_temp_4, R.id.weather_temp_5
                };

                for (int i = 0; i < 5 && i < days.size(); i++) {
                    String dateStr = days.get(i);
                    int code = codes.get(i);
                    double temp = temps.get(i);

                    TextView dayView = root.findViewById(dayIds[i]);
                    ImageView iconView = root.findViewById(iconIds[i]);
                    TextView tempView = root.findViewById(tempIds[i]);

                    // Gün adı
                    try {
                        LocalDate date = LocalDate.parse(dateStr);
                        DayOfWeek dayOfWeek = date.getDayOfWeek();
                        String dayShort = dayOfWeek.getDisplayName(TextStyle.SHORT, new Locale("tr"));
                        dayView.setText(dayShort.substring(0, 3)); // "Perşembe" -> "Per"
                    } catch (Exception e) {
                        dayView.setText("?");
                    }

                    iconView.setImageResource(WeatherIcon.getWeatherIconResource(code, true));
                    tempView.setText(String.format(Locale.getDefault(), "%.1f°", temp));
                }
            }

            @Override
            public void onError(String error) {
                Log.e("WeatherService", "Hava durumu alınamadı: " + error);
            }
        });
    }

    private boolean isDayOrNight() {
        LocalTime simdikiSaat = LocalTime.now();
        LocalTime geceBaslangic = LocalTime.of(19, 0);  // 19:00
        LocalTime gunduzBaslangic = LocalTime.of(7, 0); // 07:00

        return !simdikiSaat.isAfter(geceBaslangic) && !simdikiSaat.isBefore(gunduzBaslangic);
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

    private void dahaFazlaClick() {
        TransitionManager.beginDelayedTransition(duaCardView, new AutoTransition());

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
        initializeWeather(binding.getRoot(), getContext());
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
            if (isim.length() > 10) {
                kizBebekLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            } else
                kizBebekLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
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
            if (isim.length() > 10) {
                erkekBebekLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            } else
                erkekBebekLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
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
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_dashboardFragment_to_IbadetFragment);
    }

    private void takvimFragment() {
        String cityName = sharedPrefs.getCityNameFromPreferences();
        if (cityName == null || cityName.isEmpty()) {
            Toast.makeText(getContext(), "Takvim için konum ayarı yapmalısınız.", Toast.LENGTH_SHORT).show();
            return;
        }
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_dashboardFragment_to_IslamicDayFragment);
    }

    private void yarismaFragment() {
        NavController navController = Navigation.findNavController(requireView());
        navController.navigate(R.id.action_dashboardFragment_to_homeFragment);
    }

    private void initializeViews(View root) {
        erkekCardView = root.findViewById(R.id.erkekCardView);
        kizCardView = root.findViewById(R.id.kizCardView);
        duaCardView = root.findViewById(R.id.duaCardView);
        takvimCardView = root.findViewById(R.id.takvimCardView);
        yarismaCardView = root.findViewById(R.id.yarismaCardView);
        rozetBasari = root.findViewById(R.id.rozetBasari);
        ibadetCardView = root.findViewById(R.id.ibadetCardView);
        badgeName = root.findViewById(R.id.badgeName);
        badgeDescription = root.findViewById(R.id.badgeDescription);
        badgeIcon = root.findViewById(R.id.badgeIcon);
        erkekBebekLabel = root.findViewById(R.id.erkekBebekLabel);
        kizBebekLabel = root.findViewById(R.id.kizBebekLabel);
        eBabyRefresh = root.findViewById(R.id.eBabyRefresh);
        kBabyRefresh = root.findViewById(R.id.kBabyRefresh);
        weatherIcon = root.findViewById(R.id.weather_icon);
        locationDesc = root.findViewById(R.id.location_desc);
        weatherDesc = root.findViewById(R.id.weather_desc);
        weatherTemp = root.findViewById(R.id.weather_temp);
        loadingWeatherIcon = root.findViewById(R.id.loadingWeatherIcon);
        weatherFrame = root.findViewById(R.id.weatherFrame);
        weatherKonumAlert = root.findViewById(R.id.weatherKonumAlert);
        weatherBackground = root.findViewById(R.id.weatherBackground);
        wheatherRefresh = root.findViewById(R.id.wheatherRefresh);
        weatherPanel = root.findViewById(R.id.weatherPanel);
        hadisBilgi = root.findViewById(R.id.hadisBilgi);
        hadisMetin = root.findViewById(R.id.hadisMetin);
        dahaFazla = root.findViewById(R.id.dahaFazla);
        hadisYenile = root.findViewById(R.id.hadisYenile);
        hadisPaylas = root.findViewById(R.id.hadisPaylas);
    }

}
