package com.islamyolu.app.settings;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.islamyolu.app.R;
import com.islamyolu.app.VakitApi.PrayerAlarmScheduler;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.SettingsPreferences;

/**
 * Kerahat vakitleri (ana ekranda gösterim ve başlangıç bildirimi).
 * Vakit öncesi uyarı süreleri burada DEĞİL, her vaktin kendi detayında ayarlanır; bu ekran yalnızca
 * oraya yönlendirir (genel süre seçimi vakte özel ayarları ezdiği için kaldırıldı).
 */
public class ReminderSettingsFragment extends Fragment {

    private final NotificationPermissionGate permission = new NotificationPermissionGate(this, () -> { });

    public ReminderSettingsFragment() {
        super(R.layout.fragment_settings_reminders);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SettingsPreferences settings = AppPreferences.from(requireContext()).settings();

        view.findViewById(R.id.rowReminderTimes).setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.toNotificationsFromReminders));

        SettingsRowView rowKerahat = view.findViewById(R.id.rowKerahat);
        rowKerahat.setChecked(settings.isSwitchOn(SettingsPreferences.KEY_KERAHAT));
        rowKerahat.setOnToggleListener((row, checked) -> {
            if (checked && !permission.ensureGranted()) {
                row.setChecked(false);
                return;
            }
            settings.setSwitch(SettingsPreferences.KEY_KERAHAT, checked);
        });

        SettingsRowView rowKerahatNotify = view.findViewById(R.id.rowKerahatNotify);
        rowKerahatNotify.setChecked(settings.isKerahatNotifyOn());
        rowKerahatNotify.setOnToggleListener((row, checked) -> {
            if (checked && !permission.ensureGranted()) {
                row.setChecked(false);
                return;
            }
            settings.setKerahatNotify(checked);
            PrayerAlarmScheduler.rescheduleAsync(requireContext(), null);
        });
    }
}
