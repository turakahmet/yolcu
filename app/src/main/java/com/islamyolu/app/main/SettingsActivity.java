package com.islamyolu.app.main;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.islamyolu.app.R;
import com.islamyolu.app.Service.PrayerNotificationService;
import com.islamyolu.app.Service.SharedPrefsHelperService;
import com.islamyolu.app.VakitApi.AlarmReceiver;
import com.islamyolu.app.VakitApi.DailyPrayerWorkerReceiver;
import com.islamyolu.app.VakitApi.NamazVakiti;
import com.islamyolu.app.feedback.BilgilendirmeActivity;
import com.islamyolu.app.feedback.FeedbackActivity;
import com.islamyolu.app.feedback.PrivacyActivity;
import com.islamyolu.app.receiver.PrayerSoundReceiver;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {
    private SwitchCompat switchNotificationAll, switchNotificationImsak, switchNotificationSabah, switchNotificationOgle, switchNotificationIkindi, switchNotificationAksam, switchNotificationYatsi, switchNotificationKerahat, switchNotificationSound, switchNotificationVakitHerzamanGoster, switchVakitSound;
    private SharedPrefsHelperService sharedPrefsHelperService;
    private RadioGroup radioGroupVakit;
    private Spinner spinnerEzanSound, spinnerEzanDua;
    private Button contactButton, privacyButton, bilgilendirmeButton;
    private boolean isUserAction = true; // Kullanıcı aksiyonlarını takip etmek için flag

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
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeViews();
        sharedPrefsHelperService = new SharedPrefsHelperService(this, MainActivity.SETTINGS_PREFS);
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

            // Mevcut tüm alarmları iptal et
            cancelAllPrayerAlarms();

            // Yeni alarmları kur
            setupNewPrayerAlarms();
        }
    }

    private void cancelAllPrayerAlarms() {
        // Tüm namaz vakitleri için alarmları iptal et
        if (NamazVakiti.getInstance().getImsak() != null) {
            cancelNotification(this.getString(R.string.namaz_vakit_notification_header),
                    this.getString(R.string.imsak_vakit_notification_body));
        }
        if (NamazVakiti.getInstance().getGunes() != null) {
            cancelNotification(this.getString(R.string.namaz_vakit_notification_header),
                    this.getString(R.string.gunes_vakit_notification_body));
        }
        if (NamazVakiti.getInstance().getOgle() != null) {
            cancelNotification(this.getString(R.string.namaz_vakit_notification_header),
                    this.getString(R.string.ogle_vakit_notification_body));
        }
        if (NamazVakiti.getInstance().getIkindi() != null) {
            cancelNotification(this.getString(R.string.namaz_vakit_notification_header),
                    this.getString(R.string.ikindi_vakit_notification_body));
        }
        if (NamazVakiti.getInstance().getAksam() != null) {
            cancelNotification(this.getString(R.string.namaz_vakit_notification_header),
                    this.getString(R.string.aksam_vakit_notification_body));
        }
        if (NamazVakiti.getInstance().getYatsi() != null) {
            cancelNotification(this.getString(R.string.namaz_vakit_notification_header),
                    this.getString(R.string.yatsi_vakit_notification_body));
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

    private void cancelNotification(String title, String message) {
        Log.d("AlarmReceiver", "Alarm is being canceled!" + message);

        AlarmManager alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e("AlarmReceiver", "AlarmManager is null!");
            return;
        }

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setAction("package com.myapp.yolcu.ACTION_ALARM_NOTIFICATION");
        intent.putExtra("title", title);
        message = message.replace("##", String.valueOf(sharedPrefsHelperService.getRadioButtonState("radioGroupVakit")));
        intent.putExtra("message", message);

        // Alarm oluştururken kullanılan requestCode'u kullanın.
        int requestCode = title.hashCode() + message.hashCode(); // Benzersiz bir requestCode oluşturabilirsiniz.
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Alarmı iptal et
        alarmManager.cancel(pendingIntent);
        Log.d("AlarmReceiver", "Alarm canceled.");
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

        // Eğer her iki ses de kapalıysa alarmları iptal et
        if (ezanSoundPosition == 3 && ezanDuaPosition == 2) {
            cancelAllPrayerSoundAlarms();
        }
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

        Intent serviceIntent = new Intent(this, PrayerNotificationService.class);
        if (isChecked) {
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

                // Eğer "Kapalı" seçildiyse ve dua da kapalıysa alarmları iptal et
                if (position == 4 && sharedPrefsHelperService.getSpinnerSelection("spinner_ezan_sound", 3) == 4) {
                    cancelAllPrayerSoundAlarms();
                } else {
                    // Değilse alarmları güncelle
                    setupNewPrayerAlarms();
                }
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

                // Eğer "Kapalı" seçildiyse ve ezan da kapalıysa alarmları iptal et
                if (position == 2 && sharedPrefsHelperService.getSpinnerSelection("spinner_ezan_dua", 2) == 2) {
                    cancelAllPrayerSoundAlarms();
                } else {
                    // Değilse alarmları güncelle
                    setupNewPrayerAlarms();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void cancelAllPrayerSoundAlarms() {
        Log.d("SettingsActivity", "Tüm ezan sesi alarmları iptal ediliyor");

        // Tüm namaz vakitleri için ses alarmlarını iptal et
        if (NamazVakiti.getInstance().getImsak() != null) {
            cancelPrayerSoundAlarm("İmsak");
        }
        if (NamazVakiti.getInstance().getGunes() != null) {
            cancelPrayerSoundAlarm("Güneş");
        }
        if (NamazVakiti.getInstance().getOgle() != null) {
            cancelPrayerSoundAlarm("Öğle");
        }
        if (NamazVakiti.getInstance().getIkindi() != null) {
            cancelPrayerSoundAlarm("İkindi");
        }
        if (NamazVakiti.getInstance().getAksam() != null) {
            cancelPrayerSoundAlarm("Akşam");
        }
        if (NamazVakiti.getInstance().getYatsi() != null) {
            cancelPrayerSoundAlarm("Yatsı");
        }

    }

    private void cancelPrayerSoundAlarm(String prayerName) {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) return;

            Intent intent = new Intent(this, PrayerSoundReceiver.class);
            int requestCode = ("sound_" + prayerName + Calendar.getInstance().get(Calendar.DAY_OF_YEAR)).hashCode();

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.cancel(pendingIntent);
            Log.d("SettingsActivity", prayerName + " vakti için ses alarmı iptal edildi");

        } catch (Exception e) {
            Log.e("SettingsActivity", prayerName + " vakti için ses alarmı iptal edilirken hata: " + e.getMessage());
        }
    }
}

