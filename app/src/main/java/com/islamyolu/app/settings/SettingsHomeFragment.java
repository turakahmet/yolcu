package com.islamyolu.app.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.islamyolu.app.R;
import com.islamyolu.app.prayertimes.PrayerCitySelection;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.SettingsPreferences;

/**
 * Ayarlar ana menüsü: 8 başlık. Açıklamalar güncel ayarlardan üretilir ve alt ekrandan dönünce
 * (onResume) yenilenir. Henüz yapılmamış başlıklar "Yakında" olarak pasif gösterilir.
 */
public class SettingsHomeFragment extends Fragment {

    private static final String[] PRAYER_KEYS = {
            SettingsPreferences.KEY_NOTIFY_IMSAK, SettingsPreferences.KEY_NOTIFY_SABAH,
            SettingsPreferences.KEY_NOTIFY_OGLE, SettingsPreferences.KEY_NOTIFY_IKINDI,
            SettingsPreferences.KEY_NOTIFY_AKSAM, SettingsPreferences.KEY_NOTIFY_YATSI
    };

    private SettingsRowView rowNotifications;
    private SettingsRowView rowSound;
    private SettingsRowView rowReminders;
    private SettingsRowView rowLocation;

    public SettingsHomeFragment() {
        super(R.layout.fragment_settings_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        rowNotifications = view.findViewById(R.id.rowNotifications);
        rowSound = view.findViewById(R.id.rowSound);
        rowReminders = view.findViewById(R.id.rowReminders);
        rowLocation = view.findViewById(R.id.rowLocation);

        navigateOnClick(rowNotifications, R.id.toNotifications);
        navigateOnClick(rowSound, R.id.toSound);
        navigateOnClick(rowReminders, R.id.toReminders);
        navigateOnClick(rowLocation, R.id.toLocation);
        navigateOnClick(view.findViewById(R.id.rowAbout), R.id.toAbout);

        // Sonraki görevlerde açılacak başlıklar
        view.findViewById(R.id.rowAccount).setEnabled(false);
        view.findViewById(R.id.rowApp).setEnabled(false);
        view.findViewById(R.id.rowData).setEnabled(false);

        TextView footer = view.findViewById(R.id.settingsFooter);
        footer.setText(getString(R.string.settings_footer, getString(R.string.app_name), getString(R.string.app_version)));
    }

    @Override
    public void onResume() {
        super.onResume();
        bindSummaries();
    }

    private void bindSummaries() {
        AppPreferences preferences = AppPreferences.from(requireContext());
        SettingsPreferences settings = preferences.settings();

        int active = 0;
        if (settings.isAllNotificationsOn()) {
            for (String key : PRAYER_KEYS) {
                if (settings.isSwitchOn(key)) active++;
            }
        }
        rowNotifications.setSubtitle(active > 0
                ? getString(R.string.settings_notifications_summary, active, PRAYER_KEYS.length)
                : getString(R.string.settings_notifications_off));

        rowSound.setSubtitle(settings.isNotificationSoundOn()
                ? R.string.settings_sound_summary_on
                : R.string.settings_sound_summary_off);

        rowReminders.setSubtitle(getString(settings.isKerahatOn()
                        ? R.string.settings_reminders_summary_kerahat
                        : R.string.settings_reminders_summary,
                settings.getReminderMinutes()));

        PrayerCitySelection selection = preferences.location().getPrayerCitySelection();
        rowLocation.setSubtitle(selection != null
                ? getString(R.string.prayer_city_current, selection.getDisplayName())
                : getString(R.string.prayer_city_not_selected));
    }

    private void navigateOnClick(@NonNull View row, @IdRes int actionId) {
        row.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(actionId));
    }
}
