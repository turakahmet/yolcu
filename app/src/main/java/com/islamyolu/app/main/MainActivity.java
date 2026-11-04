package com.islamyolu.app.main;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.islamyolu.app.R;
import com.islamyolu.app.Service.AddressResolver;
import com.islamyolu.app.Service.BadgeService;
import com.islamyolu.app.Service.GoldService;
import com.islamyolu.app.Service.LocationAccess;
import com.islamyolu.app.Service.LocationService;
import com.islamyolu.app.Service.SharedPrefsHelperService;
import com.islamyolu.app.Service.SoundService;
import com.islamyolu.app.VakitApi.City;
import com.islamyolu.app.VakitApi.DailyPrayerWorkerReceiver;
import com.islamyolu.app.VakitApi.NamazVakiti;
import com.islamyolu.app.VakitApi.PrayerTimesFetcher;
import com.islamyolu.app.VakitApi.VakitNoApi;
import com.islamyolu.app.databinding.ActivityMainBinding;
import com.islamyolu.app.monthlyCalender.MonthlyPrayerActivity;
import com.islamyolu.app.profile.Avatar;

import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements ProfilePictureDialogFragment.ProfilePictureDialogListener {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private TextView imsakTime, gunesTime, ogleTime, ikindiTime, aksamTime, yatsiTime, locationTextView, usernameTextView, kalanSureTextView, toolbarKalanSureLabel, total_user_gold, total_rozet;
    private LinearLayout imsakTimeLayout, gunesTimeLayout, ogleTimeLayout, ikindiTimeLayout, aksamTimeLayout, yatsiTimeLayout, locationRefreshLayout, remain_time_layout;

    /** İzin, konum açık/kapalı ve konum alma merkezi LocationAccess'te */
    private final LocationAccess locationAccess = LocationAccess.register(this, new LocationAccess.Options(),
            new LocationAccess.Callback() {
                @Override
                public void onLocation(@NonNull Location location) {
                    saveLastKnownLocation(location);
                    handleNewLocation(location);
                }

                @Override
                public void onUnavailable(@NonNull LocationService.Unavailable reason) {
                    onLocationUnavailable(reason);
                }
            });

    // SharedPreferences için tanımlar
    private SharedPreferences locationPrefs;
    private SharedPreferences userPrefs;

    public static final String LOCATION_PREFS = "location_prefs";
    private static final String USER_PREFS = "user_prefs";
    private static final String USERNAME_KEY = "username";
    private static final String LAST_KNOWN_LAT_KEY = "last_known_latitude";
    private static final String LAST_KNOWN_LON_KEY = "last_known_longitude";
    private static final String AVATAR_ICON = "profile_picture_name";
    public static final String SETTINGS_PREFS = "SettingPrefs";
    private ImageButton refreshButton;
    private ImageView editIcon;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 123;
    private ShapeableImageView userAvatar;
    private SoundService soundService;
    private BadgeService badgeService;
    private GoldService goldService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Binding işlemi
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // SharedPreferences ve servisler
        SharedPrefsHelperService sharedPrefsHelperService = new SharedPrefsHelperService(getApplicationContext(), SETTINGS_PREFS);
        locationPrefs = getSharedPreferences(LOCATION_PREFS, Context.MODE_PRIVATE);
        userPrefs = getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE);
        sharedPrefsHelperService.defaultSettingPreference();

        initializeViews();
        initializeServices();
        initializeHeaderView();
        initialGoldAmount();
//        checkInternetConnection();

        // Refresh butonuna tıklama işlemi
        // Kullanıcı istedi: konum kapalıysa "Konumu aç" penceresi gösterilir
        refreshButton.setOnClickListener(v -> locationAccess.requestSingleLocation(true));
        // Geri tuşu davranışını ayarla
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });


        // Açılışta: izin yoksa istenir; konum kapalıysa pencere gösterilmez, son kayıtlı konum kullanılır
        if (savedInstanceState == null) {
            locationAccess.requestSingleLocation(false);
        }

        remain_time_layout.setOnClickListener(v -> {
            getSharedPreferences(MainActivity.LOCATION_PREFS, MODE_PRIVATE);
            String getCityNameFromPreferences = locationPrefs.getString("city_name", "");
            if (getCityNameFromPreferences.isEmpty()) {
                Toast.makeText(this, "Takvim için konum ayarı yapmalısınız.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(MainActivity.this, MonthlyPrayerActivity.class);
            startActivity(intent);
        });
    }

    private boolean hasNotificationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }


    private void initializeHeaderView() {
        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_dashboard, R.id.nav_home, R.id.nav_gallery, R.id.nav_compass, R.id.nav_zekat, R.id.nav_muze, R.id.nav_rozet)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);


        // NavigationView'in header'ından View'leri bulma
        View headerView = navigationView.getHeaderView(0); // Header layout'a erişim
        usernameTextView = headerView.findViewById(R.id.username); // Username TextView
        editIcon = headerView.findViewById(R.id.editIcon);
        userAvatar = headerView.findViewById(R.id.user_avatar);
        total_user_gold = headerView.findViewById(R.id.total_user_gold);
        total_rozet = headerView.findViewById(R.id.total_rozet);


        // Edit icon click listener
        String savedUsername = userPrefs.getString(USERNAME_KEY, getString(R.string.defaultUserName));
        // Kaydedilen kullanıcı adını TextView'e ata
        usernameTextView.setText(savedUsername);
        // Daha önce kaydedilmiş avatarı yükleme
        String savedName = userPrefs.getString(AVATAR_ICON, Avatar.MAN_1.name());
        Avatar savedAvatar = Avatar.fromName(savedName);
        userAvatar.setImageResource(savedAvatar.getResId());

//        // SharedPreferences içindeki tüm verileri temizle
//        SharedPreferences.Editor editor = userPrefs.edit();
//        editor.clear(); // Tüm verileri temizle
//        editor.apply(); // Değişiklikleri kaydet
        editIcon.setOnClickListener(v -> {
            if (savedUsername.equals(getString(R.string.defaultUserName))) {
                usernameTextView.setText("");
            }
            showEditDialog();
        });

        userAvatar.setOnClickListener(v -> {
            soundService.commonButtonEffect();
            ProfilePictureDialogFragment dialogFragment = new ProfilePictureDialogFragment();
            dialogFragment.show(getSupportFragmentManager(), "profilePictureDialog");
            dialogFragment.setCancelable(false);
        });

        // DrawerLayout'a DrawerListener ekleyin
        drawer.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                // Çekmece sürüklenirken yapılan işlemler (İsteğe bağlı)
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                total_user_gold.setText(String.valueOf(goldService.getGoldAmount()));
                ValueAnimator animator = ValueAnimator.ofInt(0, badgeService.getEnabledBadgeCount());
                animator.setDuration(1000); //
                animator.addUpdateListener(animation -> {
                    // TextView'de sayıyı yavaş yavaş artırarak göster
                    total_rozet.setText(animation.getAnimatedValue().toString());
                });
                animator.start();
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                // NavigationView kapandığında yapılacak işlemler
                Log.d("MainActivity", "Navigation kapandı");
                total_rozet.setText("0");
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                // Çekmece durumunda değişiklik olduğunda yapılacak işlemler (İsteğe bağlı)
            }
        });
    }

    private void initializeServices() {
        soundService = new SoundService(this);
        badgeService = new BadgeService(this);
    }

    private void initializeViews() {
        // TextView'leri tanımla
        imsakTime = findViewById(R.id.imsak_time);
        gunesTime = findViewById(R.id.gunes_time);
        ogleTime = findViewById(R.id.ogle_time);
        ikindiTime = findViewById(R.id.ikindi_time);
        aksamTime = findViewById(R.id.aksam_time);
        yatsiTime = findViewById(R.id.yatsi_time);
        locationTextView = findViewById(R.id.location_time);
        refreshButton = findViewById(R.id.refresh_button);

        imsakTimeLayout = findViewById(R.id.imsak_layout);
        gunesTimeLayout = findViewById(R.id.gunes_layout);
        ogleTimeLayout = findViewById(R.id.ogle_layout);
        ikindiTimeLayout = findViewById(R.id.ikindi_layout);
        aksamTimeLayout = findViewById(R.id.aksam_layout);
        yatsiTimeLayout = findViewById(R.id.yatsi_layout);
        kalanSureTextView = findViewById(R.id.toolbarKalanSure);
        locationRefreshLayout = findViewById(R.id.location_refresh_Layout);
        remain_time_layout = findViewById(R.id.remain_time_layout);
        toolbarKalanSureLabel = findViewById(R.id.toolbarKalanSureLabel);
    }


    private void initialGoldAmount() {
        goldService = new GoldService(this);
        goldService.setInitialGoldAmount(); // Başlangıç altın miktarını ayarla
//        total_user_gold.setText(String.valueOf(goldService.getGoldAmount()));
    }


    /** Konum alınamazsa son kayıtlı konum kullanılır; o da yoksa yenile alanı gösterilir. */
    private void onLocationUnavailable(LocationService.Unavailable reason) {
        if (reason == LocationService.Unavailable.PERMISSION_DENIED) {
            locationRefreshLayout.setVisibility(View.VISIBLE);
            Toast.makeText(this, R.string.no_permit_location, Toast.LENGTH_SHORT).show();
            return;
        }
        Location savedLocation = loadLastKnownLocation();
        if (savedLocation != null) {
            handleNewLocation(savedLocation);
        } else {
            locationRefreshLayout.setVisibility(View.VISIBLE);
            Toast.makeText(this, R.string.konumMevcutDegil, Toast.LENGTH_SHORT).show();
        }
    }

    // Yeni konumu işleme (il adı, mevcut il listesiyle eşleşsin diye Türkçe çözümlenir)
    private void handleNewLocation(Location location) {
        requestNotificationPermission();

        new AddressResolver(this).resolveAsync(location.getLatitude(), location.getLongitude(),
                Locale.forLanguageTag("tr"), address -> {
                    if (isFinishing() || isDestroyed() || address == null) {
                        return;
                    }
                    String city = address.getAdminArea();
                    if (city == null || city.isEmpty()) {
                        return;
                    }
                    locationTextView.setText(city);

                    // Şehir bilgisini kaydet
                    locationPrefs.edit().putString("city_name", city).apply();
                    Log.d("MainActivity", "Şehir bilgisi kaydedildi: " + city);

                    // Namaz vakitlerini al ve alarmları kur
                    fetchPrayerTimesNoAPI(city);
                });
    }

    private void fetchPrayerTimesNoAPI(String city) {
        if (City.fromCityName(city) == null) {
            Toast.makeText(this, city + " Şehiri için Namaz vakitleri bulunamadı. Sorun devam ederse bizimle iletişime geçiniz.", Toast.LENGTH_LONG).show();
            return;
        }
        PrayerTimesFetcher prayerTimesFetcher = new PrayerTimesFetcher(this, imsakTime, gunesTime, ogleTime, ikindiTime, aksamTime, yatsiTime, imsakTimeLayout, gunesTimeLayout, ogleTimeLayout, ikindiTimeLayout, aksamTimeLayout, yatsiTimeLayout, locationRefreshLayout, kalanSureTextView, remain_time_layout, toolbarKalanSureLabel);
        boolean vakitVarmi = prayerTimesFetcher.fetchPrayerTimesNoAPI(city);
        if (!vakitVarmi) {
            Toast.makeText(this, city + " Şehiri için Namaz vakitleri bulunamadı. Sorun devam ederse bizimle iletişime geçiniz.", Toast.LENGTH_LONG).show();
            return;
        }
        VakitNoApi vakitNoApi = new VakitNoApi();
        NamazVakiti namazVakiti = vakitNoApi.localPrayerTimes(Objects.requireNonNull(City.fromCityName(city)).getLatitude(), Objects.requireNonNull(City.fromCityName(city)).getLongitude());
        setupPrayerAlarms(namazVakiti);
    }

    private void setupPrayerAlarms(NamazVakiti namazVakiti) {
        // DailyPrayerWorker'ı başlat ve namaz vakitlerini ilet
        Intent intent = new Intent(this, DailyPrayerWorkerReceiver.class);

        // Namaz vakitlerini intent'e ekle
        intent.putExtra("imsak", namazVakiti.getImsak());
        intent.putExtra("gunes", namazVakiti.getGunes());
        intent.putExtra("ogle", namazVakiti.getOgle());
        intent.putExtra("ikindi", namazVakiti.getIkindi());
        intent.putExtra("aksam", namazVakiti.getAksam());
        intent.putExtra("yatsi", namazVakiti.getYatsi());

        sendBroadcast(intent);
        Log.d("MainActivity", "Alarmlar kurulmak üzere DailyPrayerWorker başlatıldı - Vakitler iletildi");
    }

    // Son bilinen konumu SharedPreferences'a kaydet
    private void saveLastKnownLocation(Location location) {
        SharedPreferences.Editor editor = locationPrefs.edit();
        editor.putFloat(LAST_KNOWN_LAT_KEY, (float) location.getLatitude());
        editor.putFloat(LAST_KNOWN_LON_KEY, (float) location.getLongitude());
        editor.apply();
    }

    // Son bilinen konumu SharedPreferences'dan yükle
    private Location loadLastKnownLocation() {
        float lat = locationPrefs.getFloat(LAST_KNOWN_LAT_KEY, Float.NaN);
        float lon = locationPrefs.getFloat(LAST_KNOWN_LON_KEY, Float.NaN);
        if (!Float.isNaN(lat) && !Float.isNaN(lon)) {
            Location location = new Location("");
            location.setLatitude(lat);
            location.setLongitude(lon);
            return location;
        }
        return null;
    }

    // İzin sonuçlarını işleme
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Kullanıcı izni verdi, bildirimleri başlatabilirsiniz
                Log.d("Permission", "Bildirim izni verildi");
            } else {
                // İzin reddedildi
                Toast.makeText(this, R.string.suggestion_location_permit, Toast.LENGTH_SHORT).show();

            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // İzin verilmemişse, izni iste
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    private void showEditDialog() {
        final EditText editText = new EditText(this);
        editText.setText(usernameTextView.getText().toString());

        // Tek satırda yazma ve maksimum karakter sayısını ayarlama
        editText.setSingleLine(true); // Tek satırda yazılmasını sağlar
        editText.setInputType(InputType.TYPE_CLASS_TEXT); // Metin girişi için uygun tür
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)}); // Maksimum 50 karakter

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.username_edit))
                .setView(editText)
                .setPositiveButton(getString(R.string.save), (dialog, which) -> {
                    String newUsername = editText.getText().toString().trim(); // Boşlukları temizle
                    if (newUsername.isEmpty()) {
                        Toast.makeText(MainActivity.this, R.string.unvalid_username, Toast.LENGTH_SHORT).show();
                    } else {
                        usernameTextView.setText(newUsername);

                        // SharedPreferences'a kaydet
                        SharedPreferences.Editor editor = userPrefs.edit();
                        editor.putString(USERNAME_KEY, newUsername);
                        editor.apply();

                        // Kullanıcıya bilgi ver
                        Toast.makeText(MainActivity.this, getString(R.string.valid_username_1) + " " + newUsername, Toast.LENGTH_SHORT).show();
                        Toast.makeText(MainActivity.this, getString(R.string.valid_username_2), Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    // Menü oluşturma
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // Navigasyon desteği
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this,
                R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // Modern API kullanımı (SDK 27 ve üstü)
        Network network = cm.getActiveNetwork();
        if (network != null) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            boolean isConnected = capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

            if (!isConnected) {
                locationRefreshLayout.setVisibility(View.VISIBLE);
                startActivity(new Intent(this, NetworkCheckActivity.class));
                Toast.makeText(this, R.string.no_Internet, Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            // Eğer aktif ağ yoksa
            locationRefreshLayout.setVisibility(View.VISIBLE);
            startActivity(new Intent(this, NetworkCheckActivity.class));
            Toast.makeText(this, R.string.no_Internet, Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    @Override
    public void onProfilePictureSelected(Avatar avatar) {
        soundService.commonButtonEffect();

        // SharedPreferences’a enum adı kaydet
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putString(AVATAR_ICON, avatar.name());
        editor.apply();

        // Avatar göster
        userAvatar.setImageResource(avatar.getResId());
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("Lifecycle", "onPause called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Lifecycle", "onStop called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("Lifecycle", "onDestroy called");
    }

}