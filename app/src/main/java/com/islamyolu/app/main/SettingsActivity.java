package com.islamyolu.app.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.islamyolu.app.R;
import com.islamyolu.app.Service.AddressResolver;
import com.islamyolu.app.Service.BatteryOptimization;
import com.islamyolu.app.Service.LocationAccess;
import com.islamyolu.app.Service.LocationService;
import com.islamyolu.app.Service.PrayerNotificationService;

import com.islamyolu.app.VakitApi.City;
import com.islamyolu.app.VakitApi.DailyPrayerWorkerReceiver;
import com.islamyolu.app.feedback.BilgilendirmeActivity;
import com.islamyolu.app.feedback.FeedbackActivity;
import com.islamyolu.app.feedback.PrivacyActivity;
import com.islamyolu.app.prayertimes.PrayerCitySelection;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.LocationPreferences;
import com.islamyolu.app.preferences.SettingsPreferences;
import com.islamyolu.app.prayertimes.ui.CitySelectionActivity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {
    private SwitchCompat switchNotificationAll, switchNotificationImsak, switchNotificationSabah, switchNotificationOgle, switchNotificationIkindi, switchNotificationAksam, switchNotificationYatsi, switchNotificationKerahat, switchNotificationSound, switchNotificationVakitHerzamanGoster, switchVakitSound;
    private SettingsPreferences settings;
    private LocationPreferences locationPrefs;
    private RadioGroup radioGroupVakit;
    private Spinner spinnerEzanSound, spinnerEzanDua;
    private Button contactButton, privacyButton, bilgilendirmeButton, settingLocationButton, selectPrayerCityButton, batteryOptimizationButton;
    private boolean isUserAction = true; // Kullanıcı aksiyonlarını takip etmek için flag
    private TextView locationInfo, prayerCityInfo, batteryOptimizationInfo;
    private static final String TAG = "SettingsActivity";


    private final Map<Integer, String> switchKeyMap = new HashMap<Integer, String>() {{
        put(R.id.switchNotification_Imsak, SettingsPreferences.KEY_NOTIFY_IMSAK);
        put(R.id.switchNotification_Sabah, SettingsPreferences.KEY_NOTIFY_SABAH);
        put(R.id.switchNotification_Ogle, SettingsPreferences.KEY_NOTIFY_OGLE);
        put(R.id.switchNotification_Ikindi, SettingsPreferences.KEY_NOTIFY_IKINDI);
        put(R.id.switchNotification_Aksam, SettingsPreferences.KEY_NOTIFY_AKSAM);
        put(R.id.switchNotification_Yatsi, SettingsPreferences.KEY_NOTIFY_YATSI);
        put(R.id.switchNotification_VakitHerzamanGoster, SettingsPreferences.KEY_ALWAYS_SHOW);
        put(R.id.switchNotification_Kerahat, SettingsPreferences.KEY_KERAHAT);
        put(R.id.switchNotification_Sound, SettingsPreferences.KEY_SOUND);
    }};

    private static final int REQUEST_NOTIFICATION_PERMISSION = 123;

    /** İzin, konum açık/kapalı ve konum alma merkezi LocationAccess'te */
    private final LocationAccess locationAccess = LocationAccess.register(this, new LocationAccess.Options(),
            new LocationAccess.Callback() {
                @Override
                public void onLocation(@NonNull Location location) {
                    onSettingsLocation(location);
                }

                @Override
                public void onUnavailable(@NonNull LocationService.Unavailable reason) {
                    showCityNotFound();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeViews();
        switchCompatClickListeners();
        setupSpinners();
        loadSwitchStates();
        radioGroupVakit.setOnCheckedChangeListener((group, checkedId) -> changeRadioButton(checkedId));

        // Switch durumunu yükle
        boolean vakitSoundEnabled = settings.isVakitSoundModeOn();
        Log.d(TAG, "Vakit ses modu switch durumu yükleniyor: " + vakitSoundEnabled);
        switchVakitSound.setChecked(vakitSoundEnabled);

        // Switch listener'ı ekle
        switchVakitSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Vakit ses modu switch değişti: " + isChecked);
            settings.setVakitSoundMode(isChecked);
        });

        contactButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, FeedbackActivity.class);
            startActivity(intent);
        });

        privacyButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, PrivacyActivity.class);
            startActivity(intent);
        });

        bilgilendirmeButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, BilgilendirmeActivity.class);
            startActivity(intent);
        });

        // Adım adım seçim açılır; konum yalnızca ekrandaki "Mevcut konumumu bul" ile aranır
        selectPrayerCityButton.setOnClickListener(v ->
                startActivity(new Intent(this, CitySelectionActivity.class)));

        batteryOptimizationButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle(R.string.battery_optimization_dialog_title)
                .setMessage(R.string.battery_optimization_dialog_message)
                .setPositiveButton(R.string.battery_optimization_open, (d, w) -> BatteryOptimization.openSettings(this))
                .setNegativeButton(R.string.cancel, null)
                .show());

        settingLocationButton.setOnClickListener(v -> {
            locationInfo.setText(getString(R.string.loadingProcess));
            settingLocationButton.setEnabled(false);
            // 5 saniye sonra tekrar etkinleştir

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                settingLocationButton.setEnabled(true);
            }, 5000);

            locationAccess.requestSingleLocation(true);
        });
    }

    /**
     * "Güncelle": konumdan il adını bulur (hava durumu bu ili kullanır). Namaz vakitleri artık bu ilden
     * hesaplanmaz; Diyanet vakitleri aşağıdaki "Namaz vakti şehrini seç" ile seçilen yerden gelir.
     */
    private void onSettingsLocation(Location location) {
        new AddressResolver(this).resolveAsync(location.getLatitude(), location.getLongitude(),
                Locale.forLanguageTag("tr"), address -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    String cityName = address != null ? address.getAdminArea() : null;
                    if (cityName == null || City.fromCityName(cityName) == null) {
                        showCityNotFound();
                        return;
                    }
                    locationPrefs.setLegacyCityName(cityName);
                    locationInfo.setText(getString(R.string.locaitonInfoText, cityName));
                    settingLocationButton.setEnabled(true);
                });
    }

    private void showCityNotFound() {
        locationInfo.setText(getString(R.string.sehirBulunamadi));
        settingLocationButton.setEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Şehir seçim ekranından dönüldüğünde güncel seçimi göster
        PrayerCitySelection selection = locationPrefs.getPrayerCitySelection();
        prayerCityInfo.setText(selection != null
                ? getString(R.string.prayer_city_current, selection.getDisplayName())
                : getString(R.string.prayer_city_not_selected));
        // Ayar ekranından dönüldüğünde durum yenilensin
        batteryOptimizationInfo.setText(BatteryOptimization.isIgnoring(this)
                ? R.string.battery_optimization_unrestricted
                : R.string.battery_optimization_restricted);
    }

    private void changeRadioButton(int checkedId) {
        int previousValue = settings.getReminderMinutes();
        int newValue;

        if (checkedId == R.id.radioButtonVakitBes) {
            newValue = 5;
        } else if (checkedId == R.id.radioButtonVakitOn) {
            newValue = 10;
        } else if (checkedId == R.id.radioButtonVakitYirmi) {
            newValue = 20;
        } else {
            return;
        }

        // Değer değişmişse alarmları güncelle
        if (previousValue != newValue) {
            // Yeni değeri kaydet
            settings.setReminderMinutes(newValue);

            // DailyPrayerWorker günün alarmlarını önce iptal edip yeni süreyle yeniden kurar
            setupNewPrayerAlarms();
        }
    }

    private void setupNewPrayerAlarms() {
        // DailyPrayerWorker'ı tetikle
        Intent intent = new Intent(this, DailyPrayerWorkerReceiver.class);
        sendBroadcast(intent);

        Log.d("SettingsActivity", "Prayer alarms updated with new timing: " +
                settings.getReminderMinutes() + " minutes");
    }

    private void initializeViews() {
        AppPreferences preferences = AppPreferences.from(this);
        settings = preferences.settings();
        locationPrefs = preferences.location();
        switchNotificationAll = findViewById(R.id.switchNotification_All);
        switchNotificationImsak = findViewById(R.id.switchNotification_Imsak);
        switchNotificationSabah = findViewById(R.id.switchNotification_Sabah);
        switchNotificationOgle = findViewById(R.id.switchNotification_Ogle);
        switchNotificationIkindi = findViewById(R.id.switchNotification_Ikindi);
        switchNotificationAksam = findViewById(R.id.switchNotification_Aksam);
        switchNotificationYatsi = findViewById(R.id.switchNotification_Yatsi);
        switchNotificationKerahat = findViewById(R.id.switchNotification_Kerahat);
        switchNotificationSound = findViewById(R.id.switchNotification_Sound);
        radioGroupVakit = findViewById(R.id.radioGroupVakit);
        switchNotificationVakitHerzamanGoster = findViewById(R.id.switchNotification_VakitHerzamanGoster);
        spinnerEzanSound = findViewById(R.id.spinner_ezan_sound);
        spinnerEzanDua = findViewById(R.id.spinner_ezan_dua);
        switchVakitSound = findViewById(R.id.switchNotification_VakitSound);
        contactButton = findViewById(R.id.contactButton);
        privacyButton = findViewById(R.id.privacyButton);
        bilgilendirmeButton = findViewById(R.id.bilgilendirmeButton);
        settingLocationButton = findViewById(R.id.settingLocationButton);
        locationInfo = findViewById(R.id.locationInfo);
        prayerCityInfo = findViewById(R.id.prayerCityInfo);
        selectPrayerCityButton = findViewById(R.id.selectPrayerCityButton);
        batteryOptimizationInfo = findViewById(R.id.batteryOptimizationInfo);
        batteryOptimizationButton = findViewById(R.id.batteryOptimizationButton);
        locationInfo.setText(getString(R.string.locaitonInfoText, locationPrefs.getLegacyCityName()));

    }

    private void switchCompatClickListeners() {
        switchNotificationAll.setOnCheckedChangeListener(this::onSwitchChanged);
        switchNotificationImsak.setOnCheckedChangeListener(this::onSwitchChanged);
        switchNotificationSabah.setOnCheckedChangeListener(this::onSwitchChanged);
        switchNotificationOgle.setOnCheckedChangeListener(this::onSwitchChanged);
        switchNotificationIkindi.setOnCheckedChangeListener(this::onSwitchChanged);
        switchNotificationAksam.setOnCheckedChangeListener(this::onSwitchChanged);
        switchNotificationYatsi.setOnCheckedChangeListener(this::onSwitchChanged);
        switchNotificationKerahat.setOnCheckedChangeListener(this::onSwitchChanged);
        switchNotificationSound.setOnCheckedChangeListener(this::onSwitchChanged);
        switchNotificationVakitHerzamanGoster.setOnCheckedChangeListener(this::onSwitchChanged);
    }

    private void onSwitchChanged(CompoundButton buttonView, boolean isChecked) {
        // Switch'in programatik değişimlerini ignore et
        if (!buttonView.isPressed()) {
            return;
        }

        int id = buttonView.getId();

        // Bildirim izni kontrolü
        if (isChecked && !hasNotificationPermission()) {
            buttonView.setChecked(false);
            requestNotificationPermission();
            return;
        }
        if (id == R.id.switchNotification_VakitHerzamanGoster) {
            handlePermanentNotificationSwitch(isChecked);
            return;
        }
        if (id == R.id.switchNotification_All) {
            settings.setSwitch(SettingsPreferences.KEY_NOTIFY_ALL, isChecked);
            if (isChecked) {
                allNotificationActive();
            } else {
                allNotificationPasive();
            }
            // Tüm alarmları güncelle
            updateAllAlarms();
        } else {
            // Diğer switch'ler için mevcut kontroller...
            // Switch durumunu kaydet ve ilgili alarmı güncelle
            String switchKey = getSwitchKey(id);
            if (switchKey != null) {
                Log.d("SettingsActivity", switchKey + " switch değişti: " + isChecked);
                settings.setSwitch(switchKey, isChecked);
                updateAlarmForSwitch(switchKey, isChecked); // Sadece değişen switch için alarm güncelle
            }
        }


    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        // Android 13'den düşük versiyonlarda bildirim izni otomatik verilir
        return true;
    }

    private void updateAllAlarms() {
        try {
            // Tüm switch durumlarını kontrol et ve güncelle
            boolean allEnabled = settings.isSwitchOn(SettingsPreferences.KEY_NOTIFY_ALL);
            Log.d("SettingsActivity", "Tüm bildirimler durumu: " + allEnabled);

            // DailyPrayerWorker'ı tetikle
            Intent intent = new Intent(this, DailyPrayerWorkerReceiver.class);
            sendBroadcast(intent);
            Log.d("SettingsActivity", "DailyPrayerWorker tetiklendi - Tüm alarmlar güncellenecek");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Toplu alarm güncelleme hatası: " + e.getMessage());
        }
    }

    private void updateAlarmForSwitch(String switchKey, boolean isEnabled) {
        try {
            // SharedPreferences'a kaydet
            settings.setSwitch(switchKey, isEnabled);
            Log.d("SettingsActivity", "Switch durumu güncellendi: " + switchKey + " = " + isEnabled);

            // DailyPrayerWorker'ı tetikle
            Intent intent = new Intent(this, DailyPrayerWorkerReceiver.class);
            sendBroadcast(intent);
            Log.d("SettingsActivity", "DailyPrayerWorker tetiklendi - Alarmlar güncellenecek");
        } catch (Exception e) {
            Log.e("SettingsActivity", "Alarm güncelleme hatası: " + e.getMessage());
        }
    }

    private String getSwitchKey(int id) {
        return switchKeyMap.get(id);
    }

    private void allNotificationActive() {
        switchNotificationImsak.setChecked(true);
        switchNotificationSabah.setChecked(true);
        switchNotificationOgle.setChecked(true);
        switchNotificationIkindi.setChecked(true);
        switchNotificationAksam.setChecked(true);
        switchNotificationYatsi.setChecked(true);
        prefenceChange(true);
    }

    private void allNotificationPasive() {
        switchNotificationImsak.setChecked(false);
        switchNotificationSabah.setChecked(false);
        switchNotificationOgle.setChecked(false);
        switchNotificationIkindi.setChecked(false);
        switchNotificationAksam.setChecked(false);
        switchNotificationYatsi.setChecked(false);
        prefenceChange(false);
    }

    private void prefenceChange(boolean isChecked) {
        settings.setSwitch(SettingsPreferences.KEY_NOTIFY_IMSAK, isChecked);
        settings.setSwitch(SettingsPreferences.KEY_NOTIFY_SABAH, isChecked);
        settings.setSwitch(SettingsPreferences.KEY_NOTIFY_OGLE, isChecked);
        settings.setSwitch(SettingsPreferences.KEY_NOTIFY_IKINDI, isChecked);
        settings.setSwitch(SettingsPreferences.KEY_NOTIFY_AKSAM, isChecked);
        settings.setSwitch(SettingsPreferences.KEY_NOTIFY_YATSI, isChecked);

    }

    private void loadSwitchStates() {
        switchNotificationAll.setChecked(settings.isSwitchOn(SettingsPreferences.KEY_NOTIFY_ALL));
        switchNotificationImsak.setChecked(settings.isSwitchOn(SettingsPreferences.KEY_NOTIFY_IMSAK));
        switchNotificationSabah.setChecked(settings.isSwitchOn(SettingsPreferences.KEY_NOTIFY_SABAH));
        switchNotificationOgle.setChecked(settings.isSwitchOn(SettingsPreferences.KEY_NOTIFY_OGLE));
        switchNotificationIkindi.setChecked(settings.isSwitchOn(SettingsPreferences.KEY_NOTIFY_IKINDI));
        switchNotificationAksam.setChecked(settings.isSwitchOn(SettingsPreferences.KEY_NOTIFY_AKSAM));
        switchNotificationYatsi.setChecked(settings.isSwitchOn(SettingsPreferences.KEY_NOTIFY_YATSI));
        switchNotificationVakitHerzamanGoster.setChecked(settings.isSwitchOn(SettingsPreferences.KEY_ALWAYS_SHOW));

        if (settings.getReminderMinutes() == 5)
            radioGroupVakit.check(R.id.radioButtonVakitBes);
        else if (settings.getReminderMinutes() == 10)
            radioGroupVakit.check(R.id.radioButtonVakitOn);
        else if (settings.getReminderMinutes() == 20)
            radioGroupVakit.check(R.id.radioButtonVakitYirmi);

        switchNotificationKerahat.setChecked(settings.isSwitchOn(SettingsPreferences.KEY_KERAHAT));
        switchNotificationSound.setChecked(settings.isSwitchOn(SettingsPreferences.KEY_SOUND));

        // Spinner durumlarını yükle
        int ezanSoundPosition = settings.getEzanSoundType();
        int ezanDuaPosition = settings.getEzanDuaType();

        Log.d("SettingsActivity", "Yüklenen spinner pozisyonları - Ezan: " + ezanSoundPosition + ", Dua: " + ezanDuaPosition);

        // Spinner durumlarını yüklerken programatik değişiklik olduğunu belirt
        isUserAction = false;
        spinnerEzanSound.setSelection(ezanSoundPosition);
        isUserAction = false;
        spinnerEzanDua.setSelection(ezanDuaPosition);
    }

    @SuppressWarnings("UnspecifiedImmutableFlag")
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
                Log.d("SettingsActivity", "Bildirim izni istendi");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("SettingsActivity", "Bildirim izni verildi");
                loadSwitchStates();
            } else {
                Log.d("SettingsActivity", "Bildirim izni reddedildi");
                allNotificationActive();
            }
        }
    }

    private void handlePermanentNotificationSwitch(boolean isChecked) {
        settings.setSwitch(SettingsPreferences.KEY_ALWAYS_SHOW, isChecked);
        if (isChecked && locationPrefs.getPrayerCitySelection() == null) {
            // Vakitler Room'daki seçili şehirden gelir; şehir seçilmeden kalıcı bildirim gösterilemez
            new AlertDialog.Builder(this)
                    .setTitle(R.string.uyariLabel)
                    .setMessage(R.string.prayer_status_no_city)
                    .setPositiveButton(R.string.tamam, null)
                    .show();
            settings.setSwitch(SettingsPreferences.KEY_ALWAYS_SHOW, false);
            switchNotificationVakitHerzamanGoster.setChecked(false);
            return;
        }
        if (isChecked) {
            // Cihaz yeniden başlayınca servis artık kendiliğinden açılır (BootReceiver); uyarıya gerek yok
            PrayerNotificationService.startIfEnabled(this);
        } else {
            stopService(new Intent(this, PrayerNotificationService.class));
        }
    }

    private void setupSpinners() {
        // Ezan sesi spinner'ı için adapter
        ArrayAdapter<CharSequence> ezanSoundAdapter = ArrayAdapter.createFromResource(this,
                R.array.ezan_sound_options, android.R.layout.simple_spinner_item);
        ezanSoundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEzanSound.setAdapter(ezanSoundAdapter);

        // Ezan duası spinner'ı için adapter
        ArrayAdapter<CharSequence> ezanDuaAdapter = ArrayAdapter.createFromResource(this,
                R.array.ezan_dua_options, android.R.layout.simple_spinner_item);
        ezanDuaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEzanDua.setAdapter(ezanDuaAdapter);

        // Spinner değişiklik dinleyicileri
        spinnerEzanSound.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean isUserInteracted = true; // Kullanıcının müdahale ettiğini anlamak için değişken

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isUserInteracted) {
                    isUserInteracted = false;
                    return;
                }

                // Seçimi kaydet
                settings.setEzanSoundType(position);
                Log.d("SettingsActivity", "Ezan sesi seçimi değişti: " + position);

                // Worker ses alarmlarını iptal edip seçime göre yeniden kurar ("Kapalı" ise kurmaz)
                setupNewPrayerAlarms();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }


        });

        spinnerEzanDua.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean isUserInteracted = true; // Kullanıcının müdahale ettiğini anlamak için değişken

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isUserInteracted) {
                    isUserInteracted = false;
                    return;
                }

                // Seçimi kaydet
                settings.setEzanDuaType(position);
                Log.d("SettingsActivity", "Ezan duası seçimi değişti: " + position);

                // Worker ses alarmlarını iptal edip seçime göre yeniden kurar ("Kapalı" ise kurmaz)
                setupNewPrayerAlarms();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }
}

