package com.islamyolu.app.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.islamyolu.app.R;
import com.islamyolu.app.Service.BatteryOptimization;
import com.islamyolu.app.Service.PrayerNotificationService;
import com.islamyolu.app.VakitApi.PrayerAlarmScheduler;
import com.islamyolu.app.VakitApi.PrayerReminderNotification;
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

    /** Deneme bildiriminin sabit kimliği: art arda basılınca tek bildirim güncellenir */
    private static final int TEST_NOTIFICATION_ID = 64301;
    private static final long TEST_NOTIFICATION_TIMEOUT_MS = 60_000;

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
            SettingsRowView row = entry.getKey();
            row.setOnToggleListener((r, checked) -> onPrayerToggled(r, key, checked));
            // Satır: vakte özel ayarlar (vakit öncesi uyarı ve süresi)
            row.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.toPrayer,
                    PrayerSettingsFragment.args(key, row.getTitle().toString())));
        }
        rowAlwaysShow.setOnToggleListener((row, checked) -> onAlwaysShowToggled(checked));
        rowBattery.setOnClickListener(v -> showBatteryDialog());
        view.findViewById(R.id.testNotificationButton).setOnClickListener(v -> sendTestNotification());

        bindValues();
    }

    /** Gerçek vakit bildirimiyle aynı kanal ve görünümde örnek bildirim; sorun varsa nedenini söyler. */
    private void sendTestNotification() {
        if (!permission.ensureGranted()) {
            return; // izin penceresi açıldı; izin verilince kullanıcı tekrar dener
        }
        Context context = requireContext();
        int minutes = settings.getReminderMinutes(SettingsPreferences.KEY_NOTIFY_OGLE); // örnek: öğle
        String message = getString(R.string.settings_test_notification_message,
                getString(R.string.ogle_vakit_notification_body).replace("##", String.valueOf(minutes)));
        PrayerReminderNotification.Result result = PrayerReminderNotification.show(context,
                PrayerReminderNotification.Channel.PRAYER, TEST_NOTIFICATION_ID,
                getString(R.string.namaz_vakit_notification_header), message, TEST_NOTIFICATION_TIMEOUT_MS);

        View root = requireView();
        switch (result) {
            case SHOWN:
                Snackbar.make(root, R.string.settings_test_notification_sent, Snackbar.LENGTH_SHORT).show();
                break;
            case CHANNEL_OFF:
                Snackbar.make(root, R.string.settings_test_notification_channel_off, Snackbar.LENGTH_LONG)
                        .setAction(R.string.battery_optimization_open, v -> startActivity(
                                new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())
                                        .putExtra(Settings.EXTRA_CHANNEL_ID, PrayerReminderNotification.Channel.PRAYER.id)))
                        .show();
                break;
            default:
                Snackbar.make(root, R.string.settings_test_notification_app_off, Snackbar.LENGTH_LONG)
                        .setAction(R.string.battery_optimization_open, v -> startActivity(
                                new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName())))
                        .show();
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Pil ayarından dönüldüğünde durum yenilensin
        rowBattery.setSubtitle(BatteryOptimization.isIgnoring(requireContext())
                ? R.string.battery_optimization_unrestricted
                : R.string.battery_optimization_restricted);
        // Vakit detayından dönüldüğünde süre/uyarı özeti yenilensin
        for (Map.Entry<SettingsRowView, String> entry : prayerRows.entrySet()) {
            entry.getKey().setSubtitle(PrayerSettingsFragment.reminderSummary(requireContext(), settings, entry.getValue()));
        }
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
