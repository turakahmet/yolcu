package com.islamyolu.app.settings;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.islamyolu.app.R;
import com.islamyolu.app.VakitApi.PrayerAlarmScheduler;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.SettingsPreferences;

/** Vakit öncesi uyarı süresi (5/10/20 dk) ve kerahat vakitleri. */
public class ReminderSettingsFragment extends Fragment {

    private final NotificationPermissionGate permission = new NotificationPermissionGate(this, () -> { });

    public ReminderSettingsFragment() {
        super(R.layout.fragment_settings_reminders);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SettingsPreferences settings = AppPreferences.from(requireContext()).settings();

        MaterialButtonToggleGroup group = view.findViewById(R.id.reminderMinutesGroup);
        int checkedId = buttonFor(settings.getReminderMinutes());
        if (checkedId != View.NO_ID) {
            group.check(checkedId);
        }
        // check() dinleyiciden önce çağrıldı: yalnızca kullanıcı seçimi kaydedilir
        group.addOnButtonCheckedListener((g, buttonId, isChecked) -> {
            int minutes = minutesFor(buttonId);
            if (!isChecked || minutes == 0 || minutes == settings.getReminderMinutes()) {
                return;
            }
            settings.setReminderMinutes(minutes);
            // Bugünün ve yarının alarmları yeni süreyle yeniden kurulur
            PrayerAlarmScheduler.rescheduleAsync(requireContext(), null);
        });

        SettingsRowView rowKerahat = view.findViewById(R.id.rowKerahat);
        rowKerahat.setChecked(settings.isSwitchOn(SettingsPreferences.KEY_KERAHAT));
        rowKerahat.setOnToggleListener((row, checked) -> {
            if (checked && !permission.ensureGranted()) {
                row.setChecked(false);
                return;
            }
            settings.setSwitch(SettingsPreferences.KEY_KERAHAT, checked);
        });
    }

    private static int buttonFor(int minutes) {
        switch (minutes) {
            case 5:
                return R.id.reminder5;
            case 10:
                return R.id.reminder10;
            case 20:
                return R.id.reminder20;
            default:
                return View.NO_ID;
        }
    }

    private static int minutesFor(int buttonId) {
        if (buttonId == R.id.reminder5) return 5;
        if (buttonId == R.id.reminder10) return 10;
        if (buttonId == R.id.reminder20) return 20;
        return 0;
    }
}
