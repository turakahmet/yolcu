package com.islamyolu.app.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.islamyolu.app.R;
import com.islamyolu.app.Service.BatteryOptimization;
import com.islamyolu.app.Service.PrayerNotificationService;
import com.islamyolu.app.VakitApi.PrayerAlarmScheduler;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.LocationPreferences;
import com.islamyolu.app.preferences.SettingsPreferences;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Namaz bildirimleri, kalıcı vakit bildirimi ve pil optimizasyonu.
 * Davranış eski Ayarlar ekranıyla aynıdır: her değişiklikten sonra alarmlar yeniden kurulur.
 */
public class NotificationSettingsFragment extends Fragment {

    private final NotificationPermissionGate permission = new NotificationPermissionGate(this, this::bindValues);

    private SettingsPreferences settings;
    private LocationPreferences locationPrefs;
    private SettingsRowView rowAll;
    private SettingsRowView rowAlwaysShow;
    private SettingsRowView rowBattery;
    /** Satır → tercih anahtarı (altı vakit) */
    private final Map<SettingsRowView, String> prayerRows = new LinkedHashMap<>();

    public NotificationSettingsFragment() {
        super(R.layout.fragment_settings_notifications);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        AppPreferences preferences = AppPreferences.from(requireContext());
        settings = preferences.settings();
        locationPrefs = preferences.location();

        rowAll = view.findViewById(R.id.rowNotifyAll);
        rowAlwaysShow = view.findViewById(R.id.rowAlwaysShow);
        rowBattery = view.findViewById(R.id.rowBattery);
        prayerRows.clear();
        prayerRows.put(view.findViewById(R.id.rowNotifyImsak), SettingsPreferences.KEY_NOTIFY_IMSAK);
        prayerRows.put(view.findViewById(R.id.rowNotifySabah), SettingsPreferences.KEY_NOTIFY_SABAH);
        prayerRows.put(view.findViewById(R.id.rowNotifyOgle), SettingsPreferences.KEY_NOTIFY_OGLE);
        prayerRows.put(view.findViewById(R.id.rowNotifyIkindi), SettingsPreferences.KEY_NOTIFY_IKINDI);
        prayerRows.put(view.findViewById(R.id.rowNotifyAksam), SettingsPreferences.KEY_NOTIFY_AKSAM);
        prayerRows.put(view.findViewById(R.id.rowNotifyYatsi), SettingsPreferences.KEY_NOTIFY_YATSI);

        rowAll.setOnToggleListener((row, checked) -> onAllToggled(checked));
        for (Map.Entry<SettingsRowView, String> entry : prayerRows.entrySet()) {
            String key = entry.getValue();
            entry.getKey().setOnToggleListener((row, checked) -> onPrayerToggled(row, key, checked));
        }
        rowAlwaysShow.setOnToggleListener((row, checked) -> onAlwaysShowToggled(checked));
        rowBattery.setOnClickListener(v -> showBatteryDialog());

        bindValues();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Pil ayarından dönüldüğünde durum yenilensin
        rowBattery.setSubtitle(BatteryOptimization.isIgnoring(requireContext())
                ? R.string.battery_optimization_unrestricted
                : R.string.battery_optimization_restricted);
    }

    private void bindValues() {
        if (rowAll == null) return; // görünüm henüz yok (izin sonucu geç geldi)
        rowAll.setChecked(settings.isSwitchOn(SettingsPreferences.KEY_NOTIFY_ALL));
        for (Map.Entry<SettingsRowView, String> entry : prayerRows.entrySet()) {
            entry.getKey().setChecked(settings.isSwitchOn(entry.getValue()));
        }
        rowAlwaysShow.setChecked(settings.isSwitchOn(SettingsPreferences.KEY_ALWAYS_SHOW));
    }

    private void onAllToggled(boolean checked) {
        if (checked && !permission.ensureGranted()) {
            rowAll.setChecked(false);
            return;
        }
        settings.setSwitch(SettingsPreferences.KEY_NOTIFY_ALL, checked);
        // "Tüm bildirimler" altı vakti birlikte açar/kapatır (eski davranış)
        for (Map.Entry<SettingsRowView, String> entry : prayerRows.entrySet()) {
            settings.setSwitch(entry.getValue(), checked);
            entry.getKey().setChecked(checked);
        }
        rescheduleAlarms();
    }

    private void onPrayerToggled(SettingsRowView row, String key, boolean checked) {
        if (checked && !permission.ensureGranted()) {
            row.setChecked(false);
            return;
        }
        settings.setSwitch(key, checked);
        rescheduleAlarms();
    }

    private void onAlwaysShowToggled(boolean checked) {
        if (checked && !permission.ensureGranted()) {
            rowAlwaysShow.setChecked(false);
            return;
        }
        if (checked && locationPrefs.getPrayerCitySelection() == null) {
            // Vakitler Room'daki seçili şehirden gelir; şehir seçilmeden kalıcı bildirim gösterilemez
            rowAlwaysShow.setChecked(false);
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.uyariLabel)
                    .setMessage(R.string.prayer_status_no_city)
                    .setPositiveButton(R.string.tamam, null)
                    .show();
            return;
        }
        settings.setSwitch(SettingsPreferences.KEY_ALWAYS_SHOW, checked);
        if (checked) {
            PrayerNotificationService.startIfEnabled(requireContext());
        } else {
            requireContext().stopService(new Intent(requireContext(), PrayerNotificationService.class));
        }
    }

    private void showBatteryDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.battery_optimization_dialog_title)
                .setMessage(R.string.battery_optimization_dialog_message)
                .setPositiveButton(R.string.battery_optimization_open,
                        (dialog, which) -> BatteryOptimization.openSettings(requireActivity()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void rescheduleAlarms() {
        PrayerAlarmScheduler.rescheduleAsync(requireContext(), null);
    }
}
