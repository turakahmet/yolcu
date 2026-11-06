package com.islamyolu.app.main;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
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
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
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
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.LocationPreferences;
import com.islamyolu.app.preferences.UserPreferences;
import com.islamyolu.app.Service.PrayerNotificationService;
import com.islamyolu.app.sound.AppSound;
import com.islamyolu.app.sound.SoundManager;
import com.islamyolu.app.VakitApi.DailyPrayerWorkerReceiver;
import com.islamyolu.app.databinding.ActivityMainBinding;
import com.islamyolu.app.monthlyCalender.MonthlyPrayerActivity;
import com.islamyolu.app.prayertimes.LegacyCityMigration;
import com.islamyolu.app.prayertimes.PrayerSlot;
import com.islamyolu.app.prayertimes.PrayerSyncResult;
import com.islamyolu.app.prayertimes.ui.CitySelectionActivity;
import com.islamyolu.app.prayertimes.ui.PrayerTimesPanel;
import com.islamyolu.app.prayertimes.ui.PrayerTimesViewModel;
import com.islamyolu.app.profile.Avatar;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    // Yerel tercihler merkezi AppPreferences'tan
    private LocationPreferences locationPrefs;
    private UserPreferences userPrefs;
    private ImageButton refreshButton;
    private ImageView editIcon;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 123;
    private ShapeableImageView userAvatar;
    private SoundManager sounds;
    private BadgeService badgeService;
    private GoldService goldService;

    /** Ana ekran: bugün + yarının imsakı için yeterli gün */
    private static final int MAIN_SCREEN_DAYS_AHEAD = 2;
    private PrayerTimesViewModel prayerTimesViewModel;
    private PrayerTimesPanel prayerTimesPanel;
    @Nullable private PrayerTimesViewModel.State prayerState;
    private boolean alarmsScheduled;
    private boolean legacyMigrationChecked;
    private final ExecutorService background = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Binding işlemi
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Tercihler ve servisler
        AppPreferences preferences = AppPreferences.from(this);
        locationPrefs = preferences.location();
        userPrefs = preferences.user();
        preferences.settings().applyDefaultsOnFirstRun();
        // Kalıcı vakit bildirimi açıksa ve sistem/üretici servisi kapattıysa yeniden başlatılır
        PrayerNotificationService.startIfEnabled(this);

        initializeViews();
        initializeServices();
        initializeHeaderView();
        initialGoldAmount();

        // Namaz vakitleri Room'dan LiveData ile gelir (§5); şehir seçilmemiş/veri bitmişse durum mesajı gösterilir (§6)
        setupCityClick();
        prayerTimesViewModel = new ViewModelProvider(this).get(PrayerTimesViewModel.class);
        prayerTimesViewModel.getState().observe(this, state -> {
            prayerState = state;
            prayerTimesPanel.render(state);
            // Önceki sürümdeki gibi her açılışta alarmlar bir kez yeniden kurulur
            if (state.status == PrayerTimesViewModel.Status.OK && !alarmsScheduled) {
                alarmsScheduled = true;
                setupPrayerAlarms();
            }
            // Önceki sürümden gelen kullanıcı: kayıtlı il yeni şehir yapısına bir kez taşınır (onayla)
            if (state.status == PrayerTimesViewModel.Status.NO_CITY && !legacyMigrationChecked) {
                legacyMigrationChecked = true;
                offerLegacyMigration();
            }
        });

        // Yenile: şehir seçilmemişse seçim ekranı, vakitler güncel değilse indirme yeniden denenir
        refreshButton.setOnClickListener(v -> {
            if (prayerState == null || prayerState.status == PrayerTimesViewModel.Status.NO_CITY) {
                openCitySelection();
            } else {
                prayerTimesViewModel.retrySync();
            }
        });
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
            if (prayerState == null || prayerState.selection == null) {
                Toast.makeText(this, R.string.prayer_monthly_needs_city, Toast.LENGTH_SHORT).show();
                openCitySelection();
                return;
            }
            startActivity(new Intent(MainActivity.this, MonthlyPrayerActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Şehir değişmiş veya gün dönmüş olabilir
        prayerTimesViewModel.refresh(MAIN_SCREEN_DAYS_AHEAD);
    }

    /** Şehir adına dokununca Diyanet şehir seçimi açılır. */
    private void setupCityClick() {
        findViewById(R.id.location_layout).setOnClickListener(v -> openCitySelection());
    }

    private void openCitySelection() {
        startActivity(new Intent(this, CitySelectionActivity.class));
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
        String savedUsername = userPrefs.getUsername(getString(R.string.defaultUserName));
        // Kaydedilen kullanıcı adını TextView'e ata
        usernameTextView.setText(savedUsername);
        // Daha önce kaydedilmiş avatarı yükleme
        String savedName = userPrefs.getAvatarName(Avatar.MAN_1.name());
        Avatar savedAvatar = Avatar.fromName(savedName);
        userAvatar.setImageResource(savedAvatar.getResId());

        editIcon.setOnClickListener(v -> {
            if (savedUsername.equals(getString(R.string.defaultUserName))) {
                usernameTextView.setText("");
            }
            showEditDialog();
        });

        userAvatar.setOnClickListener(v -> {
            sounds.play(AppSound.COMMON_BUTTON);
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
        sounds = SoundManager.from(this);
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

        PrayerTimesPanel.Views panelViews = new PrayerTimesPanel.Views();
        panelViews.times.put(PrayerSlot.IMSAK, imsakTime);
        panelViews.times.put(PrayerSlot.GUNES, gunesTime);
        panelViews.times.put(PrayerSlot.OGLE, ogleTime);
        panelViews.times.put(PrayerSlot.IKINDI, ikindiTime);
        panelViews.times.put(PrayerSlot.AKSAM, aksamTime);
        panelViews.times.put(PrayerSlot.YATSI, yatsiTime);
        panelViews.layouts.put(PrayerSlot.IMSAK, imsakTimeLayout);
        panelViews.layouts.put(PrayerSlot.GUNES, gunesTimeLayout);
        panelViews.layouts.put(PrayerSlot.OGLE, ogleTimeLayout);
        panelViews.layouts.put(PrayerSlot.IKINDI, ikindiTimeLayout);
        panelViews.layouts.put(PrayerSlot.AKSAM, aksamTimeLayout);
        panelViews.layouts.put(PrayerSlot.YATSI, yatsiTimeLayout);
        panelViews.remainingLabel = toolbarKalanSureLabel;
        panelViews.remainingTime = kalanSureTextView;
        panelViews.remainingLayout = remain_time_layout;
        panelViews.cityName = locationTextView;
        panelViews.refreshLayout = locationRefreshLayout;
        prayerTimesPanel = new PrayerTimesPanel(this, panelViews, AppPreferences.from(this).settings(),
                () -> prayerTimesViewModel.refresh(MAIN_SCREEN_DAYS_AHEAD));
    }


    private void initialGoldAmount() {
        goldService = new GoldService(this);
        goldService.setInitialGoldAmount(); // Başlangıç altın miktarını ayarla
//        total_user_gold.setText(String.valueOf(goldService.getGoldAmount()));
    }


    /**
     * Konum artık namaz vakti için kullanılmıyor (vakitler seçili şehrin Room verisinden gelir);
     * yalnızca hava durumu için "city_name" güncellenir. Konum alınamazsa son kayıtlı konum denenir.
     */
    private void onLocationUnavailable(LocationService.Unavailable reason) {
        if (reason == LocationService.Unavailable.PERMISSION_DENIED) {
            Toast.makeText(this, R.string.no_permit_location, Toast.LENGTH_SHORT).show();
            return;
        }
        Location savedLocation = loadLastKnownLocation();
        if (savedLocation != null) {
            handleNewLocation(savedLocation);
        }
    }

    // Yeni konumu işleme: hava durumu için il adı (mevcut il listesiyle eşleşsin diye Türkçe çözümlenir)
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
                    locationPrefs.setLegacyCityName(city);
                    Log.d("MainActivity", "Hava durumu için il kaydedildi");
                });
    }

    /** Eski il kaydı varsa il merkezinin vakitlerini önerir; eşleşmez veya internet yoksa sessiz kalır. */
    private void offerLegacyMigration() {
        background.execute(() -> {
            LegacyCityMigration.Offer offer = LegacyCityMigration.findOffer(getApplicationContext());
            if (offer != null) {
                runOnUiThread(() -> showLegacyMigrationDialog(offer));
            }
        });
    }

    private void showLegacyMigrationDialog(LegacyCityMigration.Offer offer) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.legacy_migration_title)
                .setMessage(getString(R.string.legacy_migration_message, offer.legacyName, offer.selection.getCityName()))
                .setCancelable(false)
                .setPositiveButton(R.string.legacy_migration_yes, (d, w) -> acceptLegacyMigration(offer))
                .setNegativeButton(R.string.legacy_migration_other, (d, w) -> {
                    LegacyCityMigration.decline(getApplicationContext());
                    openCitySelection();
                })
                .show();
    }

    private void acceptLegacyMigration(LegacyCityMigration.Offer offer) {
        Toast.makeText(this, getString(R.string.city_selection_downloading, offer.selection.getCityName()),
                Toast.LENGTH_SHORT).show();
        background.execute(() -> {
            PrayerSyncResult result = LegacyCityMigration.accept(getApplicationContext(), offer);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.isSuccess()) {
                    prayerTimesViewModel.refresh(MAIN_SCREEN_DAYS_AHEAD);
                } else {
                    // İndirilemedi (internet/sunucu): taşıma durumu yazılmadı, sonraki açılışta yeniden sorulur
                    Toast.makeText(this, R.string.legacy_migration_failed, Toast.LENGTH_LONG).show();
                    openCitySelection();
                }
            });
        });
    }

    /** Açılışta bir kez: ezan/bildirim alarmları Room'daki bugünün vakitleriyle kurulur. */
    private void setupPrayerAlarms() {
        sendBroadcast(new Intent(this, DailyPrayerWorkerReceiver.class));
        Log.d("MainActivity", "Alarmlar kurulmak üzere DailyPrayerWorker başlatıldı");
    }

    // Son bilinen konumu kaydet
    private void saveLastKnownLocation(Location location) {
        locationPrefs.saveLastKnownLocation(location.getLatitude(), location.getLongitude());
    }

    // Son bilinen konumu yükle
    private Location loadLastKnownLocation() {
        float lat = locationPrefs.getLastKnownLatitude();
        float lon = locationPrefs.getLastKnownLongitude();
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

                        userPrefs.setUsername(newUsername);

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



    @Override
    public void onProfilePictureSelected(Avatar avatar) {
        sounds.play(AppSound.COMMON_BUTTON);

        userPrefs.setAvatarName(avatar.name());

        // Avatar göster
        userAvatar.setImageResource(avatar.getResId());
    }

    @Override
    protected void onPause() {
        super.onPause();
        prayerTimesPanel.stop();
        Log.d("Lifecycle", "onPause called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Lifecycle", "onStop called");
    }

    @Override
    protected void onDestroy() {
        background.shutdown();
        super.onDestroy();
        Log.d("Lifecycle", "onDestroy called");
    }

}