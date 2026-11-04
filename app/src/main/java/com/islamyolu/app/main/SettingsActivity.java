package com.islamyolu.app.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import com.islamyolu.app.Service.LocationAccess;
import com.islamyolu.app.Service.LocationService;
import com.islamyolu.app.Service.PrayerNotificationService;
import com.islamyolu.app.Service.SharedPrefsHelperService;
import com.islamyolu.app.VakitApi.City;
import com.islamyolu.app.VakitApi.DailyPrayerWorkerReceiver;
import com.islamyolu.app.feedback.BilgilendirmeActivity;
import com.islamyolu.app.feedback.FeedbackActivity;
import com.islamyolu.app.feedback.PrivacyActivity;
import com.islamyolu.app.prayertimes.PrayerCitySelection;
import com.islamyolu.app.prayertimes.ui.CitySelectionActivity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {
    private SwitchCompat switchNotificationAll, switchNotificationImsak, switchNotificationSabah, switchNotificationOgle, switchNotificationIkindi, switchNotificationAksam, switchNotificationYatsi, switchNotificationKerahat, switchNotificationSound, switchNotificationVakitHerzamanGoster, switchVakitSound;
    private SharedPrefsHelperService sharedPrefsHelperService;
    private RadioGroup radioGroupVakit;
    private Spinner spinnerEzanSound, spinnerEzanDua;
    private Button contactButton, privacyButton, bilgilendirmeButton, settingLocationButton, selectPrayerCityButton;
    private boolean isUserAction = true; // Kullanıcı aksiyonlarını takip etmek için flag
    private TextView locationInfo, prayerCityInfo;
    private static final String TAG = "SettingsActivity";


    private final Map<Integer, String> switchKeyMap = new HashMap<Integer, String>() {{
        put(R.id.switchNotification_Imsak, "switchNotificationImsak");
        put(R.id.switchNotification_Sabah, "switchNotificationSabah");
        put(R.id.switchNotification_Ogle, "switchNotificationOgle");
        put(R.id.switchNotification_Ikindi, "switchNotificationIkindi");
        put(R.id.switchNotification_Aksam, "switchNotificationAksam");
        put(R.id.switchNotification_Yatsi, "switchNotificationYatsi");
        put(R.id.switchNotification_VakitHerzamanGoster, "switchNotificationVakitHerzamanGoster");
        put(R.id.switchNotification_Kerahat, "switchNotificationKerahat");
        put(R.id.switchNotification_Sound, "switchNotificationSound");
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
        boolean vakitSoundEnabled = sharedPrefsHelperService.isVakitSoundModeEnabled();
        Log.d(TAG, "Vakit ses modu switch durumu yükleniyor: " + vakitSoundEnabled);
        switchVakitSound.setChecked(vakitSoundEnabled);

        // Switch listener'ı ekle
        switchVakitSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Vakit ses modu switch değişti: " + isChecked);
            sharedPrefsHelperService.setVakitSoundModeEnabled(isChecked);
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

        settingLocationButton.setOnClickListener(v -> {
            locationInfo.setText(getString(R.string.loadingProcess));
            settingLocationButton.setEnabled(false);
            // 5 saniye sonra tekrar etkinleştir

            new Handler().postDelayed(() -> {
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
                    sharedPrefsHelperService.setCityNameFromPreferences(cityName);
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
        PrayerCitySelection selection = sharedPrefsHelperService.getPrayerCitySelection();
        prayerCityInfo.setText(selection != null
                ? getString(R.string.prayer_city_current, selection.getDisplayName())
                : getString(R.string.prayer_city_not_selected));
    }

    private void changeRadioButton(int checkedId) {
        int previousValue = sharedPrefsHelperService.getRadioButtonState("radioGroupVakit");
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
            sharedPrefsHelperService.setRadioButtonState("radioGroupVakit", newValue);

            // DailyPrayerWorker günün alarmlarını önce iptal edip yeni süreyle yeniden kurar
            setupNewPrayerAlarms();
        }
    }

    private void setupNewPrayerAlarms() {
        // DailyPrayerWorker'ı tetikle
        Intent intent = new Intent(this, DailyPrayerWorkerReceiver.class);
        sendBroadcast(intent);

        Log.d("SettingsActivity", "Prayer alarms updated with new timing: " +
                sharedPrefsHelperService.getRadioButtonState("radioGroupVakit") + " minutes");
    }

    private void initializeViews() {
        sharedPrefsHelperService = new SharedPrefsHelperService(this, MainActivity.SETTINGS_PREFS);
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
        locationInfo.setText(getString(R.string.locaitonInfoText, sharedPrefsHelperService.getCityNameFromPreferences()));

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
            sharedPrefsHelperService.setSwitchState("switchNotificationAll", isChecked);
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
                sharedPrefsHelperService.setSwitchState(switchKey, isChecked);
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
            boolean allEnabled = sharedPrefsHelperService.getSwitchState("switchNotificationAll");
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
            sharedPrefsHelperService.setSwitchState(switchKey, isEnabled);
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
        sharedPrefsHelperService.setSwitchState("switchNotificationImsak", isChecked);
        sharedPrefsHelperService.setSwitchState("switchNotificationSabah", isChecked);
        sharedPrefsHelperService.setSwitchState("switchNotificationOgle", isChecked);
        sharedPrefsHelperService.setSwitchState("switchNotificationIkindi", isChecked);
        sharedPrefsHelperService.setSwitchState("switchNotificationAksam", isChecked);
        sharedPrefsHelperService.setSwitchState("switchNotificationYatsi", isChecked);

    }

    private void loadSwitchStates() {
        switchNotificationAll.setChecked(sharedPrefsHelperService.getSwitchState("switchNotificationAll"));
        switchNotificationImsak.setChecked(sharedPrefsHelperService.getSwitchState("switchNotificationImsak"));
        switchNotificationSabah.setChecked(sharedPrefsHelperService.getSwitchState("switchNotificationSabah"));
        switchNotificationOgle.setChecked(sharedPrefsHelperService.getSwitchState("switchNotificationOgle"));
        switchNotificationIkindi.setChecked(sharedPrefsHelperService.getSwitchState("switchNotificationIkindi"));
        switchNotificationAksam.setChecked(sharedPrefsHelperService.getSwitchState("switchNotificationAksam"));
        switchNotificationYatsi.setChecked(sharedPrefsHelperService.getSwitchState("switchNotificationYatsi"));
        switchNotificationVakitHerzamanGoster.setChecked(sharedPrefsHelperService.getSwitchState("switchNotificationVakitHerzamanGoster"));

        if (sharedPrefsHelperService.getRadioButtonState("radioGroupVakit") == 5)
            radioGroupVakit.check(R.id.radioButtonVakitBes);
        else if (sharedPrefsHelperService.getRadioButtonState("radioGroupVakit") == 10)
            radioGroupVakit.check(R.id.radioButtonVakitOn);
        else if (sharedPrefsHelperService.getRadioButtonState("radioGroupVakit") == 20)
            radioGroupVakit.check(R.id.radioButtonVakitYirmi);

        switchNotificationKerahat.setChecked(sharedPrefsHelperService.getSwitchState("switchNotificationKerahat"));
        switchNotificationSound.setChecked(sharedPrefsHelperService.getSwitchState("switchNotificationSound"));

        // Spinner durumlarını yükle
        int ezanSoundPosition = sharedPrefsHelperService.getSpinnerSelection("spinner_ezan_sound", 3);
        int ezanDuaPosition = sharedPrefsHelperService.getSpinnerSelection("spinner_ezan_dua", 2);

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
        sharedPrefsHelperService.setSwitchState("switchNotificationVakitHerzamanGoster", isChecked);
        if (isChecked && sharedPrefsHelperService.getPrayerCitySelection() == null) {
            // Vakitler Room'daki seçili şehirden gelir; şehir seçilmeden kalıcı bildirim gösterilemez
            new AlertDialog.Builder(this)
                    .setTitle(R.string.uyariLabel)
                    .setMessage(R.string.prayer_status_no_city)
                    .setPositiveButton(R.string.tamam, null)
                    .show();
            sharedPrefsHelperService.setSwitchState("switchNotificationVakitHerzamanGoster", false);
            switchNotificationVakitHerzamanGoster.setChecked(false);
            return;
        }
        Intent serviceIntent = new Intent(this, PrayerNotificationService.class);
        if (isChecked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 15 = API 34
                new AlertDialog.Builder(this)
                        .setTitle("Dikkat")
                        .setMessage("Android 15 ve üzeri cihazlarda bu hizmet cihaz yeniden başladığında otomatik olarak çalışmaz. Cihaz yeniden başladığında bu servisi manuel olarak yeniden etkinleştirmeniz gerekir.")
                        .setPositiveButton("Tamam", null)
                        .show();
            }
            startService(serviceIntent);
        } else {
            stopService(serviceIntent);
        }
    }

    private void setupSpinners() {
        // Ezan sesi spinner'ı için adapter
        ArrayAdapter<CharSequence> ezanSoundAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Ezan", "Kısa ezan", "Muhabbet", "Kısa Bildirim", "Kapalı"});
        ezanSoundAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEzanSound.setAdapter(ezanSoundAdapter);

        // Ezan duası spinner'ı için adapter
        ArrayAdapter<CharSequence> ezanDuaAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Ezan dua", "Ezan dua ve meal", "Kapalı"});
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
                sharedPrefsHelperService.setSpinnerSelection("spinner_ezan_sound", position);
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
                sharedPrefsHelperService.setSpinnerSelection("spinner_ezan_dua", position);
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

