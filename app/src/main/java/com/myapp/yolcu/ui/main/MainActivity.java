package com.myapp.yolcu.ui.main;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.myapp.yolcu.NetworkCheckActivity;
import com.myapp.yolcu.R;
import com.myapp.yolcu.databinding.ActivityMainBinding;
import com.myapp.yolcu.ui.VakitApi.CityUrlMapping;
import com.myapp.yolcu.ui.VakitApi.PrayerTimesFetcher;
import com.myapp.yolcu.ui.VakitApi.VakitNoApi;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ProfilePictureDialogFragment.ProfilePictureDialogListener {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private TextView imsakTime, gunesTime, ogleTime, ikindiTime, aksamTime, yatsiTime, locationTextView, usernameTextView, kalanSureTextView, toolbarKalanSureLabel;
    private LinearLayout imsakTimeLayout, gunesTimeLayout, ogleTimeLayout, ikindiTimeLayout, aksamTimeLayout, yatsiTimeLayout, locationRefreshLayout, remain_time_layout;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private LocationCallback locationCallback;
    private boolean isLocationFetching = false; // Konum alınıp alınmadığını kontrol etmek için

    // SharedPreferences için tanımlar
    private SharedPreferences locationPrefs;
    private SharedPreferences userPrefs;
    private static final String PREFS_LOCATION_NAME = "location_prefs";
    private static final String PREFS_USER_NAME = "user_prefs";
    private static final String USERNAME_KEY = "username";
    private static final String LAST_KNOWN_LAT_KEY = "last_known_latitude";
    private static final String LAST_KNOWN_LON_KEY = "last_known_longitude";
    private ImageButton refreshButton;
    private ImageView editIcon;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 123;
    private ShapeableImageView userAvatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Binding işlemi
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // SharedPreferences başlatma
        //location
        locationPrefs = getSharedPreferences(PREFS_LOCATION_NAME, Context.MODE_PRIVATE);
        //user
        userPrefs = getSharedPreferences(PREFS_USER_NAME, Context.MODE_PRIVATE);
        // Kullanıcının altın miktarını SharedPreferences ile ayarlama
        setInitialGoldAmount();

        // FusedLocationProviderClient başlatma
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Konum izni kontrolü
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // İzin yoksa izin iste
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            // İzin varsa konumu al
            getLastLocation();
        }
        // Bildirim izni iste
        requestNotificationPermission();

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


        // Toolbar ve Navigation ayarları
        setSupportActionBar(binding.appBarMain.toolbar);
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


        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        checkInternetConnection();

        // NavigationView'in header'ından View'leri bulma
        View headerView = navigationView.getHeaderView(0); // Header layout'a erişim
        usernameTextView = headerView.findViewById(R.id.username); // Username TextView
        editIcon = headerView.findViewById(R.id.editIcon);
        userAvatar = headerView.findViewById(R.id.user_avatar);


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
        editIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (savedUsername.equals(getString(R.string.defaultUserName))) {
                    usernameTextView.setText("");
                }
                showEditDialog();
            }
        });

        userAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfilePictureDialogFragment dialogFragment = new ProfilePictureDialogFragment();
                dialogFragment.show(getSupportFragmentManager(), "profilePictureDialog");
            }
        });


    }

    private void setInitialGoldAmount() {
        SharedPreferences sharedPreferences = getSharedPreferences("GamePreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Eğer daha önce altın miktarı ayarlanmamışsa 500 altın ver
        if (sharedPreferences.getInt("userGold", -1) == -1) {
            editor.putInt("userGold", 5000);
            editor.apply();
        }
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
        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            assert addresses != null;
            if (addresses.size() > 0) {
                String city = addresses.get(0).getAdminArea();
//                fetchPrayerTimes(city); //diyanetten alırsan burası acilacak aşağıdaki kapanacak
                fetchPrayerTimesNoAPI(city);// kendin hesaplarsan burası acilacak yukardaki kapanak
                locationTextView.setText(city); // Şehir adını göster
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.checkLocationAndInternet, Toast.LENGTH_LONG).show();
        }
    }

    private void fetchPrayerTimesNoAPI(String city) {
        PrayerTimesFetcher prayerTimesFetcher = new PrayerTimesFetcher(this, imsakTime, gunesTime, ogleTime, ikindiTime, aksamTime, yatsiTime, imsakTimeLayout, gunesTimeLayout, ogleTimeLayout, ikindiTimeLayout, aksamTimeLayout, yatsiTimeLayout, locationRefreshLayout, kalanSureTextView, remain_time_layout, toolbarKalanSureLabel);
        prayerTimesFetcher.fetchPrayerTimesNoAPI(city);
    }

    // Namaz vakitlerini çekme
    private void fetchPrayerTimes(String city) {
        CityUrlMapping cityUrlMapping = new CityUrlMapping();
        String url = cityUrlMapping.getUrlForCity(city);
        if (url != null) {
            PrayerTimesFetcher prayerTimesFetcher = new PrayerTimesFetcher(this, imsakTime, gunesTime, ogleTime, ikindiTime, aksamTime, yatsiTime, imsakTimeLayout, gunesTimeLayout, ogleTimeLayout, ikindiTimeLayout, aksamTimeLayout, yatsiTimeLayout, locationRefreshLayout, kalanSureTextView, remain_time_layout, toolbarKalanSureLabel);
            prayerTimesFetcher.fetchPrayerTimes(url);
        } else {
            Toast.makeText(this, R.string.no_Location, Toast.LENGTH_SHORT).show();
        }
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
                .setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newUsername = editText.getText().toString().trim(); // Boşlukları temizle
                        if (newUsername.isEmpty()) {
                            Toast.makeText(MainActivity.this, R.string.unvalid_username, Toast.LENGTH_SHORT).show();
                            return;
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
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void showProfilePictureDialog() {
        // Resim seçeneklerini tanımla
        final int[] imageOptions = {R.drawable.man_1, R.drawable.man_2, R.drawable.women_1, R.drawable.women_2};
        final String[] imageNames = {"Man 1", "Man 2", "Woman 1", "Woman 2"};

        // Görsel seçeneklerini gösteren bir AlertDialog oluştur
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Profile Picture")
                .setItems(imageNames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Seçilen resmi al
                        int selectedImage = imageOptions[which];

                        // Resmi ShapeableImageView'e ayarla
                        userAvatar.setImageResource(selectedImage);

                        // SharedPreferences'a kaydet
                        SharedPreferences.Editor editor = userPrefs.edit();
                        editor.putInt("profile_picture", selectedImage);
                        editor.apply();
                    }
                })
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

    private void checkInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            locationRefreshLayout.setVisibility(View.VISIBLE);
            startActivity(new Intent(this, NetworkCheckActivity.class));
            Toast.makeText(this, R.string.no_Internet, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onProfilePictureSelected(int resourceId) {
        // Seçilen resmi ayarla
        userAvatar.setImageResource(resourceId);

        // SharedPreferences'a kaydet
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putInt("profile_picture", resourceId);
        editor.apply();
    }
}