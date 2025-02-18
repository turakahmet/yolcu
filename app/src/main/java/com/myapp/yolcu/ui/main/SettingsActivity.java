package com.myapp.yolcu.ui.main;

import static com.myapp.yolcu.ui.main.MainActivity.sharedPrefsHelperService;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.myapp.yolcu.R;
import com.myapp.yolcu.ui.Service.PrayerNotificationService;
import com.myapp.yolcu.ui.Service.SharedPrefsHelperService;
import com.myapp.yolcu.ui.VakitApi.AlarmReceiver;
import com.myapp.yolcu.ui.VakitApi.DailyPrayerWorker;
import com.myapp.yolcu.ui.VakitApi.DailyPrayerWorkerReceiver;
import com.myapp.yolcu.ui.VakitApi.NamazVakiti;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Calendar;

public class SettingsActivity extends AppCompatActivity {
    private SwitchCompat switchNotificationAll, switchNotificationImsak, switchNotificationSabah, switchNotificationOgle, switchNotificationIkindi, switchNotificationAksam, switchNotificationYatsi, switchNotificationKerahat, switchNotificationSound, switchNotificationVakitHerzamanGoster;
    private SharedPrefsHelperService sharedPrefsHelperService;
    private RadioGroup radioGroupVakit;

    private final Map<Integer, String> switchKeyMap = new HashMap<Integer, String>() {{
        put(R.id.switchNotification_Imsak, "switchNotificationImsak");
        put(R.id.switchNotification_Sabah, "switchNotificationSabah");
        put(R.id.switchNotification_Ogle, "switchNotificationOgle");
        put(R.id.switchNotification_Ikindi, "switchNotificationIkindi");
        put(R.id.switchNotification_Aksam, "switchNotificationAksam");
        put(R.id.switchNotification_Yatsi, "switchNotificationYatsi");
        put(R.id.switchNotification_VakitHerzamanGoster, "switchNotificationVakitHerzamanGoster");
    }};

    private static final int REQUEST_NOTIFICATION_PERMISSION = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initializeViews();
        sharedPrefsHelperService = new SharedPrefsHelperService(this, MainActivity.SETTINGS_PREFS);
        switchCompatClickListeners();
        loadSwitchStates();
        radioGroupVakit.setOnCheckedChangeListener((group, checkedId) -> changeRadioButton(checkedId));

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
        
        Toast.makeText(this, getString(R.string.prayer_times_updated), Toast.LENGTH_SHORT).show();
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
        switchNotificationSound=findViewById(R.id.switchNotification_Sound);
        radioGroupVakit = findViewById(R.id.radioGroupVakit);
        switchNotificationVakitHerzamanGoster=findViewById(R.id.switchNotification_VakitHerzamanGoster);
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

        if(sharedPrefsHelperService.getRadioButtonState("radioGroupVakit")==5)
            radioGroupVakit.check(R.id.radioButtonVakitBes);
        else if(sharedPrefsHelperService.getRadioButtonState("radioGroupVakit")==10)
            radioGroupVakit.check(R.id.radioButtonVakitOn);
        else if(sharedPrefsHelperService.getRadioButtonState("radioGroupVakit")==20)
            radioGroupVakit.check(R.id.radioButtonVakitYirmi);

        switchNotificationKerahat.setChecked(sharedPrefsHelperService.getSwitchState("switchNotificationKerahat"));
        switchNotificationSound.setChecked(sharedPrefsHelperService.getSwitchState("switchNotificationSound"));
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
}

