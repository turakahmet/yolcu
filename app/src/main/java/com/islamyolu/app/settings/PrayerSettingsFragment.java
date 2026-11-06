package com.islamyolu.app.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.islamyolu.app.R;
import com.islamyolu.app.VakitApi.PrayerAlarmScheduler;
import com.islamyolu.app.preferences.AppPreferences;
import com.islamyolu.app.preferences.SettingsPreferences;

/**
 * Tek bir vaktin ayarları: vaktin bildirimi (ezan + hatırlatma), vakit öncesi uyarı ve süresi.
 * Değişiklikler anında kaydedilir ve alarmlar yeniden kurulur (ayrı "Kaydet" adımı yok).
 */
public class PrayerSettingsFragment extends Fragment {

    private static final String ARG_KEY = "prayerKey";
    private static final String ARG_TITLE = "prayerTitle";

    private final NotificationPermissionGate permission = new NotificationPermissionGate(this, () -> { });

    @NonNull
    static Bundle args(@NonNull String prayerKey, @NonNull String prayerTitle) {
        Bundle bundle = new Bundle();
        bundle.putString(ARG_KEY, prayerKey);
        bundle.putString(ARG_TITLE, prayerTitle);
        return bundle;
    }

    /** Vakit satırının açıklaması: "10 dk önce uyarı" veya "Vakit öncesi uyarı kapalı". */
    @NonNull
    static String reminderSummary(@NonNull Context context, @NonNull SettingsPreferences settings,
                                  @NonNull String prayerKey) {
        if (!settings.isReminderOn(prayerKey)) {
            return context.getString(R.string.settings_prayer_reminder_off);
        }
        int minutes = settings.getReminderMinutes(prayerKey);
        return SettingsPreferences.KEY_NOTIFY_SABAH.equals(prayerKey)
                ? context.getString(R.string.settings_sabah_reminder_summary, minutes)
                : context.getString(R.string.settings_prayer_reminder_summary, minutes);
    }

    public PrayerSettingsFragment() {
        super(R.layout.fragment_settings_prayer);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String prayerKey = requireArguments().getString(ARG_KEY);
        if (prayerKey == null) {
            throw new IllegalArgumentException("prayerKey argümanı gerekli");
        }
        SettingsPreferences settings = AppPreferences.from(requireContext()).settings();

        SettingsRowView rowEnabled = view.findViewById(R.id.rowPrayerEnabled);
        SettingsRowView rowReminder = view.findViewById(R.id.rowReminderOn);
        TextInputLayout minutesLayout = view.findViewById(R.id.reminderMinutesLayout);
        MaterialAutoCompleteTextView minutesDropdown = view.findViewById(R.id.reminderMinutesDropdown);

        rowEnabled.setChecked(settings.isSwitchOn(prayerKey));
        rowReminder.setChecked(settings.isReminderOn(prayerKey));
        minutesLayout.setEnabled(settings.isReminderOn(prayerKey));

        rowEnabled.setOnToggleListener((row, checked) -> {
            if (checked && !permission.ensureGranted()) {
                row.setChecked(false);
                return;
            }
            settings.setSwitch(prayerKey, checked);
            reschedule();
        });
        rowReminder.setOnToggleListener((row, checked) -> {
            settings.setReminderOn(prayerKey, checked);
            minutesLayout.setEnabled(checked);
            reschedule();
        });

        int[] options = SettingsPreferences.reminderOptions(prayerKey);
        String[] labels = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            labels[i] = getString(R.string.settings_minutes_before, options[i]);
        }
        minutesDropdown.setSimpleItems(labels);
        minutesDropdown.setText(getString(R.string.settings_minutes_before, settings.getReminderMinutes(prayerKey)), false);
        minutesDropdown.setOnItemClickListener((parent, v, position, id) -> {
            settings.setReminderMinutes(prayerKey, options[position]);
            reschedule();
        });
        if (SettingsPreferences.KEY_NOTIFY_SABAH.equals(prayerKey)) {
            // Sabah namazı güneş doğunca biter: süre güneş doğuşuna göre sayılır
            minutesLayout.setHelperText(getString(R.string.settings_sabah_reminder_info));
        }
    }

    private void reschedule() {
        PrayerAlarmScheduler.rescheduleAsync(requireContext(), null);
    }
}
