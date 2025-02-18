package com.myapp.yolcu.ui.main;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.myapp.yolcu.R;
import com.myapp.yolcu.databinding.ActivityMainBinding;
import com.myapp.yolcu.ui.Service.BadgeService;
import com.myapp.yolcu.ui.Service.SharedPrefsHelperService;
import com.myapp.yolcu.ui.VakitApi.City;
import com.myapp.yolcu.ui.VakitApi.CityUrlMapping;
import com.myapp.yolcu.ui.VakitApi.DailyPrayerWorkerReceiver;
import com.myapp.yolcu.ui.VakitApi.PrayerTimesFetcher;
import com.myapp.yolcu.ui.Service.GoldService;
import com.myapp.yolcu.ui.Service.SoundService;
import com.myapp.yolcu.ui.VakitApi.VakitNoApi;
import com.myapp.yolcu.ui.VakitApi.NamazVakiti;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements ProfilePictureDialogFragment.ProfilePictureDialogListener {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private TextView imsakTime, gunesTime, ogleTime, ikindiTime, aksamTime, yatsiTime, locationTextView, usernameTextView, kalanSureTextView, toolbarKalanSureLabel, total_user_gold, total_rozet;
    private LinearLayout imsakTimeLayout, gunesTimeLayout, ogleTimeLayout, ikindiTimeLayout, aksamTimeLayout, yatsiTimeLayout, locationRefreshLayout, remain_time_layout;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private LocationCallback locationCallback;
    private boolean isLocationFetching = false; // Konum alınıp alınmadığını kontrol etmek için

    // SharedPreferences için tanımlar
    private SharedPreferences locationPrefs;
    private SharedPreferences userPrefs;
    private SharedPreferences settingsPrefs;
    public static SharedPrefsHelperService sharedPrefsHelperService;

    public static final String LOCATION_PREFS = "location_prefs";
    private static final String USER_PREFS = "user_prefs";
    private static final String USERNAME_KEY = "username";
    private static final String LAST_KNOWN_LAT_KEY = "last_known_latitude";
    private static final String LAST_KNOWN_LON_KEY = "last_known_longitude";
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
        locationPrefs = getSharedPreferences(LOCATION_PREFS, Context.MODE_PRIVATE);
        userPrefs = getSharedPreferences(USER_PREFS, Context.MODE_PRIVATE);
        sharedPrefsHelperService = new SharedPrefsHelperService(this, SETTINGS_PREFS);
        sharedPrefsHelperService.defaultSettingPreference();



        // FusedLocationProviderClient başlatma
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        initializeViews();
        initializeServices();
        initializeHeaderView();
        initialGoldAmount();
        checkInternetConnection();

        // Toolbar ve Navigation ayarları
        binding.appBarMain.fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());

        // Refresh butonuna tıklama işlemi
        refreshButton.setOnClickListener(v -> {
            // Konum bilgilerini yenilemek için
            checkInternetConnection();
            try {
                getLastLocation();
            } catch (Exception e) {
                e.printStackTrace(); // Hata ayıklamak için
                // Kullanıcıya bir hata mesajı gösterebilirsiniz
            }
        });
        // Geri tuşu davranışını ayarla
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });


        // Konum izni kontrolü
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // İzin yoksa izin iste
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            // İzin varsa konumu al
            getLastLocation();
        }
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
                R.id.nav_home, R.id.nav_gallery, R.id.nav_compass,R.id.nav_zekat,R.id.nav_muze,R.id.nav_rozet)
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
        total_user_gold=headerView.findViewById(R.id.total_user_gold);
        total_rozet=headerView.findViewById(R.id.total_rozet);


        // Edit icon click listener
        String savedUsername = userPrefs.getString(USERNAME_KEY, getString(R.string.defaultUserName));
        // Kaydedilen kullanıcı adını TextView'e ata
        usernameTextView.setText(savedUsername);
        // Daha önce kaydedilmiş resmi yükle
        int savedPicture = userPrefs.getInt("profile_picture", R.drawable.man_1); // default_avatar varsayılan resim
        userAvatar.setImageResource(savedPicture);

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


    // Son konumu al veya SharedPreferences'dan yükle
    private void getLastLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // İzin yoksa izin iste
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }

        // Son bilinen konumu al
        fusedLocationClient.getLastLocation().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Location locationResult = task.getResult();
                saveLastKnownLocation(locationResult); // Konumu kaydet
                handleNewLocation(locationResult);
            } else {
                // Konum alınamadı, SharedPreferences'dan son konumu al
                Location savedLocation = loadLastKnownLocation();
                if (savedLocation != null) {
                    handleNewLocation(savedLocation);
                } else {
                    // Son konum yok, konum güncellemelerini başlat
                    locationRefreshLayout.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this, R.string.konumMevcutDegil, Toast.LENGTH_SHORT).show();
                    startLocationUpdates();
                }
            }
        });
    }

    // Konum güncellemelerini başlat
    private void startLocationUpdates() {
        if (isLocationFetching) return; // Zaten konum alınıyorsa tekrar başlatma

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // İzin yoksa izin iste
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return;
        }

        isLocationFetching = true;

        // LocationRequest oluşturma (yeni API ile)
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000) // En fazla 5 saniye aralıklarla güncelleme
                .build();

        // Konum güncelleme geri çağrısı
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    saveLastKnownLocation(location); // Konumu kaydet
                    handleNewLocation(location);
                    // Konum bilgisi alındıktan sonra güncellemeleri durdur
                    stopLocationUpdates();
                }
            }
        };

        // Konum güncellemelerini başlat
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    // Konum güncellemelerini durdur
    private void stopLocationUpdates() {
        if (!isLocationFetching) return;

        fusedLocationClient.removeLocationUpdates(locationCallback);
        isLocationFetching = false;
    }

    // Yeni konumu işleme
    private void handleNewLocation(Location location) {
        // İlk açılışta bildirim izni kontrolü
        requestNotificationPermission();
        try {
            Geocoder geocoder = new Geocoder(this, new Locale("tr"));
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getAdminArea();
                
                if (city != null && !city.isEmpty()) {
                    // UI güncelle
                    locationTextView.setText(city);
                    
                    // Şehir bilgisini kaydet
                    locationPrefs.edit().putString("city_name", city).apply();
                    Log.d("MainActivity", "Şehir bilgisi kaydedildi: " + city);

                    // Namaz vakitlerini al ve alarmları kur
                    fetchPrayerTimesNoAPI(city);
                }
            }
        } catch (IOException e) {
            Log.e("MainActivity", "Geocoder hatası: " + e.getMessage());
        }
    }

    private void fetchPrayerTimesNoAPI(String city) {
        PrayerTimesFetcher prayerTimesFetcher = new PrayerTimesFetcher(this, imsakTime, gunesTime, ogleTime, ikindiTime, aksamTime, yatsiTime, imsakTimeLayout, gunesTimeLayout, ogleTimeLayout, ikindiTimeLayout, aksamTimeLayout, yatsiTimeLayout, locationRefreshLayout, kalanSureTextView, remain_time_layout, toolbarKalanSureLabel);
        prayerTimesFetcher.fetchPrayerTimesNoAPI(city);

        VakitNoApi vakitNoApi = new VakitNoApi();
        NamazVakiti namazVakiti = vakitNoApi.localPrayerTimes(Objects.requireNonNull(City.fromCityName(city)).getLatitude(), Objects.requireNonNull(City.fromCityName(city)).getLongitude());
        setupPrayerAlarms(namazVakiti);
    }

    // Namaz vakitlerini çekme
    private void fetchPrayerTimes(String city) {
        CityUrlMapping cityUrlMapping = new CityUrlMapping();
        String url = cityUrlMapping.getUrlForCity(city);
        if (url != null) {
            PrayerTimesFetcher prayerTimesFetcher = new PrayerTimesFetcher(this, imsakTime, gunesTime, ogleTime, ikindiTime, aksamTime, yatsiTime, imsakTimeLayout, gunesTimeLayout, ogleTimeLayout, ikindiTimeLayout, aksamTimeLayout, yatsiTimeLayout, locationRefreshLayout, kalanSureTextView, remain_time_layout, toolbarKalanSureLabel);
            prayerTimesFetcher.fetchPrayerTimes(url);
        } else {
            Log.e("MainActivity", "Namaz vakitleri alınamadı: " + city + " için");
        }
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
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                locationRefreshLayout.setVisibility(View.VISIBLE);
                Toast.makeText(this, R.string.no_permit_location, Toast.LENGTH_SHORT).show();
            }
        }
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
//
//    private void showProfilePictureDialog() {
//        // Resim seçeneklerini tanımla
//        final int[] imageOptions = {R.drawable.man_1, R.id.man_2, R.id.women_1, R.id.women_2};
//        final String[] imageNames = {"Man 1", "Man 2", "Woman 1", "Woman 2"};
//
//        // Görsel seçeneklerini gösteren bir AlertDialog oluştur
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Select Profile Picture")
//                .setItems(imageNames, (dialog, which) -> {
//                    // Seçilen resmi al
//                    int selectedImage = imageOptions[which];
//
//                    // Resmi ShapeableImageView'e ayarla
//                    userAvatar.setImageResource(selectedImage);
//
//                    // SharedPreferences'a kaydet
//                    SharedPreferences.Editor editor = userPrefs.edit();
//                    editor.putInt("profile_picture", selectedImage);
//                    editor.apply();
//                })
//                .show();
//    }

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
        // Menü öğelerine tıklanma işlemleri
        if (item.getItemId() == R.id.action_settings) {
            // Ayarlar ekranını aç
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
    public void onProfilePictureSelected(int resourceId) {
        soundService.commonButtonEffect();
        // Seçilen resmi ayarla
        userAvatar.setImageResource(resourceId);

        // SharedPreferences'a kaydet
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putInt("profile_picture", resourceId);
        editor.apply();
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